package jobportal.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import jobportal.model.Exportable;
import jobportal.model.User;
import jobportal.model.UserPreferences;

/**
 * All FILE HANDLING of the project is in this class:
 *
 *  1. CSV export                 -> BufferedWriter + FileWriter          (character streams)
 *  2. Resume copy                -> FileInputStream + FileOutputStream   (byte streams)
 *  3. Save/load preferences      -> ObjectOutputStream / ObjectInputStream (serialization)
 *  4. Application activity log   -> RandomAccessFile
 */
public final class FileHandler {

    // Folder for the log, resumes and saved preferences (the SQLite file is configured in db.properties).
    // "data" is the default; the automatic tests use -Djobportal.data.dir=... to keep their files separate.
    private static final String DATA_FOLDER = System.getProperty("jobportal.data.dir", "data");
    private static final String RESUME_FOLDER = DATA_FOLDER + "/resumes";
    private static final String LOG_FILE = DATA_FOLDER + "/activity.log";

    private FileHandler() {
    }

    private static void makeFolder(String path) {
        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    // ------------------------------------------------------------------
    // 1. CSV EXPORT
    // ------------------------------------------------------------------

    /**
     * Writes any array of Exportable objects (Jobs or Applications) to a CSV file.
     * This works for both classes because of the Exportable interface (polymorphism).
     */
    public static void exportToCsv(Exportable[] rows, File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        try {
            if (rows.length > 0) {
                writer.write(rows[0].getCsvHeader());
                writer.newLine();
            }
            for (Exportable row : rows) {
                writer.write(row.toCsvRow());
                writer.newLine();
            }
        } finally {
            writer.close();
        }
    }

    // ------------------------------------------------------------------
    // 2. RESUME COPY  (byte streams)
    // ------------------------------------------------------------------

    /** Copies the resume chosen by the user into data/resumes and returns the new path. */
    public static String copyResume(File source, int userId) throws IOException {
        makeFolder(RESUME_FOLDER);

        String name = source.getName();
        String extension = "";
        int dotPosition = name.lastIndexOf('.');
        if (dotPosition >= 0) {
            extension = name.substring(dotPosition);          // example: ".pdf"
        }
        File destination = new File(RESUME_FOLDER, "resume_user" + userId + extension);

        FileInputStream input = new FileInputStream(source);
        try {
            FileOutputStream output = new FileOutputStream(destination);
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            } finally {
                output.close();
            }
        } finally {
            input.close();
        }
        return destination.getPath();
    }

    // ------------------------------------------------------------------
    // 3. SERIALIZATION of UserPreferences
    // ------------------------------------------------------------------

    public static void savePreferences(UserPreferences prefs, int userId) throws IOException {
        makeFolder(DATA_FOLDER);
        ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(DATA_FOLDER + "/prefs_user" + userId + ".ser"));
        try {
            out.writeObject(prefs);          // serialization: object -> bytes in a file
        } finally {
            out.close();
        }
    }

    /** Reads the saved preferences. If nothing is saved yet, returns a new default object. */
    public static UserPreferences loadPreferences(int userId) {
        File file = new File(DATA_FOLDER + "/prefs_user" + userId + ".ser");
        if (!file.exists()) {
            return new UserPreferences();
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (UserPreferences) in.readObject();     // deserialization: bytes -> object
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Could not read preferences: " + e.getMessage());
            return new UserPreferences();
        }
    }

    // ------------------------------------------------------------------
    // 4. ACTIVITY LOG with RandomAccessFile
    // ------------------------------------------------------------------

    // Method overloading: two versions of writeLog

    public static synchronized void writeLog(String message) {
        // StringBuffer is thread-safe; log lines can be written by different threads
        StringBuffer line = new StringBuffer();
        line.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        line.append(" | ");
        line.append(message);
        line.append("\n");

        makeFolder(DATA_FOLDER);
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(LOG_FILE, "rw");     // "rw" = read and write
            file.seek(file.length());                        // jump to the END of the file
            file.write(line.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("Could not write log: " + e.getMessage());
        } finally {
            closeQuietly(file);
        }
    }

    public static void writeLog(User user, String action) {
        writeLog(user.getRole() + " " + user.getEmail() + " : " + action);
    }

    /**
     * Returns the last lines of the log file.
     * synchronized: a thread cannot read while another thread is writing.
     */
    public static synchronized String readRecentLog(int maxLines) {
        File logFile = new File(LOG_FILE);
        if (!logFile.exists()) {
            return "No activity recorded yet.";
        }
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(logFile, "r");       // "r" = read only
            long start = Math.max(0, file.length() - 20000); // read only the last 20 KB
            file.seek(start);                                // jump to that position
            byte[] chunk = new byte[(int) (file.length() - start)];
            file.readFully(chunk);

            String[] lines = new String(chunk, StandardCharsets.UTF_8).split("\n");
            int firstLine = Math.max(0, lines.length - maxLines);
            if (start > 0 && firstLine == 0) {
                firstLine = 1;                               // first line may be cut in the middle
            }
            StringBuilder result = new StringBuilder();
            for (int i = firstLine; i < lines.length; i++) {
                result.append(lines[i]).append("\n");
            }
            return result.toString();
        } catch (IOException e) {
            return "Could not read the log file: " + e.getMessage();
        } finally {
            closeQuietly(file);
        }
    }

    private static void closeQuietly(RandomAccessFile file) {
        if (file != null) {
            try {
                file.close();
            } catch (IOException e) {
                System.out.println("Could not close file: " + e.getMessage());
            }
        }
    }
}
