package jobportal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import jobportal.MainApp;
import jobportal.dao.ApplicationDao;
import jobportal.dao.JobDao;
import jobportal.dao.UserDao;
import jobportal.model.Admin;
import jobportal.model.Exportable;
import jobportal.model.Job;
import jobportal.model.User;
import jobportal.util.FileHandler;
import jobportal.util.TextUtil;

/**
 * Dashboard of the Admin. It EXTENDS BaseDashboard and adds:
 * Dashboard (system statistics), Manage Users, Manage Jobs, Activity Log.
 */
public class AdminDashboard extends BaseDashboard {

    private final Admin admin;
    private final UserDao userDao = new UserDao();
    private final JobDao jobDao = new JobDao();
    private final ApplicationDao applicationDao = new ApplicationDao();

    // ----- Dashboard page -----
    private final JLabel seekersLabel = new JLabel("0");
    private final JLabel recruitersLabel = new JLabel("0");
    private final JLabel jobsLabel = new JLabel("0");
    private final JLabel openJobsLabel = new JLabel("0");
    private final JLabel applicationsLabel = new JLabel("0");
    private final UIHelper.BarChart systemChart = new UIHelper.BarChart("System overview");

    // ----- Manage Users page -----
    private final DefaultTableModel usersModel = UIHelper.createTableModel(
            new String[]{"ID", "Name", "Email", "Phone", "Role", "Status"});
    private final JTable usersTable = UIHelper.createTable(usersModel);
    private List<User> users = new ArrayList<>();

    // ----- Manage Jobs page -----
    private final DefaultTableModel jobsModel = UIHelper.createTableModel(
            new String[]{"ID", "Job Title", "Company", "Location", "Type", "Applicants", "Status", "Posted On"});
    private final JTable jobsTable = UIHelper.createTable(jobsModel);
    private List<Job> jobs = new ArrayList<>();

    // ----- Activity Log page -----
    private final JTextArea logArea = new JTextArea();

    public AdminDashboard(Admin admin) {
        super(admin);
        this.admin = admin;

        addPage("Dashboard", createDashboardPanel());
        addPage("Manage Users", createUsersPanel());
        addPage("Manage Jobs", createJobsPanel());
        addPage("Activity Log", createLogPanel());

        showPage("Dashboard");
    }

    /** Implementation of the abstract method of BaseDashboard. */
    @Override
    protected void onPageShown(String pageName) {
        if (pageName.equals("Dashboard")) {
            loadStatistics();
        } else if (pageName.equals("Manage Users")) {
            loadUsers();
        } else if (pageName.equals("Manage Jobs")) {
            loadJobs();
        } else if (pageName.equals("Activity Log")) {
            loadLog();
        }
    }

    // ==================================================================
    // DASHBOARD  (system statistics)
    // ==================================================================

