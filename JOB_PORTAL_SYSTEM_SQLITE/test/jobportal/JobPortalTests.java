package jobportal;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jobportal.dao.ApplicationDao;
import jobportal.dao.Database;
import jobportal.dao.JobDao;
import jobportal.dao.NotificationDao;
import jobportal.dao.UserDao;
import jobportal.exception.DuplicateRecordException;
import jobportal.exception.JobPortalException;
import jobportal.model.Admin;
import jobportal.model.Application;
import jobportal.model.Company;
import jobportal.model.Job;
import jobportal.model.JobSeeker;
import jobportal.model.Recruiter;
import jobportal.model.User;
import jobportal.service.NotificationService;
import jobportal.service.RestApiServer;

/**
 * Automatic tests for the SQLite version of the Job Portal System.
 * No test library is needed: this is a normal Java program that prints PASS / FAIL for every check
 * and ends with exit code 0 (everything passed) or 1 (something failed).
 *
 * The tests never touch your real database: they use a temporary database file.
 *
 * Modes (first program argument):
 *   (none)                    all tests in one run (new temporary database)
 *   persist-write             creates test records in the database given by -Djobportal.db.url=...
 *   persist-verify            checks, in a NEW Java process, that those records are still there
 */
public class JobPortalTests {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";

        if (mode.equals("all")) {
            Path temp = Files.createTempDirectory("jobportal_test_");
            System.setProperty("jobportal.db.url", "jdbc:sqlite:" + temp.resolve("test_job_portal.db").toString().replace('\\', '/'));
            System.setProperty("jobportal.data.dir", temp.resolve("data").toString());
            runAllTests(temp);
        } else if (mode.equals("persist-write")) {
            persistWrite();
        } else if (mode.equals("persist-verify")) {
            persistVerify();
        } else {
            System.out.println("Unknown mode: " + mode);
            System.exit(2);
        }

        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------
    // tiny assertion helpers
    // ------------------------------------------------------------------

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    private static void checkEquals(String name, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (ok) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name + "   (expected: " + expected + ", actual: " + actual + ")");
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("== " + title);
    }

    private static int count(String sql, Object... params) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static boolean throwsDuplicate(ThrowingRunnable action) {
        try {
            action.run();
            return false;
        } catch (DuplicateRecordException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean throwsJobPortal(ThrowingRunnable action) {
        try {
            action.run();
            return false;
        } catch (JobPortalException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    // ------------------------------------------------------------------
    // ALL TESTS
    // ------------------------------------------------------------------

    private static void runAllTests(Path temp) throws Exception {
        File dbFile = new File(temp.toFile(), "test_job_portal.db");

        section("A. Dependencies and clean database");
        Class<?> driver = Class.forName("org.sqlite.JDBC");
        check("SQLite JDBC driver class loads (" + driver.getName() + ")", true);
        check("the test database file does not exist yet", !dbFile.exists());
        check("Database uses a SQLite URL", Database.getUrl().startsWith("jdbc:sqlite:"));

        Database.testConnection();      // creates the file, the tables and the sample data
        check("database file was created automatically", dbFile.isFile() && dbFile.length() > 0);

        try (Connection con = Database.getConnection()) {
            check("JDBC product is SQLite", con.getMetaData().getURL().startsWith("jdbc:sqlite:"));
            DatabaseMetaData meta = con.getMetaData();
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            for (String t : new String[]{"users", "job_seekers", "companies", "recruiters", "jobs",
                                         "applications", "saved_jobs", "notifications"}) {
                check("table exists: " + t, tables.contains(t));
            }
            int indexes = 0;
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name LIKE 'idx_%'")) {
                rs.next();
                indexes = rs.getInt(1);
            }
            check("indexes were created (6 idx_* indexes)", indexes == 6);
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("PRAGMA foreign_keys")) {
                rs.next();
                checkEquals("PRAGMA foreign_keys = 1 on a new connection", 1, rs.getInt(1));
            }
        }
        checkEquals("sample data: users", 7, count("SELECT COUNT(*) FROM users"));
        checkEquals("sample data: job_seekers", 3, count("SELECT COUNT(*) FROM job_seekers"));
        checkEquals("sample data: companies", 3, count("SELECT COUNT(*) FROM companies"));
        checkEquals("sample data: recruiters", 3, count("SELECT COUNT(*) FROM recruiters"));
        checkEquals("sample data: jobs", 9, count("SELECT COUNT(*) FROM jobs"));
        checkEquals("sample data: applications", 9, count("SELECT COUNT(*) FROM applications"));
        checkEquals("sample data: saved_jobs", 4, count("SELECT COUNT(*) FROM saved_jobs"));
        checkEquals("sample data: notifications", 7, count("SELECT COUNT(*) FROM notifications"));

        section("B. Constraints (foreign keys, CHECK, UNIQUE)");
        check("foreign key blocks an application for a missing job", sqlFails(
                "INSERT INTO applications (job_id, seeker_id) VALUES (9999, 5)"));
        check("CHECK blocks an invalid role", sqlFails(
                "INSERT INTO users (full_name, email, password_hash, phone, role) VALUES ('X','x@x.com','h','9000000000','BOSS')"));
        check("CHECK blocks an invalid job type", sqlFails(
                "INSERT INTO jobs (recruiter_id,title,description,location,job_type) VALUES (2,'t','d','l','Weekend')"));
        check("CHECK keeps the VARCHAR(100) limit of full_name", sqlFails(
                "INSERT INTO users (full_name, email, password_hash, phone, role) VALUES ('" + "n".repeat(101) + "','long@x.com','h','9000000000','ADMIN')"));
        check("UNIQUE email", sqlFails(
                "INSERT INTO users (full_name, email, password_hash, phone, role) VALUES ('Dup','admin@jobportal.com','h','9000000000','ADMIN')"));
        boolean uniqueDetected = false;
        try (Connection con = Database.getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate("INSERT INTO applications (job_id, seeker_id) VALUES (1, 5)");
        } catch (SQLException e) {
            uniqueDetected = Database.isUniqueViolation(e);
        }
        check("Database.isUniqueViolation recognises UNIQUE (job_id, seeker_id)", uniqueDetected);
        checkEquals("script splitter ignores ';' inside quotes and comments", 2,
                Database.splitStatements("-- c;\nINSERT INTO t VALUES ('a;b'); SELECT 1;").size());

        section("C. Safe repeated initialization");
        Database.initialize();
        Database.initialize();
        checkEquals("users unchanged after initializing twice more", 7, count("SELECT COUNT(*) FROM users"));
        checkEquals("jobs unchanged after initializing twice more", 9, count("SELECT COUNT(*) FROM jobs"));
        checkEquals("applications unchanged after initializing twice more", 9, count("SELECT COUNT(*) FROM applications"));

        UserDao userDao = new UserDao();
        JobDao jobDao = new JobDao();
        ApplicationDao appDao = new ApplicationDao();
        NotificationDao notDao = new NotificationDao();

        section("D. Login and roles");
        User admin = userDao.login("admin@jobportal.com", "admin123");
        check("admin login returns Admin", admin instanceof Admin && admin.getRole().equals(User.ROLE_ADMIN));
        User rec = userDao.login("hr@technova.com", "recruit123");
        check("recruiter login returns Recruiter with company", rec instanceof Recruiter
                && ((Recruiter) rec).getCompany().getName().equals("TechNova Solutions"));
        checkEquals("recruiter id", 2, rec.getUserId());
        for (String mail : new String[]{"careers@infobridge.com", "hiring@greenbyte.com"}) {
            check("recruiter login works: " + mail, userDao.login(mail, "recruit123") instanceof Recruiter);
        }
        for (String mail : new String[]{"rahul@student.com", "priya@student.com", "amit@student.com"}) {
            check("job seeker login works: " + mail, userDao.login(mail, "seeker123") instanceof JobSeeker);
        }
        JobSeeker rahul = (JobSeeker) userDao.login("rahul@student.com", "seeker123");
        check("seeker profile loaded (skills contain java)", rahul.getSkills().contains("java"));
        check("wrong password is rejected", throwsJobPortal(() -> userDao.login("admin@jobportal.com", "wrong")));
        check("unknown email is rejected", throwsJobPortal(() -> userDao.login("nobody@x.com", "whatever")));
        check("email login is case/space tolerant", userDao.login("  ADMIN@JobPortal.com ", "admin123") != null);

        section("E. Jobs: browse, search, filters, dates");
        List<Job> open = jobDao.searchJobs("");
        checkEquals("open jobs visible to seekers", 8, open.size());
        check("closed job is hidden from search", open.stream().noneMatch(j -> j.getTitle().equals("Database Administrator")));
        checkEquals("all jobs (admin view)", 9, jobDao.getAllJobs().size());
        checkEquals("search 'java' finds the same as 'JAVA' (case-insensitive)",
                jobDao.searchJobs("java").size(), jobDao.searchJobs("JAVA").size());
        check("search 'java' finds Java Developer", jobDao.searchJobs("JAVA").stream().anyMatch(j -> j.getTitle().equals("Java Developer")));
        check("search by company name", jobDao.searchJobs("greenbyte").size() == 2);
        checkEquals("location filter 'PUNE' (capitals)", 2, jobDao.searchJobs("", "PUNE", "All").size());
        checkEquals("job type filter Internship", 2, jobDao.searchJobs("", "", "Internship").size());
        checkEquals("combined filter java + Pune + Full-time", 2, jobDao.searchJobs("java", "pune", "Full-time").size());
        Job j1 = jobDao.getJobById(1);
        check("getJobById(1) returns Java Developer with company", j1 != null && j1.getTitle().equals("Java Developer")
                && j1.getCompanyName().equals("TechNova Solutions") && j1.getSalaryLpa() == 6.5);
        checkEquals("applicant count of job 1", 2, j1.getApplicantCount());
        check("getJobById(999) is null", jobDao.getJobById(999) == null);
        check("posted_date is an ISO date (yyyy-MM-dd)", j1.getPostedDate().matches("\\d{4}-\\d{2}-\\d{2}"));
        check("sample job 1 was posted 10 days ago", j1.getPostedDate().equals(LocalDate.now().minusDays(10).toString()));
        checkEquals("jobs of recruiter 2", 3, jobDao.getJobsByRecruiter(2).size());
        checkEquals("countJobs()", 9, jobDao.countJobs());
        checkEquals("countJobs(CLOSED)", 1, jobDao.countJobs("CLOSED"));

        section("F. Job seeker: apply, status tracking, saved jobs");
        checkEquals("Rahul has 3 applications", 3, appDao.getApplicationsBySeeker(5).size());
        int amit = 7;
        check("Amit has not applied for job 3", !appDao.hasApplied(3, amit));
        appDao.applyForJob(3, amit);
        check("Amit can apply for job 3", appDao.hasApplied(3, amit));
        check("applying twice -> DuplicateRecordException", throwsDuplicate(() -> appDao.applyForJob(3, amit)));
        Application newest = appDao.getApplicationsBySeeker(amit).get(0);
        checkEquals("new application starts as APPLIED", "APPLIED", newest.getStatus());
        check("applied_date is today", newest.getAppliedDate().equals(LocalDate.now().toString()));
        checkEquals("applications total after apply", 10, appDao.countApplications());
        Map<String, Integer> seekerCounts = appDao.getStatusCountsForSeeker(amit);
        checkEquals("Amit status counts: INTERVIEW", 1, seekerCounts.get("INTERVIEW"));
        checkEquals("Amit status counts: APPLIED", 3, seekerCounts.get("APPLIED"));

        checkEquals("Rahul saved jobs", 2, jobDao.countSavedJobs(5));
        jobDao.saveJob(5, 7);
        checkEquals("saving a job adds it", 3, jobDao.countSavedJobs(5));
        check("saving twice -> DuplicateRecordException", throwsDuplicate(() -> jobDao.saveJob(5, 7)));
        check("saved job list contains the job", jobDao.getSavedJobs(5).stream().anyMatch(j -> j.getJobId() == 7));
        jobDao.removeSavedJob(5, 7);
        checkEquals("removing a saved job", 2, jobDao.countSavedJobs(5));

        section("G. Recruiter: manage jobs and applicants");
        Job fresh = new Job(2, "Test Engineer", "Writes tests; checks quotes ' and semicolons ;", "Pune", "Contract", 3.25, "java, testing", 1);
        jobDao.addJob(fresh);
        List<Job> mine = jobDao.getJobsByRecruiter(2);
        checkEquals("recruiter now has 4 jobs", 4, mine.size());
        Job added = mine.get(0);                       // newest first
        check("new job is OPEN and has the right data", added.getTitle().equals("Test Engineer") && added.isOpen()
                && added.getSalaryLpa() == 3.25 && added.getPostedDate().equals(LocalDate.now().toString())
                && added.getDescription().contains("quotes '"));
        added.setTitle("Senior Test Engineer");
        added.setSalaryLpa(5.5);
        jobDao.updateJob(added);
        check("job update is saved", jobDao.getJobById(added.getJobId()).getTitle().equals("Senior Test Engineer")
                && jobDao.getJobById(added.getJobId()).getSalaryLpa() == 5.5);
        jobDao.updateJobStatus(added.getJobId(), "CLOSED");
        check("closed job disappears from search", jobDao.searchJobs("Senior Test").isEmpty());
        jobDao.updateJobStatus(added.getJobId(), "OPEN");
        check("reopened job appears in search again", jobDao.searchJobs("Senior Test").size() == 1);

        appDao.applyForJob(added.getJobId(), 5);
        jobDao.saveJob(6, added.getJobId());
        List<Application> forRecruiter = appDao.getApplicationsForRecruiter(2);
        checkEquals("recruiter 2 applicants (5 sample + Amit job3 + Rahul new job)", 7, forRecruiter.size());
        checkEquals("applicants of one job", 1, appDao.getApplicationsForJob(added.getJobId()).size());
        int appId = appDao.getApplicationsForJob(added.getJobId()).get(0).getApplicationId();
        appDao.updateStatus(appId, "SHORTLISTED");
        checkEquals("status change is saved", "SHORTLISTED", appDao.getApplicationsForJob(added.getJobId()).get(0).getStatus());
        check("invalid status is rejected by CHECK constraint", sqlFails("UPDATE applications SET status='MAYBE' WHERE application_id=" + appId));
        Map<String, Integer> recCounts = appDao.getStatusCountsForRecruiter(2);
        checkEquals("recruiter status counts SHORTLISTED (sample + new)", 2, recCounts.get("SHORTLISTED"));

        jobDao.deleteJob(added.getJobId());
        check("deleting a job removes it", jobDao.getJobById(added.getJobId()) == null);
        checkEquals("ON DELETE CASCADE removed its applications", 0, count("SELECT COUNT(*) FROM applications WHERE job_id = ?", added.getJobId()));
        checkEquals("ON DELETE CASCADE removed its saved jobs", 0, count("SELECT COUNT(*) FROM saved_jobs WHERE job_id = ?", added.getJobId()));

        section("H. Registration (transactions, generated ids) and profiles");
        JobSeeker newSeeker = new JobSeeker("Test Seeker", "test.seeker@mail.com", User.hashPassword("secret1"), "9123456780");
        newSeeker.setSkills("java, sql");
        newSeeker.setEducation("BCA");
        newSeeker.setCity("Delhi");
        userDao.registerJobSeeker(newSeeker);
        check("new seeker gets an id from AUTOINCREMENT", newSeeker.getUserId() == 8);
        JobSeeker loaded = (JobSeeker) userDao.login("test.seeker@mail.com", "secret1");
        check("new seeker can log in and profile was saved", loaded.getCity().equals("Delhi") && loaded.getSkills().equals("java, sql"));
        check("duplicate seeker email -> DuplicateRecordException", throwsDuplicate(() -> userDao.registerJobSeeker(
                new JobSeeker("Other", "test.seeker@mail.com", User.hashPassword("secret1"), "9123456781"))));
        checkEquals("failed duplicate left users unchanged", 8, userDao.getAllUsers().size());

        int companiesBefore = count("SELECT COUNT(*) FROM companies");
        Company company = new Company("Test Corp");
        company.setIndustry("Testing");
        company.setLocation("Chennai");
        Recruiter newRec = new Recruiter("Test Recruiter", "test.rec@mail.com", User.hashPassword("secret2"), "9123456782", company, "Lead");
        userDao.registerRecruiter(newRec);
        check("new recruiter and company got ids", newRec.getUserId() == 9 && newRec.getCompany().getCompanyId() == companiesBefore + 1);
        check("new recruiter can log in with company", ((Recruiter) userDao.login("test.rec@mail.com", "secret2")).getCompany().getName().equals("Test Corp"));
        check("duplicate recruiter email -> DuplicateRecordException", throwsDuplicate(() -> userDao.registerRecruiter(
                new Recruiter("Dup", "test.rec@mail.com", User.hashPassword("secret2"), "9123456783", new Company("Orphan Inc"), "x"))));
        checkEquals("no orphan company after the failed registration", companiesBefore + 1, count("SELECT COUNT(*) FROM companies"));

        loaded.setFullName("Test Seeker Renamed");
        loaded.setSkills("java, sql, jdbc");
        loaded.setExperienceYears(2);
        userDao.updateJobSeeker(loaded);
        JobSeeker reloaded = (JobSeeker) userDao.login("test.seeker@mail.com", "secret1");
        check("seeker profile update is saved", reloaded.getFullName().equals("Test Seeker Renamed")
                && reloaded.getSkills().equals("java, sql, jdbc") && reloaded.getExperienceYears() == 2);
        Recruiter recLoaded = (Recruiter) userDao.login("test.rec@mail.com", "secret2");
        recLoaded.setDesignation("Head of HR");
        recLoaded.getCompany().setWebsite("www.test.example");
        userDao.updateRecruiter(recLoaded);
        Recruiter recReloaded = (Recruiter) userDao.login("test.rec@mail.com", "secret2");
        check("recruiter + company update is saved", recReloaded.getDesignation().equals("Head of HR")
                && recReloaded.getCompany().getWebsite().equals("www.test.example"));

        section("I. Admin: statistics, block, delete");
        checkEquals("countUsers(JOB_SEEKER)", 4, userDao.countUsers("JOB_SEEKER"));
        checkEquals("countUsers(RECRUITER)", 4, userDao.countUsers("RECRUITER"));
        checkEquals("countUsers(ADMIN)", 1, userDao.countUsers("ADMIN"));
        userDao.updateUserStatus(6, "BLOCKED");
        check("blocked user cannot log in", throwsJobPortal(() -> userDao.login("priya@student.com", "seeker123")));
        userDao.updateUserStatus(6, "ACTIVE");
        check("unblocked user can log in again", userDao.login("priya@student.com", "seeker123") != null);
        int jobsBefore = jobDao.countJobs();
        int appsBefore = appDao.countApplications();
        userDao.deleteUser(3);   // recruiter of InfoBridge: has 3 jobs and applications
        check("deleted recruiter cannot log in", throwsJobPortal(() -> userDao.login("careers@infobridge.com", "recruit123")));
        checkEquals("cascade: the 3 jobs of the deleted recruiter are gone", jobsBefore - 3, jobDao.countJobs());
        check("cascade: their applications are gone", appDao.countApplications() < appsBefore);
        checkEquals("unused company was cleaned up", 0, count("SELECT COUNT(*) FROM companies WHERE name = 'InfoBridge Systems'"));
        checkEquals("cascade: their notifications are gone", 0, count("SELECT COUNT(*) FROM notifications WHERE user_id = 3"));
        checkEquals("no orphan rows in applications", 0, count(
                "SELECT COUNT(*) FROM applications a LEFT JOIN jobs j ON a.job_id=j.job_id WHERE j.job_id IS NULL"));

        section("J. Notifications");
        int unread = notDao.countUnread(5);
        notDao.addNotification(5, "Hello from the test");
        checkEquals("unread counter increases", unread + 1, notDao.countUnread(5));
        Vector<String> list = notDao.getNotifications(5);
        check("newest notification first, marked [NEW]", list.get(0).startsWith("[NEW]") && list.get(0).endsWith("Hello from the test"));
        check("time text looks like '21 Oct 2026, 14:35'", list.get(0).matches("\\[NEW\\]  \\d{2} [A-Z][a-z]{2} \\d{4}, \\d{2}:\\d{2}   -   .*"));
        notDao.markAllRead(5);
        checkEquals("mark all read", 0, notDao.countUnread(5));
        check("read notifications have no [NEW] tag", !notDao.getNotifications(5).get(0).startsWith("[NEW]"));
        int before = notDao.getNotifications(2).size();
        NotificationService.sendAsync(2, "async message");
        NotificationService.shutdown();     // waits for the background thread pool
        checkEquals("background SendTask saved its notification", before + 1, notDao.getNotifications(2).size());

        section("K. Concurrency (several threads writing at the same time)");
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            final int worker = t;
            futures.add(pool.submit(() -> {
                int errors = 0;
                NotificationDao dao = new NotificationDao();
                for (int i = 0; i < 25; i++) {
                    try {
                        dao.addNotification(5, "load " + worker + "/" + i);
                        dao.countUnread(5);
                    } catch (SQLException e) {
                        errors++;
                        System.out.println("     " + e.getMessage());
                    }
                }
                return errors;
            }));
        }
        int errors = 0;
        for (Future<Integer> f : futures) {
            errors += f.get();
        }
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        checkEquals("200 concurrent inserts, no 'database is locked' errors", 0, errors);
        checkEquals("all 200 concurrent notifications were stored", 200, count("SELECT COUNT(*) FROM notifications WHERE message LIKE 'load %'"));

        section("L. REST API");
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        RestApiServer server = new RestApiServer(port);
        server.start();
        try {
            String base = "http://localhost:" + port;
            String body = httpGet(base + "/jobs", 200);
            check("GET /jobs returns a JSON array of the open jobs", body.startsWith("[") && body.contains("\"title\": \"Java Developer\"")
                    && !body.contains("Database Administrator"));
            String one = httpGet(base + "/jobs/1", 200);
            check("GET /jobs/1 returns job 1", one.contains("\"id\": 1") && one.contains("TechNova Solutions") && one.contains("\"salaryLpa\": 6.5"));
            httpGet(base + "/jobs/999", 404);
            check("GET /jobs/999 -> 404", true);
            httpGet(base + "/jobs/abc", 400);
            check("GET /jobs/abc -> 400", true);
            check("POST /jobs -> 405", httpStatus(base + "/jobs", "POST") == 405);
        } finally {
            server.stop();
        }

        section("M. Connections are SQLite only");
        try (Connection con = Database.getConnection()) {
            check("driver name is SQLite JDBC", con.getMetaData().getDriverName().contains("SQLite"));
        }
        check("no database server needed: the data lives in one file", dbFile.isFile());
    }

    private static boolean sqlFails(String sql) {
        try (Connection con = Database.getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate(sql);
            return false;
        } catch (SQLException e) {
            return true;
        }
    }

    private static String httpGet(String url, int expectedStatus) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection(Proxy.NO_PROXY);
        con.setRequestMethod("GET");
        int status = con.getResponseCode();
        if (status != expectedStatus) {
            throw new IllegalStateException(url + " returned " + status + " instead of " + expectedStatus);
        }
        try (InputStream in = status >= 400 ? con.getErrorStream() : con.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int httpStatus(String url, String method) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection(Proxy.NO_PROXY);
        con.setRequestMethod(method);
        return con.getResponseCode();
    }

    // ------------------------------------------------------------------
    // PERSISTENCE ACROSS PROGRAM RESTARTS (two separate Java processes)
    // ------------------------------------------------------------------

    private static void persistWrite() throws Exception {
        section("Persistence, step 1: write records, then the program ends");
        System.setProperty("jobportal.data.dir", System.getProperty("java.io.tmpdir") + File.separator + "jobportal_persist_data");
        UserDao userDao = new UserDao();
        JobSeeker seeker = new JobSeeker("Persistent Person", "persist@mail.com", User.hashPassword("persist1"), "9000011111");
        seeker.setCity("Nagpur");
        userDao.registerJobSeeker(seeker);
        new ApplicationDao().applyForJob(1, seeker.getUserId());
        new JobDao().saveJob(seeker.getUserId(), 2);
        new NotificationDao().addNotification(seeker.getUserId(), "persist me");
        Job job = new Job(2, "Persistent Job", "stays after restart", "Pune", "Full-time", 7.0, "java", 0);
        new JobDao().addJob(job);
        check("records written (user id " + seeker.getUserId() + ")", seeker.getUserId() > 0);
    }

    private static void persistVerify() throws Exception {
        section("Persistence, step 2: NEW Java process reads the records");
        System.setProperty("jobportal.data.dir", System.getProperty("java.io.tmpdir") + File.separator + "jobportal_persist_data");
        UserDao userDao = new UserDao();
        JobSeeker seeker = (JobSeeker) userDao.login("persist@mail.com", "persist1");
        check("user is still there after restart", seeker != null && seeker.getCity().equals("Nagpur"));
        check("application is still there", new ApplicationDao().hasApplied(1, seeker.getUserId()));
        checkEquals("saved job is still there", 1, new JobDao().countSavedJobs(seeker.getUserId()));
        check("notification is still there", new NotificationDao().getNotifications(seeker.getUserId()).get(0).endsWith("persist me"));
        check("new job is still there", new JobDao().searchJobs("Persistent Job").size() == 1);
        checkEquals("sample data was NOT duplicated by the restart (users = 7 samples + 1)", 8, count("SELECT COUNT(*) FROM users"));
        checkEquals("sample data was NOT duplicated by the restart (jobs = 9 samples + 1)", 10, count("SELECT COUNT(*) FROM jobs"));
    }
}
