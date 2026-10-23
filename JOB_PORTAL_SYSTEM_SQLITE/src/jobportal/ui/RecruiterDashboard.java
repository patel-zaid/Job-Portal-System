package jobportal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import jobportal.dao.ApplicationDao;
import jobportal.dao.JobDao;
import jobportal.dao.UserDao;
import jobportal.exception.ValidationException;
import jobportal.model.Application;
import jobportal.model.Company;
import jobportal.model.Exportable;
import jobportal.model.Job;
import jobportal.model.JobSeeker;
import jobportal.model.Recruiter;
import jobportal.service.NotificationService;
import jobportal.util.FileHandler;
import jobportal.util.Validator;

/**
 * Dashboard of a Recruiter. It EXTENDS BaseDashboard and adds:
 * Home (statistics), My Jobs, Applicants, Company Profile, Notifications.
 */
public class RecruiterDashboard extends BaseDashboard {

    private final Recruiter recruiter;
    private final JobDao jobDao = new JobDao();
    private final ApplicationDao applicationDao = new ApplicationDao();
    private final UserDao userDao = new UserDao();

    // ----- Home page -----
    private final JLabel jobsLabel = new JLabel("0");
    private final JLabel openLabel = new JLabel("0");
    private final JLabel applicantsLabel = new JLabel("0");
    private final JLabel selectedLabel = new JLabel("0");
    private final UIHelper.BarChart statusChart = new UIHelper.BarChart("Applicants by status");
    private final DefaultTableModel latestModel = UIHelper.createTableModel(
            new String[]{"Applicant", "Job", "Match", "Status"});
    private final JTable latestTable = UIHelper.createTable(latestModel);

    // ----- My Jobs page -----
    private final DefaultTableModel jobsModel = UIHelper.createTableModel(new String[]{
        "ID", "Job Title", "Location", "Type", "Salary (LPA)", "Applicants", "Status", "Posted On"});
    private final JTable jobsTable = UIHelper.createTable(jobsModel);
    private List<Job> myJobs = new ArrayList<>();          // row i of the table = myJobs.get(i)

    // ----- Applicants page -----
    private final JComboBox<String> jobFilterCombo = new JComboBox<>();
    private boolean fillingCombo = false;                  // true while we refill the combo box
    private final DefaultTableModel applicantModel = UIHelper.createTableModel(new String[]{
        "App ID", "Applicant", "Email", "Job", "Exp (yrs)", "Skill Match", "Applied On", "Status"});
    private final JTable applicantTable = UIHelper.createTable(applicantModel);
    private List<Application> applications = new ArrayList<>();

    // ----- Company Profile page -----
    private final JTextField nameField = new JTextField(30);
    private final JTextField phoneField = new JTextField(30);
    private final JTextField designationField = new JTextField(30);
    private final JTextField companyNameField = new JTextField(30);
    private final JTextField industryField = new JTextField(30);
    private final JTextField locationField = new JTextField(30);
    private final JTextField websiteField = new JTextField(30);
    private final JTextArea descriptionArea = new JTextArea(4, 30);

    public RecruiterDashboard(Recruiter recruiter) {
        super(recruiter);
        this.recruiter = recruiter;

        addPage("Home", createHomePanel());
        addPage("My Jobs", createJobsPanel());
        addPage("Applicants", createApplicantsPanel());
        addPage("Company Profile", createProfilePanel());
        addPage("Notifications", createNotificationPanel());

        enableNotifications();
        showPage("Home");
    }

    /** Implementation of the abstract method of BaseDashboard. */
    @Override
    protected void onPageShown(String pageName) {
        if (pageName.equals("Home")) {
            loadHome();
        } else if (pageName.equals("My Jobs")) {
            loadMyJobs();
        } else if (pageName.equals("Applicants")) {
            loadApplicantsPage();
        } else if (pageName.equals("Company Profile")) {
            loadProfileFields();
        } else if (pageName.equals("Notifications")) {
            loadNotifications();
        }
    }

