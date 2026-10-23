package jobportal;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import jobportal.dao.ApplicationDao;
import jobportal.dao.Database;
import jobportal.dao.JobDao;
import jobportal.dao.NotificationDao;
import jobportal.dao.UserDao;
import jobportal.model.Application;
import jobportal.model.Job;
import jobportal.model.JobSeeker;
import jobportal.model.User;
import jobportal.ui.AdminDashboard;
import jobportal.ui.JobSeekerDashboard;
import jobportal.ui.LoginFrame;
import jobportal.ui.RecruiterDashboard;

/**
 * Drives the REAL Swing screens (login, registration, the three dashboards) with simulated clicks and typing,
 * and checks every result directly in the SQLite database.
 *
 * It needs a screen. On a machine without a screen (Linux server) start it with:  xvfb-run -a java ...
 * Message boxes (JOptionPane) are answered automatically by a helper thread.
 *
 * Optional: -Dui.screenshots=folder  saves a PNG of every screen that was visited.
 */
public class UiSmokeTest {

    private static int passed = 0;
    private static int failed = 0;

    private static final List<String> messages = new CopyOnWriteArrayList<>();
    private static final Map<String, Consumer<JDialog>> dialogScripts = new ConcurrentHashMap<>();
    private static volatile String nextInputChoice = null;
    private static volatile boolean answerYes = true;
    private static File shotDir = null;
    private static int shotNumber = 0;

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("SKIPPED: no screen available. Run with:  xvfb-run -a java ...  (or on a normal desktop).");
            System.exit(0);
        }
        Path temp = Files.createTempDirectory("jobportal_ui_test_");
        System.setProperty("jobportal.db.url", "jdbc:sqlite:" + temp.resolve("ui_test.db").toString().replace('\\', '/'));
        System.setProperty("jobportal.data.dir", temp.resolve("data").toString());
        if (System.getProperty("ui.screenshots") != null) {
            shotDir = new File(System.getProperty("ui.screenshots"));
            shotDir.mkdirs();
        }
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        startDialogHelper();

        try {
            runTests();
        } catch (Throwable t) {
            failed++;
            System.out.println("  FAIL  unexpected exception: " + t);
            t.printStackTrace(System.out);
        }
        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------

    private static void runTests() throws Exception {
        Database.testConnection();                  // same start-up step as MainApp
        UserDao userDao = new UserDao();
        JobDao jobDao = new JobDao();
        ApplicationDao appDao = new ApplicationDao();
        NotificationDao notDao = new NotificationDao();

        section("1. Login screen");
        LoginFrame login = onEdt(() -> {
            LoginFrame f = new LoginFrame();
            f.setVisible(true);
            return f;
        });
        sleep(300);
        shot("login", login);
        check("login window is visible", login.isShowing());

        messages.clear();
        doLogin(login, "admin@jobportal.com", "wrong-password");
        waitFor(() -> !messages.isEmpty(), 3000);
        check("wrong password shows 'Invalid email or password.'", messages.contains("Invalid email or password."));
        check("login window stays open after a failed login", login.isShowing());

        messages.clear();
        doLogin(login, "", "");
        waitFor(() -> !messages.isEmpty(), 3000);
        check("empty fields show a validation message", messages.contains("Email cannot be empty."));

        // ---------------- registration dialog ----------------
        section("2. Registration dialog (writes to SQLite)");
        dialogScripts.put("Create New Account", dlg -> {
            List<JTextField> fields = findAll(dlg, JTextField.class);
            JTextField name = fields.get(0), email = fields.get(1), phone = fields.get(2);
            name.setText("Ui Tester");
            email.setText("ui.tester@mail.com");
            phone.setText("9988776655");
            List<JPasswordField> pw = findAll(dlg, JPasswordField.class);
            pw.get(0).setText("uitest1");
            pw.get(1).setText("uitest1");
            findButton(dlg, "Register").doClick();
        });
        messages.clear();
        clickAsync(findButton(login, "Create New Account"));
        waitFor(() -> messages.contains("Registration successful! You can login now."), 8000);
        check("registration success message was shown", messages.contains("Registration successful! You can login now."));
        sleep(300);
        User registered = userDao.login("ui.tester@mail.com", "uitest1");
        check("new account exists in the SQLite database", registered instanceof JobSeeker && registered.getFullName().equals("Ui Tester"));
        check("registration filled the email field of the login screen",
                findAll(login, JTextField.class).get(0).getText().equals("ui.tester@mail.com"));

        // ---------------- admin ----------------
        section("3. Admin dashboard");
        AdminDashboard admin = (AdminDashboard) loginAs(login, "admin@jobportal.com", "admin123", AdminDashboard.class);
        check("admin dashboard opened", admin != null && admin.isShowing());
        shot("admin_dashboard", admin);
        click(admin, "Manage Users");
        JTable usersTable = showingTable(admin);
        check("Manage Users lists all 8 users (7 sample + 1 registered)", usersTable.getRowCount() == 8);
        shot("admin_users", admin);
        int rowOfPriya = rowWithText(usersTable, "priya@student.com");
        check("Priya found in the users table", rowOfPriya >= 0);
        selectRow(usersTable, rowOfPriya);
        clickAsync(findButton(admin, "Block / Unblock"));
        waitFor(() -> isBlocked(userDao, "priya@student.com"), 5000);
        check("Block button wrote status BLOCKED into the database", isBlocked(userDao, "priya@student.com"));
        sleep(300);
        usersTable = showingTable(admin);
        selectRow(usersTable, rowWithText(usersTable, "priya@student.com"));
        clickAsync(findButton(admin, "Block / Unblock"));
        waitFor(() -> !isBlocked(userDao, "priya@student.com"), 5000);
        check("Block / Unblock again restored ACTIVE", !isBlocked(userDao, "priya@student.com"));

        click(admin, "Manage Jobs");
        check("Manage Jobs lists all 9 jobs", showingTable(admin).getRowCount() == 9);
        shot("admin_jobs", admin);
        click(admin, "Activity Log");
        sleep(300);
        String logText = findAll(admin, JTextArea.class).stream().filter(Component::isShowing)
                .map(JTextArea::getText).findFirst().orElse("");
        check("Activity Log shows the LOGIN of the admin", logText.contains("ADMIN admin@jobportal.com : LOGIN"));
        shot("admin_log", admin);
        click(admin, "Dashboard");
        shot("admin_dashboard_again", admin);
        LoginFrame login2 = logout(admin);
        check("Logout returns to the login screen", login2 != null && login2.isShowing());

        // ---------------- recruiter ----------------
        section("4. Recruiter dashboard (TechNova)");
        RecruiterDashboard rec = (RecruiterDashboard) loginAs(login2, "hr@technova.com", "recruit123", RecruiterDashboard.class);
        check("recruiter dashboard opened", rec != null && rec.isShowing());
        shot("recruiter_home", rec);
        click(rec, "My Jobs");
        check("My Jobs shows the 3 jobs of TechNova", showingTable(rec).getRowCount() == 3);
        dialogScripts.put("Post New Job", dlg -> {
            List<JTextField> f = findAll(dlg, JTextField.class);   // title, location, salary, experience, skills
            f.get(0).setText("UI Test Engineer");
            f.get(1).setText("Chennai");
            f.get(2).setText("4.75");
            f.get(3).setText("2");
            f.get(4).setText("java, selenium");
            findAll(dlg, JTextArea.class).get(0).setText("Created by the automatic UI test.");
            findButton(dlg, "Save Job").doClick();
        });
        messages.clear();
        clickAsync(findButton(rec, "Post New Job"));
        waitFor(() -> messages.contains("Job posted successfully."), 8000);
        check("'Job posted successfully.' was shown", messages.contains("Job posted successfully."));
        List<Job> hrJobs = jobDao.getJobsByRecruiter(2);
        check("posted job was written to SQLite", hrJobs.size() == 4 && hrJobs.get(0).getTitle().equals("UI Test Engineer")
                && hrJobs.get(0).getSalaryLpa() == 4.75 && hrJobs.get(0).getLocation().equals("Chennai"));
        sleep(300);
        check("job table refreshed (4 rows)", showingTable(rec).getRowCount() == 4);
        shot("recruiter_jobs", rec);

        // close / reopen
        JTable jt = showingTable(rec);
        selectRow(jt, rowWithText(jt, "UI Test Engineer"));
        clickAsync(findButton(rec, "Close / Reopen"));
        int newJobId = hrJobs.get(0).getJobId();
        waitFor(() -> jobDao.getJobById(newJobId).getStatus().equals("CLOSED"), 5000);
        check("Close button set the job to CLOSED in SQLite", jobDao.getJobById(newJobId).getStatus().equals("CLOSED"));
        sleep(300);
        jt = showingTable(rec);
        selectRow(jt, rowWithText(jt, "UI Test Engineer"));
        clickAsync(findButton(rec, "Close / Reopen"));
        waitFor(() -> jobDao.getJobById(newJobId).getStatus().equals("OPEN"), 5000);
        check("Reopen set the job back to OPEN", jobDao.getJobById(newJobId).getStatus().equals("OPEN"));

        // applicants + status change
        click(rec, "Applicants");
        sleep(300);
        JTable at = showingTable(rec);
        check("Applicants page lists the 5 applications for TechNova", at.getRowCount() == 5);
        shot("recruiter_applicants", rec);
        int amitRow = rowWithText(at, "Amit Patel");
        check("Amit Patel (applied for Junior Software Engineer) is listed", amitRow >= 0);
        selectRow(at, amitRow);
        int notificationsBefore = notDao.getNotifications(7).size();
        nextInputChoice = "INTERVIEW";
        clickAsync(findButton(rec, "Update Status"));
        waitFor(() -> statusOfAmitJob2(appDao).equals("INTERVIEW"), 5000);
        check("Update Status wrote INTERVIEW into SQLite", statusOfAmitJob2(appDao).equals("INTERVIEW"));
        waitFor(() -> notDao.getNotifications(7).size() > notificationsBefore, 5000);
        check("seeker Amit received a notification (background thread)", notDao.getNotifications(7).size() == notificationsBefore + 1
                && notDao.getNotifications(7).get(0).contains("Junior Software Engineer") && notDao.getNotifications(7).get(0).contains("INTERVIEW"));

        selectRow(at = showingTable(rec), rowWithText(showingTable(rec), "Amit Patel"));
        messages.clear();
        clickAsync(findButton(rec, "View Applicant"));
        waitFor(() -> !messages.isEmpty(), 5000);
        check("View Applicant shows the applicant details", messages.stream().anyMatch(m -> m.contains("amit@student.com") && m.contains("Skill match")));

        click(rec, "Company Profile");
        shot("recruiter_company", rec);
        check("Company Profile shows TechNova",
                findAll(rec, JTextField.class).stream().filter(Component::isShowing).anyMatch(f -> f.getText().equals("TechNova Solutions")));
        click(rec, "Notifications");
        shot("recruiter_notifications", rec);

        // delete job through the UI (confirm dialog answered with YES)
        click(rec, "My Jobs");
        jt = showingTable(rec);
        selectRow(jt, rowWithText(jt, "UI Test Engineer"));
        answerYes = true;
        clickAsync(findButton(rec, "Delete Job"));
        waitFor(() -> jobDao.getJobById(newJobId) == null, 5000);
        check("Delete Job removed the job from SQLite", jobDao.getJobById(newJobId) == null);
        LoginFrame login3 = logout(rec);

        // ---------------- job seeker ----------------
        section("5. Job seeker dashboard (Amit)");
        JobSeekerDashboard seeker = (JobSeekerDashboard) loginAs(login3, "amit@student.com", "seeker123", JobSeekerDashboard.class);
        check("job seeker dashboard opened", seeker != null && seeker.isShowing());
        shot("seeker_home", seeker);

        click(seeker, "Browse Jobs");
        sleep(300);
        check("Browse Jobs shows the 8 open jobs", showingTable(seeker).getRowCount() == 8);
        JTextField keyword = findAll(seeker, JTextField.class).stream().filter(f -> f.isShowing() && f.getColumns() == 16).findFirst().get();
        onEdt(() -> { keyword.setText("JAVA"); return null; });
        clickAsync(findButton(seeker, "Search"));
        sleep(500);
        int javaRows = showingTable(seeker).getRowCount();
        check("search 'JAVA' (capitals) finds the Java jobs", javaRows == jobDao.searchJobs("java").size() && javaRows >= 3);
        shot("seeker_browse_java", seeker);

        JTable bt = showingTable(seeker);
        int internRow = rowWithText(bt, "Java Intern");
        check("Java Intern is in the result", internRow >= 0);
        // Amit has not applied for Java Intern (job 3) yet
        selectRow(bt, internRow);
        messages.clear();
        answerYes = true;
        clickAsync(findButton(seeker, "Apply Now"));
        waitFor(() -> appDao.hasApplied(3, 7), 5000);
        check("Apply Now wrote the application into SQLite", appDao.hasApplied(3, 7));
        waitFor(() -> messages.stream().anyMatch(m -> m.startsWith("Application submitted successfully!")), 5000);
        check("success message was shown", messages.stream().anyMatch(m -> m.startsWith("Application submitted successfully!")));
        sleep(300);
        selectRow(bt = showingTable(seeker), rowWithText(showingTable(seeker), "Java Intern"));
        messages.clear();
        clickAsync(findButton(seeker, "Apply Now"));
        waitFor(() -> messages.stream().anyMatch(m -> m.contains("already applied")), 5000);
        check("applying twice shows 'already applied'", messages.stream().anyMatch(m -> m.contains("already applied")));
        check("still only one application in SQLite", appDao.getApplicationsBySeeker(7).stream().filter(a -> a.getJob().getJobId() == 3).count() == 1);

        selectRow(bt = showingTable(seeker), rowWithText(showingTable(seeker), "Java Developer"));
        int savedBefore = jobDao.countSavedJobs(7);
        messages.clear();
        clickAsync(findButton(seeker, "Save Job"));
        waitFor(() -> jobDao.countSavedJobs(7) > savedBefore, 5000);
        check("Save Job wrote the bookmark into SQLite", jobDao.countSavedJobs(7) == savedBefore + 1);
        selectRow(bt = showingTable(seeker), rowWithText(showingTable(seeker), "Java Developer"));
        messages.clear();
        clickAsync(findButton(seeker, "Save Job"));
        waitFor(() -> messages.stream().anyMatch(m -> m.contains("already saved")), 5000);
        check("saving twice shows 'already saved'", messages.stream().anyMatch(m -> m.contains("already saved")));

        click(seeker, "My Applications");
        sleep(300);
        check("My Applications shows 4 rows (3 sample + Java Intern)", showingTable(seeker).getRowCount() == 4);
        shot("seeker_applications", seeker);
        click(seeker, "Saved Jobs");
        sleep(300);
        check("Saved Jobs shows 2 rows (1 old + Java Developer)", showingTable(seeker).getRowCount() == 2);
        shot("seeker_saved", seeker);
        JTable st = showingTable(seeker);
        selectRow(st, rowWithText(st, "Java Developer"));
        clickAsync(findButton(seeker, "Remove"));
        waitFor(() -> jobDao.countSavedJobs(7) == savedBefore, 5000);
        check("Remove deleted the bookmark in SQLite", jobDao.countSavedJobs(7) == savedBefore);

        click(seeker, "My Profile");
        shot("seeker_profile", seeker);
        List<JTextField> pf = new ArrayList<>();
        for (JTextField f : findAll(seeker, JTextField.class)) {
            if (f.isShowing() && f.getColumns() == 30) {
                pf.add(f);
            }
        }
        check("profile page has the 7 profile text fields", pf.size() == 7);
        JTextField city = pf.get(6);
        check("profile shows the stored city (Mumbai)", city.getText().equals("Mumbai"));
        onEdt(() -> { city.setText("Nashik"); return null; });
        messages.clear();
        clickAsync(findButton(seeker, "Save Profile"));
        waitFor(() -> messages.contains("Profile saved successfully."), 5000);
        check("Save Profile wrote the new city into SQLite", ((JobSeeker) userDao.login("amit@student.com", "seeker123")).getCity().equals("Nashik"));

        click(seeker, "Notifications");
        sleep(300);
        shot("seeker_notifications", seeker);
        clickAsync(findButton(seeker, "Mark All As Read"));
        waitFor(() -> unreadOf(notDao, 7) == 0, 5000);
        check("Mark All As Read cleared the unread notifications in SQLite", unreadOf(notDao, 7) == 0);
        sleep(300);
        LoginFrame login4 = logout(seeker);

        section("6. The newly registered account can log in through the screen");
        JobSeekerDashboard fresh = (JobSeekerDashboard) loginAs(login4, "ui.tester@mail.com", "uitest1", JobSeekerDashboard.class);
        check("registered user reaches the job seeker dashboard", fresh != null && fresh.isShowing());
    }

    // ------------------------------------------------------------------
    // helpers: screen driving
    // ------------------------------------------------------------------

    private static boolean isBlocked(UserDao dao, String email) {
        try {
            dao.login(email, email.startsWith("priya") ? "seeker123" : "x");
            return false;
        } catch (Exception e) {
            return e.getMessage() != null && e.getMessage().contains("blocked");
        }
    }

    private static String statusOfAmitJob2(ApplicationDao dao) {
        try {
            for (Application a : dao.getApplicationsBySeeker(7)) {
                if (a.getJob().getJobId() == 2) {
                    return a.getStatus();
                }
            }
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
        return "none";
    }

    private static int unreadOf(NotificationDao dao, int userId) {
        try {
            return dao.countUnread(userId);
        } catch (Exception e) {
            return -1;
        }
    }

    private static void doLogin(LoginFrame frame, String email, String password) throws Exception {
        List<JTextField> fields = findAll(frame, JTextField.class);
        JTextField emailField = fields.get(0);
        JPasswordField pwField = findAll(frame, JPasswordField.class).get(0);
        onEdt(() -> {
            emailField.setText(email);
            pwField.setText(password);
            return null;
        });
        clickAsync(findButton(frame, "LOGIN"));
        sleep(400);
    }

    /** Logs in and returns the dashboard window of the given class (null if it did not open). */
    private static Window loginAs(LoginFrame frame, String email, String password, Class<? extends Window> dashboardClass) throws Exception {
        doLogin(frame, email, password);
        waitFor(() -> findWindow(dashboardClass) != null, 8000);
        sleep(500);
        return findWindow(dashboardClass);
    }

    private static LoginFrame logout(Window dashboard) throws Exception {
        clickAsync(findButton(dashboard, "Logout"));
        waitFor(() -> findWindow(LoginFrame.class) != null, 5000);
        sleep(300);
        return (LoginFrame) findWindow(LoginFrame.class);
    }

    private static Window findWindow(Class<? extends Window> type) {
        for (Window w : Window.getWindows()) {
            if (type.isInstance(w) && w.isShowing()) {
                return w;
            }
        }
        return null;
    }

    private static void click(Window w, String buttonText) throws Exception {
        clickAsync(findButton(w, buttonText));
        sleep(450);
    }

    /** doClick on the Swing thread, but without waiting (a click can open a modal dialog). */
    private static void clickAsync(JButton button) {
        SwingUtilities.invokeLater(button::doClick);
        sleep(350);
    }

    private static JButton findButton(Container root, String text) {
        for (JButton b : findAll(root, JButton.class)) {
            if (text.equals(b.getText()) && b.isShowing()) {
                return b;
            }
        }
        for (JButton b : findAll(root, JButton.class)) {       // not showing yet (inside a dialog that is being built)
            if (text.equals(b.getText())) {
                return b;
            }
        }
        throw new IllegalStateException("Button not found: " + text);
    }

    private static JTable showingTable(Window dashboard) {
        for (JTable t : findAll(dashboard, JTable.class)) {
            if (t.isShowing()) {
                return t;
            }
        }
        throw new IllegalStateException("No table is showing");
    }

    private static int rowWithText(JTable table, String text) {
        for (int r = 0; r < table.getRowCount(); r++) {
            for (int c = 0; c < table.getColumnCount(); c++) {
                Object v = table.getValueAt(r, c);
                if (v != null && v.toString().contains(text)) {
                    return r;
                }
            }
        }
        return -1;
    }

    private static void selectRow(JTable table, int row) throws Exception {
        if (row < 0) {
            throw new IllegalStateException("row not found");
        }
        onEdt(() -> {
            table.setRowSelectionInterval(row, row);
            return null;
        });
    }

    private static <T extends Component> List<T> findAll(Container root, Class<T> type) {
        List<T> result = new ArrayList<>();
        collect(root, type, result);
        return result;
    }

    private static <T extends Component> void collect(Container c, Class<T> type, List<T> out) {
        for (Component child : c.getComponents()) {
            if (type.isInstance(child)) {
                out.add(type.cast(child));
            }
            if (child instanceof Container) {
                collect((Container) child, type, out);
            }
        }
    }

    /** Helper thread: answers message boxes and runs the scripts of our own dialogs. */
    private static void startDialogHelper() {
        Thread helper = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(80);
                    for (Window w : Window.getWindows()) {
                        if (!(w instanceof JDialog) || !w.isShowing()) {
                            continue;
                        }
                        JDialog dialog = (JDialog) w;
                        List<JOptionPane> panes = findAll(dialog, JOptionPane.class);
                        if (!panes.isEmpty()) {
                            JOptionPane pane = panes.get(0);
                            Object msg = pane.getMessage();
                            String text = msg instanceof String ? (String) msg : describe(msg);
                            messages.add(text);
                            SwingUtilities.invokeLater(() -> {
                                if (pane.getWantsInput() && nextInputChoice != null) {
                                    pane.setInputValue(nextInputChoice);
                                    nextInputChoice = null;
                                    pane.setValue(JOptionPane.OK_OPTION);
                                } else if (pane.getWantsInput()) {
                                    pane.setValue(JOptionPane.OK_OPTION);
                                } else if (pane.getOptionType() == JOptionPane.YES_NO_OPTION) {
                                    pane.setValue(answerYes ? JOptionPane.YES_OPTION : JOptionPane.NO_OPTION);
                                } else {
                                    pane.setValue(JOptionPane.OK_OPTION);
                                }
                            });
                            Thread.sleep(250);
                        } else if (dialogScripts.containsKey(dialog.getTitle())) {
                            Consumer<JDialog> script = dialogScripts.remove(dialog.getTitle());
                            SwingUtilities.invokeLater(() -> script.accept(dialog));
                            Thread.sleep(250);
                        }
                    }
                } catch (Exception e) {
                    // keep helping
                }
            }
        }, "dialog-helper");
        helper.setDaemon(true);
        helper.start();
    }

    private static String describe(Object message) {
        if (message instanceof Container) {
            StringBuilder sb = new StringBuilder();
            for (JTextArea area : findAll((Container) message, JTextArea.class)) {
                sb.append(area.getText());
            }
            return sb.toString();
        }
        return String.valueOf(message);
    }

    private static void shot(String name, Window window) throws Exception {
        if (shotDir == null || window == null) {
            return;
        }
        sleep(200);
        BufferedImage image = onEdt(() -> {
            BufferedImage img = new BufferedImage(window.getWidth(), window.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            window.printAll(g);
            g.dispose();
            return img;
        });
        shotNumber++;
        ImageIO.write(image, "png", new File(shotDir, String.format("%02d_%s.png", shotNumber, name)));
    }

    // ------------------------------------------------------------------
    // helpers: threads, waiting, assertions
    // ------------------------------------------------------------------

    private interface Condition {
        boolean test() throws Exception;
    }

    private static void waitFor(Condition condition, long millis) {
        long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            try {
                if (condition.test()) {
                    return;
                }
            } catch (Exception e) {
                // not yet
            }
            sleep(50);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static <T> T onEdt(java.util.concurrent.Callable<T> task) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return task.call();
        }
        final Object[] box = new Object[1];
        final Exception[] error = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                box[0] = task.call();
            } catch (Exception e) {
                error[0] = e;
            }
        });
        if (error[0] != null) {
            throw error[0];
        }
        @SuppressWarnings("unchecked")
        T result = (T) box[0];
        return result;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("== " + title);
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }
}
