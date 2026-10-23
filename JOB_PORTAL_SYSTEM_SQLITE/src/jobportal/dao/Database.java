package jobportal.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.sqlite.SQLiteConfig;

/**
 * Creates JDBC connections to the SQLite database and prepares it automatically.
 *
 * SQLite is a database that lives in ONE FILE (default: data/job_portal.db), so no database
 * server has to be installed or started. At the first connection this class
 *
 *   1. creates the "data" folder and the database file if they do not exist,
 *   2. creates all tables and indexes   (database/schema_sqlite.sql, safe to repeat),
 *   3. inserts the sample data ONCE     (database/sample_data_sqlite.sql, only into an empty database).
 *
 * The JDBC URL is read from the file "db.properties" in the project folder
 * (default jdbc:sqlite:data/job_portal.db). Only SQLite URLs are accepted.
 */
public class Database {

    private static final String DEFAULT_URL = "jdbc:sqlite:data/job_portal.db";
    private static final String URL_PREFIX = "jdbc:sqlite:";
    private static final String SCHEMA_SCRIPT = "schema_sqlite.sql";
    private static final String SAMPLE_DATA_SCRIPT = "sample_data_sqlite.sql";

    /** Increase this number when the table structure changes in a later version (see initialize()). */
    private static final int SCHEMA_VERSION = 1;

    private static String url = DEFAULT_URL;
    private static boolean initialized = false;

    // static block: runs once when the class is loaded
    static {
        loadSettings();
    }

    private static void loadSettings() {
        Properties props = new Properties();
        try (FileInputStream input = new FileInputStream("db.properties")) {
            props.load(input);
        } catch (IOException e) {
            System.out.println("db.properties not found - using the default database: " + DEFAULT_URL);
        }
        // A system property (-Djobportal.db.url=...) wins; it is used by the automatic tests.
        String configured = System.getProperty("jobportal.db.url", props.getProperty("db.url", DEFAULT_URL)).trim();
        if (configured.startsWith(URL_PREFIX)) {
            url = configured;
        } else {
            System.out.println("Ignoring db.url '" + configured + "': this project uses SQLite only. Using " + DEFAULT_URL);
            url = DEFAULT_URL;
        }
    }

    // ------------------------------------------------------------------
    // CONNECTIONS
    // ------------------------------------------------------------------