    // ==================================================================
    // HOME  (basic recruitment statistics)
    // ==================================================================

    private JPanel createHomePanel() {
        JPanel page = UIHelper.createPagePanel("Recruitment Overview");

        JPanel cards = new JPanel(new GridLayout(1, 4, 15, 0));
        cards.setOpaque(false);
        cards.add(UIHelper.createStatCard("Jobs Posted", jobsLabel, UIHelper.PRIMARY));
        cards.add(UIHelper.createStatCard("Open Jobs", openLabel, UIHelper.SUCCESS));
        cards.add(UIHelper.createStatCard("Total Applicants", applicantsLabel, UIHelper.WARNING));
        cards.add(UIHelper.createStatCard("Candidates Selected", selectedLabel, new Color(150, 90, 190)));

        UIHelper.setColumnWidths(latestTable, new int[]{120, 150, 55, 120});
        UIHelper.applyStatusRenderer(latestTable, 3);
        JPanel latestSection = UIHelper.createSection("Latest applicants", UIHelper.wrapTable(latestTable));
        JPanel chartSection = UIHelper.createSection("Hiring pipeline", statusChart);

        JPanel middle = new JPanel(new GridLayout(1, 2, 15, 0));
        middle.setOpaque(false);
        middle.add(latestSection);
        middle.add(chartSection);

        JPanel body = new JPanel(new BorderLayout(0, 15));
        body.setOpaque(false);
        body.add(cards, BorderLayout.NORTH);
        body.add(middle, BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void loadHome() {
        try {
            int recruiterId = recruiter.getUserId();
            List<Job> jobs = jobDao.getJobsByRecruiter(recruiterId);

            int openJobs = 0;
            int totalApplicants = 0;
            for (Job job : jobs) {
                if (job.isOpen()) {
                    openJobs++;
                }
                totalApplicants = totalApplicants + job.getApplicantCount();
            }

            Map<String, Integer> counts = applicationDao.getStatusCountsForRecruiter(recruiterId);
            jobsLabel.setText(String.valueOf(jobs.size()));
            openLabel.setText(String.valueOf(openJobs));
            applicantsLabel.setText(String.valueOf(totalApplicants));
            selectedLabel.setText(String.valueOf(counts.get(Application.STATUS_SELECTED)));
            statusChart.setData(counts);

            List<Application> all = applicationDao.getApplicationsForRecruiter(recruiterId);
            latestModel.setRowCount(0);
            int limit = Math.min(8, all.size());               // show only the newest 8
            for (int i = 0; i < limit; i++) {
                Application app = all.get(i);
                latestModel.addRow(new Object[]{
                    app.getSeeker().getFullName(), app.getJob().getTitle(),
                    app.getMatchPercent() + "%", app.getStatus()
                });
            }
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    // ==================================================================
    // MY JOBS  (post / edit / close / delete)
    // ==================================================================

    private JPanel createJobsPanel() {
        JPanel page = UIHelper.createPagePanel("My Jobs");

        UIHelper.setColumnWidths(jobsTable, new int[]{40, 220, 100, 90, 90, 80, 80, 100});
        UIHelper.applyStatusRenderer(jobsTable, 6);

        JButton postButton = UIHelper.createButton("Post New Job", UIHelper.SUCCESS);
        postButton.addActionListener(e -> postJob());
        JButton editButton = UIHelper.createButton("Edit Job");
        editButton.addActionListener(e -> editJob());
        JButton statusButton = UIHelper.createButton("Close / Reopen", UIHelper.WARNING);
        statusButton.addActionListener(e -> toggleJobStatus());
        JButton deleteButton = UIHelper.createButton("Delete Job", UIHelper.DANGER);
        deleteButton.addActionListener(e -> deleteJob());

        JPanel buttons = UIHelper.createButtonBar();
        buttons.add(postButton);
        buttons.add(editButton);
        buttons.add(statusButton);
        buttons.add(deleteButton);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(buttons, BorderLayout.NORTH);
        body.add(UIHelper.wrapTable(jobsTable), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void loadMyJobs() {
        try {
            myJobs = jobDao.getJobsByRecruiter(recruiter.getUserId());
            jobsModel.setRowCount(0);
            for (Job job : myJobs) {
                jobsModel.addRow(new Object[]{
                    job.getJobId(), job.getTitle(), job.getLocation(), job.getJobType(),
                    job.getSalaryLpa(), job.getApplicantCount(), job.getStatus(), job.getPostedDate()
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
        return myJobs.get(row);
    }

    private void postJob() {
        JobFormDialog dialog = new JobFormDialog(this, recruiter.getUserId(), null);
        dialog.setVisible(true);                       // modal: waits here until the dialog closes
        Job newJob = dialog.getSavedJob();
        if (newJob == null) {
            return;                                    // user pressed Cancel
        }
        try {
            jobDao.addJob(newJob);
            FileHandler.writeLog(recruiter, "POSTED JOB " + newJob.getTitle());
            loadMyJobs();
            UIHelper.showInfo(this, "Job posted successfully.");
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    private void editJob() {
        Job selected = getSelectedJob();
        if (selected == null) {
            return;
        }
        JobFormDialog dialog = new JobFormDialog(this, recruiter.getUserId(), selected);
        dialog.setVisible(true);
        Job changedJob = dialog.getSavedJob();
        if (changedJob == null) {
            return;
        }
        try {
            jobDao.updateJob(changedJob);
            FileHandler.writeLog(recruiter, "EDITED JOB #" + changedJob.getJobId());
            loadMyJobs();
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
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
            FileHandler.writeLog(recruiter, "JOB #" + selected.getJobId() + " set to " + newStatus);
            loadMyJobs();
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    private void deleteJob() {
        Job selected = getSelectedJob();
        if (selected == null) {
            return;
        }
        if (!UIHelper.confirm(this, "Delete \"" + selected.getTitle() + "\"?\n"
                + "All applications for this job will also be deleted.")) {
            return;
        }
        try {
            jobDao.deleteJob(selected.getJobId());
            FileHandler.writeLog(recruiter, "DELETED JOB #" + selected.getJobId());
            loadMyJobs();
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    // ==================================================================
    // APPLICANTS  (view, change status, export)
    // ==================================================================

    private JPanel createApplicantsPanel() {
        JPanel page = UIHelper.createPagePanel("Applicants");

        UIHelper.setColumnWidths(applicantTable, new int[]{55, 130, 170, 170, 70, 85, 90, 100});
        UIHelper.applyStatusRenderer(applicantTable, 7);

        jobFilterCombo.addActionListener(e -> {
            if (!fillingCombo) {
                loadApplicants();
            }
        });

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filterBar.setBackground(Color.WHITE);
        filterBar.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER));
        filterBar.add(new JLabel("Show applicants of:"));
        filterBar.add(jobFilterCombo);

        JButton profileButton = UIHelper.createButton("View Applicant");
        profileButton.addActionListener(e -> viewApplicant());
        JButton statusButton = UIHelper.createButton("Update Status", UIHelper.SUCCESS);
        statusButton.addActionListener(e -> updateStatus());
        JButton exportButton = UIHelper.createButton("Export CSV", UIHelper.GRAY);
        exportButton.addActionListener(e -> exportApplicants());

        JPanel buttons = UIHelper.createButtonBar();
        buttons.add(profileButton);
        buttons.add(statusButton);
        buttons.add(exportButton);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(filterBar, BorderLayout.NORTH);
        body.add(UIHelper.wrapTable(applicantTable), BorderLayout.CENTER);
        body.add(buttons, BorderLayout.SOUTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    /** Refills the job combo box, then loads the applicants. */
    private void loadApplicantsPage() {
        try {
            myJobs = jobDao.getJobsByRecruiter(recruiter.getUserId());
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
            return;
        }
        fillingCombo = true;                     // stop the combo listener while we change the items
        jobFilterCombo.removeAllItems();
        jobFilterCombo.addItem("All my jobs");
        for (Job job : myJobs) {
            jobFilterCombo.addItem("#" + job.getJobId() + " - " + job.getTitle());
        }
        fillingCombo = false;
        loadApplicants();
    }

    private void loadApplicants() {
        try {
            int selectedIndex = jobFilterCombo.getSelectedIndex();
            if (selectedIndex <= 0) {
                applications = applicationDao.getApplicationsForRecruiter(recruiter.getUserId());
            } else {
                Job job = myJobs.get(selectedIndex - 1);     // index 0 is "All my jobs"
                applications = applicationDao.getApplicationsForJob(job.getJobId());
            }
            applicantModel.setRowCount(0);
            for (Application app : applications) {
                JobSeeker seeker = app.getSeeker();
                applicantModel.addRow(new Object[]{
                    app.getApplicationId(), seeker.getFullName(), seeker.getEmail(),
                    app.getJob().getTitle(), seeker.getExperienceYears(),
                    app.getMatchPercent() + "%", app.getAppliedDate(), app.getStatus()
                });
            }
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    private Application getSelectedApplication() {
        int row = applicantTable.getSelectedRow();
        if (row < 0) {
            UIHelper.showInfo(this, "Please select an applicant from the table first.");
            return null;
        }
        return applications.get(row);
    }

    private void viewApplicant() {
        Application app = getSelectedApplication();
        if (app == null) {
            return;
        }
        JobSeeker seeker = app.getSeeker();
        StringBuilder text = new StringBuilder();
        text.append("Name         : ").append(seeker.getFullName()).append("\n");
        text.append("Email        : ").append(seeker.getEmail()).append("\n");
        text.append("Phone        : ").append(seeker.getPhone()).append("\n");
        text.append("City         : ").append(seeker.getCity()).append("\n");
        text.append("Education    : ").append(seeker.getEducation()).append("\n");
        text.append("Experience   : ").append(seeker.getExperienceYears()).append(" year(s)\n");
        text.append("Skills       : ").append(seeker.getSkills()).append("\n");
        text.append("Resume       : ");
        if (seeker.getResumePath().isEmpty()) {
            text.append("not uploaded\n");
        } else {
            text.append(seeker.getResumePath()).append("\n");
        }
        text.append("\nApplied for  : ").append(app.getJob().getTitle()).append("\n");
        text.append("Job skills   : ").append(app.getJob().getRequiredSkills()).append("\n");
        text.append("Skill match  : ").append(app.getMatchPercent()).append("%\n");
        text.append("Status       : ").append(app.getStatus()).append("\n");

        JTextArea area = new JTextArea(text.toString(), 13, 42);
        area.setEditable(false);
        area.setFont(UIHelper.FONT_NORMAL);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Applicant Details", JOptionPane.PLAIN_MESSAGE);
    }

    private void updateStatus() {
        Application app = getSelectedApplication();
        if (app == null) {
            return;
        }
        Object choice = JOptionPane.showInputDialog(this,
                "New status for " + app.getSeeker().getFullName() + " (" + app.getJob().getTitle() + "):",
                "Update Application Status", JOptionPane.QUESTION_MESSAGE,
                null, Application.STATUS_LIST, app.getStatus());
        if (choice == null) {
            return;                                     // Cancel pressed
        }
        String newStatus = (String) choice;
        if (newStatus.equals(app.getStatus())) {
            return;                                     // nothing changed
        }
        try {
            applicationDao.updateStatus(app.getApplicationId(), newStatus);
            FileHandler.writeLog(recruiter, "APPLICATION #" + app.getApplicationId() + " set to " + newStatus);

            // Inform the job seeker in the background (thread pool)
            NotificationService.sendAsync(app.getSeeker().getUserId(),
                    "Your application for " + app.getJob().getTitle() + " at "
                    + app.getJob().getCompanyName() + " is now " + newStatus);

            loadApplicants();
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    private void exportApplicants() {
        if (applications.isEmpty()) {
            UIHelper.showInfo(this, "There are no applicants to export.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("applicants.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getPath() + ".csv");
        }
        try {
            // List -> array of Exportable (Application implements Exportable)
            Exportable[] rows = applications.toArray(new Exportable[0]);
            FileHandler.exportToCsv(rows, file);
            FileHandler.writeLog(recruiter, "EXPORTED " + rows.length + " applicants to CSV");
            UIHelper.showInfo(this, "Exported " + rows.length + " applicants to:\n" + file.getPath());
        } catch (IOException e) {
            UIHelper.showError(this, "Could not write the file:\n" + e.getMessage());
        }
    }

    // ==================================================================
    // COMPANY PROFILE
    // ==================================================================

    private JPanel createProfilePanel() {
        JPanel page = UIHelper.createPagePanel("Company Profile");

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        UIHelper.addFormRow(form, 0, "Your Name", nameField);
        UIHelper.addFormRow(form, 1, "Your Phone", phoneField);
        UIHelper.addFormRow(form, 2, "Designation", designationField);
        UIHelper.addFormRow(form, 3, "Company Name", companyNameField);
        UIHelper.addFormRow(form, 4, "Industry", industryField);
        UIHelper.addFormRow(form, 5, "Company Location", locationField);
        UIHelper.addFormRow(form, 6, "Website", websiteField);
        UIHelper.addFormRow(form, 7, "About Company", new JScrollPane(descriptionArea));

        JButton saveButton = UIHelper.createButton("Save Profile", UIHelper.SUCCESS);
        saveButton.addActionListener(e -> saveProfile());
        JPanel buttons = UIHelper.createButtonBar();
        buttons.add(saveButton);

        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);
        left.add(form, BorderLayout.NORTH);
        left.add(buttons, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.add(left);
        page.add(wrapper, BorderLayout.CENTER);
        return page;
    }

    private void loadProfileFields() {
        Company company = recruiter.getCompany();
        nameField.setText(recruiter.getFullName());
        phoneField.setText(recruiter.getPhone());
        designationField.setText(recruiter.getDesignation());
        companyNameField.setText(company.getName());
        industryField.setText(company.getIndustry());
        locationField.setText(company.getLocation());
        websiteField.setText(company.getWebsite());
        descriptionArea.setText(company.getDescription());
    }

    private void saveProfile() {
        try {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String designation = designationField.getText().trim();
            String companyName = companyNameField.getText().trim();
            String industry = industryField.getText().trim();
            String location = locationField.getText().trim();
            String website = websiteField.getText().trim();
            String description = descriptionArea.getText().trim();

            Validator.checkNotEmpty(name, "Your name");
            Validator.checkMaxLength(name, 100, "Your name");
            Validator.checkPhone(phone);
            Validator.checkNotEmpty(companyName, "Company name");
            Validator.checkMaxLength(designation, 80, "Designation");
            Validator.checkMaxLength(companyName, 100, "Company name");
            Validator.checkMaxLength(industry, 80, "Industry");
            Validator.checkMaxLength(location, 80, "Company location");
            Validator.checkMaxLength(website, 120, "Website");
            Validator.checkMaxLength(description, 500, "About company");

            recruiter.setFullName(name);
            recruiter.setPhone(phone);
            recruiter.setDesignation(designation);
            Company company = recruiter.getCompany();
            company.setName(companyName);
            company.setIndustry(industry);
            company.setLocation(location);
            company.setWebsite(website);
            company.setDescription(description);

            userDao.updateRecruiter(recruiter);
            FileHandler.writeLog(recruiter, "COMPANY PROFILE UPDATED");
            UIHelper.showInfo(this, "Profile saved successfully.");

        } catch (ValidationException e) {
            UIHelper.showWarning(this, e.getMessage());
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }
}