    private JPanel createDashboardPanel() {
        JPanel page = UIHelper.createPagePanel("System Statistics");

        JPanel cards = new JPanel(new GridLayout(1, 5, 15, 0));
        cards.setOpaque(false);
        cards.add(UIHelper.createStatCard("Job Seekers", seekersLabel, UIHelper.PRIMARY));
        cards.add(UIHelper.createStatCard("Recruiters", recruitersLabel, new Color(150, 90, 190)));
        cards.add(UIHelper.createStatCard("Total Jobs", jobsLabel, UIHelper.WARNING));
        cards.add(UIHelper.createStatCard("Open Jobs", openJobsLabel, UIHelper.SUCCESS));
        cards.add(UIHelper.createStatCard("Applications", applicationsLabel, UIHelper.DANGER));

        JTextArea restInfo = new JTextArea(
                "The built-in REST API (JDK HttpServer) is running.\n\n"
              + "GET http://localhost:" + MainApp.REST_PORT + "/jobs\n"
              + "     returns all open jobs as JSON\n\n"
              + "GET http://localhost:" + MainApp.REST_PORT + "/jobs/1\n"
              + "     returns job number 1 as JSON\n\n"
              + "Open a link in a web browser to test it.");
        restInfo.setEditable(false);
        restInfo.setFont(new Font("Monospaced", Font.PLAIN, 12));
        restInfo.setForeground(UIHelper.TEXT_DARK);
        restInfo.setOpaque(false);

        JPanel middle = new JPanel(new GridLayout(1, 2, 15, 0));
        middle.setOpaque(false);
        middle.add(UIHelper.createSection("Overview chart", systemChart));
        middle.add(UIHelper.createSection("REST API", restInfo));

        JPanel body = new JPanel(new BorderLayout(0, 15));
        body.setOpaque(false);
        body.add(cards, BorderLayout.NORTH);
        body.add(middle, BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void loadStatistics() {
        try {
            int seekers = userDao.countUsers(User.ROLE_JOB_SEEKER);
            int recruiters = userDao.countUsers(User.ROLE_RECRUITER);
            int totalJobs = jobDao.countJobs();                      // overloaded method (no parameter)
            int openJobs = jobDao.countJobs(Job.STATUS_OPEN);        // overloaded method (with parameter)
            int closedJobs = jobDao.countJobs(Job.STATUS_CLOSED);
            int applications = applicationDao.countApplications();

            seekersLabel.setText(String.valueOf(seekers));
            recruitersLabel.setText(String.valueOf(recruiters));
            jobsLabel.setText(String.valueOf(totalJobs));
            openJobsLabel.setText(String.valueOf(openJobs));
            applicationsLabel.setText(String.valueOf(applications));

            Map<String, Integer> chartData = new LinkedHashMap<>();
            chartData.put("Seekers", seekers);
            chartData.put("Recruiters", recruiters);
            chartData.put("Open Jobs", openJobs);
            chartData.put("Closed Jobs", closedJobs);
            chartData.put("Applications", applications);
            systemChart.setData(chartData);
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    // ==================================================================
    // MANAGE USERS
    // ==================================================================

    private JPanel createUsersPanel() {
        JPanel page = UIHelper.createPagePanel("Manage Users");

        UIHelper.setColumnWidths(usersTable, new int[]{40, 160, 200, 100, 100, 80});
        UIHelper.applyStatusRenderer(usersTable, 5);

        JButton blockButton = UIHelper.createButton("Block / Unblock", UIHelper.WARNING);
        blockButton.addActionListener(e -> toggleBlockUser());
        JButton deleteButton = UIHelper.createButton("Delete User", UIHelper.DANGER);
        deleteButton.addActionListener(e -> deleteUser());
        JButton refreshButton = UIHelper.createButton("Refresh");
        refreshButton.addActionListener(e -> loadUsers());

        JPanel buttons = UIHelper.createButtonBar();
        buttons.add(blockButton);
        buttons.add(deleteButton);
        buttons.add(refreshButton);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(buttons, BorderLayout.NORTH);
        body.add(UIHelper.wrapTable(usersTable), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void loadUsers() {
        try {
            users = userDao.getAllUsers();
            usersModel.setRowCount(0);
            for (User u : users) {
                // getRole() is abstract in User: each child returns its own role (polymorphism)
                String roleText = TextUtil.capitalizeWords(u.getRole().replace('_', ' '));
                usersModel.addRow(new Object[]{
                    u.getUserId(), u.getFullName(), u.getEmail(), u.getPhone(), roleText, u.getStatus()
                });
            }
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    private User getSelectedUser() {
        int row = usersTable.getSelectedRow();
        if (row < 0) {
            UIHelper.showInfo(this, "Please select a user from the table first.");
            return null;
        }
        User selected = users.get(row);
        if (selected.getUserId() == admin.getUserId()) {
            UIHelper.showWarning(this, "You cannot block or delete your own admin account.");
            return null;
        }
        return selected;
    }

    private void toggleBlockUser() {
        User selected = getSelectedUser();
        if (selected == null) {
            return;
        }
        String newStatus;
        if (selected.isBlocked()) {
            newStatus = User.STATUS_ACTIVE;
        } else {
            newStatus = User.STATUS_BLOCKED;
        }
        try {
            userDao.updateUserStatus(selected.getUserId(), newStatus);
            FileHandler.writeLog(admin, "USER " + selected.getEmail() + " set to " + newStatus);
            loadUsers();
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    private void deleteUser() {
        User selected = getSelectedUser();
        if (selected == null) {
            return;
        }
        if (!UIHelper.confirm(this, "Delete user \"" + selected.getFullName() + "\"?\n"
                + "Their jobs, applications and notifications will also be deleted.")) {
            return;
        }
        try {
            userDao.deleteUser(selected.getUserId());
            FileHandler.writeLog(admin, "DELETED USER " + selected.getEmail());
            loadUsers();
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    // ==================================================================
    // MANAGE JOBS
    // ==================================================================

    private JPanel createJobsPanel() {
        JPanel page = UIHelper.createPagePanel("Manage Jobs");

        UIHelper.setColumnWidths(jobsTable, new int[]{40, 200, 150, 100, 90, 80, 80, 100});
        UIHelper.applyStatusRenderer(jobsTable, 6);

        JButton statusButton = UIHelper.createButton("Close / Reopen", UIHelper.WARNING);
        statusButton.addActionListener(e -> toggleJobStatus());
        JButton deleteButton = UIHelper.createButton("Delete Job", UIHelper.DANGER);
        deleteButton.addActionListener(e -> deleteJob());
        JButton exportButton = UIHelper.createButton("Export CSV", UIHelper.GRAY);
        exportButton.addActionListener(e -> exportJobs());
        JButton refreshButton = UIHelper.createButton("Refresh");
        refreshButton.addActionListener(e -> loadJobs());

        JPanel buttons = UIHelper.createButtonBar();
        buttons.add(statusButton);
        buttons.add(deleteButton);
        buttons.add(exportButton);
        buttons.add(refreshButton);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(buttons, BorderLayout.NORTH);
        body.add(UIHelper.wrapTable(jobsTable), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void loadJobs() {
        try {
            jobs = jobDao.getAllJobs();
            jobsModel.setRowCount(0);
            for (Job job : jobs) {
                jobsModel.addRow(new Object[]{
                    job.getJobId(), job.getTitle(), job.getCompanyName(), job.getLocation(),
                    job.getJobType(), job.getApplicantCount(), job.getStatus(), job.getPostedDate()
                });
            }
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    private Job getSelectedJob() {
        int row = jobsTable.getSelectedRow();
        if (row < 0) {
            UIHelper.showInfo(this, "Please select a job from the table first.");
            return null;
        }
        return jobs.get(row);
    }

    private void toggleJobStatus() {
        Job selected = getSelectedJob();
        if (selected == null) {
            return;
        }
        String newStatus;
        if (selected.isOpen()) {
            newStatus = Job.STATUS_CLOSED;
        } else {
            newStatus = Job.STATUS_OPEN;
        }
        try {
            jobDao.updateJobStatus(selected.getJobId(), newStatus);
            FileHandler.writeLog(admin, "JOB #" + selected.getJobId() + " set to " + newStatus);
            loadJobs();
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    private void deleteJob() {
        Job selected = getSelectedJob();
        if (selected == null) {
            return;
        }
        if (!UIHelper.confirm(this, "Delete job \"" + selected.getTitle() + "\"?\n"
                + "Its applications will also be deleted.")) {
            return;
        }
        try {
            jobDao.deleteJob(selected.getJobId());
            FileHandler.writeLog(admin, "DELETED JOB #" + selected.getJobId());
            loadJobs();
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    private void exportJobs() {
        if (jobs.isEmpty()) {
            UIHelper.showInfo(this, "There are no jobs to export.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("jobs.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getPath() + ".csv");
        }
        try {
            Exportable[] rows = jobs.toArray(new Exportable[0]);     // Job implements Exportable
            FileHandler.exportToCsv(rows, file);
            FileHandler.writeLog(admin, "EXPORTED " + rows.length + " jobs to CSV");
            UIHelper.showInfo(this, "Exported " + rows.length + " jobs to:\n" + file.getPath());
        } catch (IOException e) {
            UIHelper.showError(this, "Could not write the file:\n" + e.getMessage());
        }
    }

    // ==================================================================
    // ACTIVITY LOG  (read with RandomAccessFile)
    // ==================================================================

    private JPanel createLogPanel() {
        JPanel page = UIHelper.createPagePanel("Activity Log");

        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JButton refreshButton = UIHelper.createButton("Refresh");
        refreshButton.addActionListener(e -> loadLog());
        JLabel info = new JLabel("Last 200 lines of data/activity.log (written and read with RandomAccessFile)");
        info.setFont(UIHelper.FONT_SMALL);
        info.setForeground(UIHelper.TEXT_MUTED);

        JPanel top = UIHelper.createButtonBar();
        top.add(refreshButton);
        top.add(info);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(UIHelper.createSection("Recent activity", new JScrollPane(logArea)), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void loadLog() {
        String text = FileHandler.readRecentLog(200);
        logArea.setText(text);
        logArea.setCaretPosition(text.length());        // scroll to the newest line
    }
}