    /**
     * Opens a new connection (and prepares the database the first time).
     * The caller must close it (we use try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        ensureInitialized();
        return openConnection();
    }

    /** Used at start-up to check that the database can be opened and answers a query. */
    public static void testConnection() throws SQLException {
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next();
        }
    }

    /** Opens a raw connection with the settings every connection of this project needs. */
    private static Connection openConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");           // load the SQLite JDBC driver (lib/sqlite-jdbc-*.jar)
        } catch (ClassNotFoundException e) {
            throw new SQLException("The SQLite JDBC driver (lib/sqlite-jdbc-*.jar) was not found on the classpath.", e);
        }
        SQLiteConfig config = new SQLiteConfig();
        config.setBusyTimeout(10000);                   // wait up to 10 s if another thread is writing
        config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);   // setAutoCommit(false) -> BEGIN IMMEDIATE
        Connection con = DriverManager.getConnection(url, config.toProperties());
        try (Statement st = con.createStatement()) {
            // SQLite ignores foreign keys unless this is switched on for EACH connection
            st.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            con.close();
            throw e;
        }
        return con;
    }

    // ------------------------------------------------------------------
    // AUTOMATIC INITIALIZATION
    // ------------------------------------------------------------------

    private static synchronized void ensureInitialized() throws SQLException {
        if (!initialized) {
            initialize();
        }
    }

    /**
     * Creates the database folder / file, the tables, the indexes and (only for a new, empty database)
     * the sample data. It is safe to call it many times: nothing is deleted and nothing is duplicated.
     * Everything runs in ONE transaction, so a failure never leaves a half-created database.
     */
    public static synchronized void initialize() throws SQLException {
        createDatabaseFolder();

        String schema = readScript(SCHEMA_SCRIPT);
        String sampleData = readScript(SAMPLE_DATA_SCRIPT);

        try (Connection con = openConnection()) {
            con.setAutoCommit(false);                       // start the transaction (BEGIN IMMEDIATE)
            try {
                runScript(con, schema);                     // CREATE TABLE / INDEX IF NOT EXISTS

                int version = readUserVersion(con);         // 0 = brand new database file
                if (version == 0 && countRows(con, "users") == 0) {
                    runScript(con, sampleData);             // demo accounts, jobs, applications ...
                    System.out.println("New SQLite database created with sample data: " + getDatabaseFile());
                }
                if (version < SCHEMA_VERSION) {
                    try (Statement st = con.createStatement()) {
                        st.execute("PRAGMA user_version = " + SCHEMA_VERSION);
                    }
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
        initialized = true;
    }

    private static int readUserVersion(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static int countRows(Connection con, String table) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static void createDatabaseFolder() throws SQLException {
        File file = getDatabaseFile();
        if (file == null) {
            return;                                         // in-memory database: no file
        }
        File folder = file.getAbsoluteFile().getParentFile();
        if (folder != null && !folder.exists() && !folder.mkdirs()) {
            throw new SQLException("Could not create the database folder: " + folder.getPath());
        }
    }

    /** The database file on disk, or null for an in-memory database. */
    public static File getDatabaseFile() {
        String path = url.substring(URL_PREFIX.length());
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        if (path.isEmpty() || path.equals(":memory:") || path.startsWith("file::memory:")) {
            return null;
        }
        if (path.startsWith("file:")) {
            path = path.substring("file:".length());
        }
        return new File(path);
    }

    public static String getUrl() {
        return url;
    }

    // ------------------------------------------------------------------
    // SQL SCRIPT HELPERS
    // ------------------------------------------------------------------

    /**
     * Reads a script from the "database" folder. It is searched (1) on the class path (works when the
     * folder is copied next to the compiled classes), (2) in ./database (the project folder) and
     * (3) in ../database relative to the compiled classes.
     */
    private static String readScript(String name) throws SQLException {
        try {
            try (InputStream in = Database.class.getResourceAsStream("/database/" + name)) {
                if (in != null) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            List<Path> candidates = new ArrayList<>();
            candidates.add(Paths.get("database", name));
            try {
                Path classes = Paths.get(Database.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                Path parent = classes.toAbsolutePath().getParent();
                if (parent != null) {
                    candidates.add(parent.resolve("database").resolve(name));
                }
            } catch (URISyntaxException | RuntimeException e) {
                // no code source information - the other candidates are enough
            }
            for (Path candidate : candidates) {
                if (Files.isRegularFile(candidate)) {
                    return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            throw new SQLException("Could not read the database script " + name + ": " + e.getMessage(), e);
        }
        throw new SQLException("Database script not found: database/" + name
                + "  (start the program from the project folder).");
    }

    private static void runScript(Connection con, String script) throws SQLException {
        try (Statement st = con.createStatement()) {
            for (String sql : splitStatements(script)) {
                st.execute(sql);
            }
        }
    }

    /** Splits a SQL script at ';' (ignores ';' inside quotes and removes -- comments). */
    public static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        int length = script.length();
        for (int i = 0; i < length; i++) {
            char c = script.charAt(i);
            if (inQuote) {
                current.append(c);
                if (c == '\'') {
                    if (i + 1 < length && script.charAt(i + 1) == '\'') {   // '' = escaped quote
                        current.append('\'');
                        i++;
                    } else {
                        inQuote = false;
                    }
                }
            } else if (c == '\'') {
                inQuote = true;
                current.append(c);
            } else if (c == '-' && i + 1 < length && script.charAt(i + 1) == '-') {
                while (i < length && script.charAt(i) != '\n') {            // skip the comment
                    i++;
                }
                current.append('\n');
            } else if (c == ';') {
                addStatement(statements, current);
            } else {
                current.append(c);
            }
        }
        addStatement(statements, current);
        return statements;
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String sql = current.toString().trim();
        if (!sql.isEmpty()) {
            statements.add(sql);
        }
        current.setLength(0);
    }

    // ------------------------------------------------------------------
    // SMALL HELPERS USED BY THE DAO CLASSES
    // ------------------------------------------------------------------

    /** Returns the id generated by the last INSERT on this connection (AUTOINCREMENT column). */
    public static int lastInsertId(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * True if the exception was caused by a UNIQUE constraint (duplicate row).
     * SQLite reports "[SQLITE_CONSTRAINT_UNIQUE] ... UNIQUE constraint failed: ..." (error code 19 / 2067 / 1555).
     */
    public static boolean isUniqueViolation(SQLException e) {
        int code = e.getErrorCode();
        String message = e.getMessage() == null ? "" : e.getMessage();
        return message.contains("UNIQUE constraint failed")
                || message.contains("SQLITE_CONSTRAINT_UNIQUE")
                || message.contains("SQLITE_CONSTRAINT_PRIMARYKEY")
                || code == 2067 || code == 1555;
    }
}
