package jobportal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import jobportal.dao.ApplicationDao;
import jobportal.dao.JobDao;
import jobportal.dao.UserDao;
import jobportal.exception.DuplicateRecordException;
import jobportal.exception.ValidationException;
import jobportal.model.Application;
import jobportal.model.Job;
import jobportal.model.JobSeeker;
import jobportal.model.UserPreferences;
import jobportal.service.NotificationService;
import jobportal.util.FileHandler;
import jobportal.util.TextUtil;
import jobportal.util.Validator;

/**
 * Dashboard of a Job Seeker. It EXTENDS BaseDashboard (inheritance) and adds its own pages:
 * Home, Browse Jobs, My Applications, Saved Jobs, My Profile, Notifications.
 */
public class JobSeekerDashboard extends BaseDashboard {

    private static final String[] JOB_TABLE_COLUMNS = {
        "ID", "Job Title", "Company", "Location", "Type", "Salary (LPA)", "Exp (yrs)", "Skill Match", "Posted On"
    };
    private static final int[] JOB_TABLE_WIDTHS = {40, 190, 140, 90, 85, 90, 70, 80, 90};

    private final JobSeeker seeker;                       // same object as "user" of the parent, but typed as JobSeeker
    private final JobDao jobDao = new JobDao();
    private final ApplicationDao applicationDao = new ApplicationDao();
    private final UserDao userDao = new UserDao();

    // ----- Home page -----
    private final JLabel totalLabel = new JLabel("0");
    private final JLabel progressLabel = new JLabel("0");
    private final JLabel selectedLabel = new JLabel("0");
    private final JLabel savedLabel = new JLabel("0");
    private final DefaultTableModel recommendedModel =
            UIHelper.createTableModel(new String[]{"Job Title", "Company", "Match"});
    private final UIHelper.BarChart statusChart = new UIHelper.BarChart("My applications by status");

    // ----- Browse Jobs page -----
    private final JTextField keywordField = new JTextField(16);
    private final JTextField locationField = new JTextField(10);
    private JComboBox<String> typeCombo;
    private final DefaultTableModel browseModel = UIHelper.createTableModel(JOB_TABLE_COLUMNS);
    private final JTable browseTable = UIHelper.createTable(browseModel);
    private List<Job> browseJobs = new ArrayList<>();     // row i of the table = browseJobs.get(i)

    // ----- My Applications page -----
    private final DefaultTableModel applicationModel = UIHelper.createTableModel(
            new String[]{"App ID", "Job Title", "Company", "Location", "Applied On", "Status"});
    private final JTable applicationTable = UIHelper.createTable(applicationModel);

    // ----- Saved Jobs page -----
    private final DefaultTableModel savedModel = UIHelper.createTableModel(JOB_TABLE_COLUMNS);
    private final JTable savedTable = UIHelper.createTable(savedModel);
    private List<Job> savedJobs = new ArrayList<>();

    // ----- My Profile page -----
    private final JTextField nameField = new JTextField(30);
    private final JTextField emailField = new JTextField(30);
    private final JTextField phoneField = new JTextField(30);
    private final JTextField skillsField = new JTextField(30);
    private final JTextField educationField = new JTextField(30);
    private final JTextField experienceField = new JTextField(30);
    private final JTextField cityField = new JTextField(30);
    private final JTextField resumeField = new JTextField(22);

    public JobSeekerDashboard(JobSeeker seeker) {
        super(seeker);                    // calls the constructor of BaseDashboard
        this.seeker = seeker;

        addPage("Home", createHomePanel());
        addPage("Browse Jobs", createBrowsePanel());
        addPage("My Applications", createApplicationsPanel());
        addPage("Saved Jobs", createSavedPanel());
        addPage("My Profile", createProfilePanel());
        addPage("Notifications", createNotificationPanel());   // method of BaseDashboard

        enableNotifications();            // starts the background notification thread
        showPage("Home");
    }

    /** Implementation of the abstract method of BaseDashboard. */
    @Override
    protected void onPageShown(String pageName) {
        if (pageName.equals("Home")) {
            loadHome();
        } else if (pageName.equals("Browse Jobs")) {
            loadBrowseJobs();
        } else if (pageName.equals("My Applications")) {
            loadApplications();
        } else if (pageName.equals("Saved Jobs")) {
            loadSavedJobs();
        } else if (pageName.equals("My Profile")) {
            loadProfileFields();
        } else if (pageName.equals("Notifications")) {
            loadNotifications();
        }
    }

    // ==================================================================
    // HOME
    // ==================================================================

    private JPanel createHomePanel() {
        JPanel page = UIHelper.createPagePanel("Home");

        JPanel cards = new JPanel(new GridLayout(1, 4, 15, 0));
        cards.setOpaque(false);
        cards.add(UIHelper.createStatCard("Total Applications", totalLabel, UIHelper.PRIMARY));
        cards.add(UIHelper.createStatCard("In Progress", progressLabel, UIHelper.WARNING));
        cards.add(UIHelper.createStatCard("Selected", selectedLabel, UIHelper.SUCCESS));
        cards.add(UIHelper.createStatCard("Saved Jobs", savedLabel, new Color(150, 90, 190)));

        JTable recommendedTable = UIHelper.createTable(recommendedModel);
        UIHelper.setColumnWidths(recommendedTable, new int[]{200, 150, 60});
        JPanel recommendedSection = UIHelper.createSection("Recommended for you (best skill match)",
                UIHelper.wrapTable(recommendedTable));
        JPanel chartSection = UIHelper.createSection("Application progress", statusChart);

        JPanel middle = new JPanel(new GridLayout(1, 2, 15, 0));
        middle.setOpaque(false);
        middle.add(recommendedSection);
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
            Map<String, Integer> counts = applicationDao.getStatusCountsForSeeker(seeker.getUserId());
            int total = 0;
            for (int number : counts.values()) {
                total = total + number;
            }
            int inProgress = counts.get(Application.STATUS_SHORTLISTED) + counts.get(Application.STATUS_INTERVIEW);

            totalLabel.setText(String.valueOf(total));
            progressLabel.setText(String.valueOf(inProgress));
            selectedLabel.setText(String.valueOf(counts.get(Application.STATUS_SELECTED)));
            savedLabel.setText(String.valueOf(jobDao.countSavedJobs(seeker.getUserId())));
            statusChart.setData(counts);

            List<Job> bestJobs = pickBestMatches(jobDao.searchJobs(""), 5);
            recommendedModel.setRowCount(0);
            for (Job job : bestJobs) {
                recommendedModel.addRow(new Object[]{
                    job.getTitle(), job.getCompanyName(), seeker.calculateMatch(job) + "%"
                });
            }
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    /** Picks the jobs with the highest skill match, one by one (simple loops, no streams). */
    private List<Job> pickBestMatches(List<Job> jobs, int howMany) {
        List<Job> remaining = new ArrayList<>(jobs);
        List<Job> best = new ArrayList<>();
        while (best.size() < howMany && !remaining.isEmpty()) {
            Job top = remaining.get(0);
            for (Job job : remaining) {
                if (seeker.calculateMatch(job) > seeker.calculateMatch(top)) {
                    top = job;
                }
            }
            best.add(top);
            remaining.remove(top);
        }
        return best;
    }

    // ==================================================================
    // BROWSE JOBS
    // ==================================================================

    private JPanel createBrowsePanel() {
        JPanel page = UIHelper.createPagePanel("Browse Jobs");

        // Combo box options: "All" + the job types (array copy)
        String[] typeOptions = new String[Job.JOB_TYPES.length + 1];
        typeOptions[0] = "All";
        for (int i = 0; i < Job.JOB_TYPES.length; i++) {
            typeOptions[i + 1] = Job.JOB_TYPES[i];
        }
        typeCombo = new JComboBox<>(typeOptions);

        // Deserialization: fill the filters with the last search of this user
        UserPreferences prefs = FileHandler.loadPreferences(seeker.getUserId());
        keywordField.setText(prefs.getLastKeyword());
        locationField.setText(prefs.getLastLocation());
        typeCombo.setSelectedItem(prefs.getLastJobType());

        JButton searchButton = UIHelper.createButton("Search");
        searchButton.addActionListener(e -> searchClicked());
        JButton resetButton = UIHelper.createButton("Reset", UIHelper.GRAY);
        resetButton.addActionListener(e -> resetFilters());

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filterBar.setBackground(Color.WHITE);
        filterBar.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER));
        filterBar.add(new JLabel("Keyword / Skill / Company:"));
        filterBar.add(keywordField);
        filterBar.add(new JLabel("Location:"));
        filterBar.add(locationField);
        filterBar.add(new JLabel("Type:"));
        filterBar.add(typeCombo);
        filterBar.add(searchButton);
        filterBar.add(resetButton);

        UIHelper.setColumnWidths(browseTable, JOB_TABLE_WIDTHS);

        JButton detailsButton = UIHelper.createButton("View Details");
        detailsButton.addActionListener(e -> {
            Job job = getSelectedJob(browseTable, browseJobs);
            if (job != null) {
                showJobDetails(job);
            }
        });
        JButton applyButton = UIHelper.createButton("Apply Now", UIHelper.SUCCESS);
        applyButton.addActionListener(e -> {
            Job job = getSelectedJob(browseTable, browseJobs);
            if (job != null) {
                applyToJob(job);
            }
        });
        JButton saveButton = UIHelper.createButton("Save Job", UIHelper.WARNING);
        saveButton.addActionListener(e -> {
            Job job = getSelectedJob(browseTable, browseJobs);
            if (job != null) {
                saveJob(job);
            }
        });

        JPanel buttons = UIHelper.createButtonBar();
        buttons.add(detailsButton);
        buttons.add(applyButton);
        buttons.add(saveButton);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(filterBar, BorderLayout.NORTH);
        body.add(UIHelper.wrapTable(browseTable), BorderLayout.CENTER);
        body.add(buttons, BorderLayout.SOUTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void loadBrowseJobs() {
        try {
            String keyword = TextUtil.clean(keywordField.getText());
            String location = TextUtil.clean(locationField.getText());
            String type = (String) typeCombo.getSelectedItem();
            browseJobs = jobDao.searchJobs(keyword, location, type);
            fillJobTable(browseModel, browseJobs);
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    private void searchClicked() {
        // Serialization: remember this search for the next time
        UserPreferences prefs = new UserPreferences();
        prefs.setLastKeyword(TextUtil.clean(keywordField.getText()));
        prefs.setLastLocation(TextUtil.clean(locationField.getText()));
        prefs.setLastJobType((String) typeCombo.getSelectedItem());
        try {
            FileHandler.savePreferences(prefs, seeker.getUserId());
        } catch (IOException e) {
            System.out.println("Could not save search preferences: " + e.getMessage());
        }
        loadBrowseJobs();
    }

    private void resetFilters() {
        keywordField.setText("");
        locationField.setText("");
        typeCombo.setSelectedIndex(0);
        loadBrowseJobs();
    }

    // ==================================================================
    // MY APPLICATIONS
    // ==================================================================

    private JPanel createApplicationsPanel() {
        JPanel page = UIHelper.createPagePanel("My Applications");

        UIHelper.setColumnWidths(applicationTable, new int[]{60, 220, 160, 110, 100, 110});
        UIHelper.applyStatusRenderer(applicationTable, 5);

        JButton refreshButton = UIHelper.createButton("Refresh");
        refreshButton.addActionListener(e -> loadApplications());
        JLabel legend = new JLabel("Status flow:  APPLIED  >  SHORTLISTED  >  INTERVIEW  >  SELECTED  (or REJECTED)");
        legend.setFont(UIHelper.FONT_SMALL);
        legend.setForeground(UIHelper.TEXT_MUTED);

        JPanel bottom = UIHelper.createButtonBar();
        bottom.add(refreshButton);
        bottom.add(legend);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(UIHelper.wrapTable(applicationTable), BorderLayout.CENTER);
        body.add(bottom, BorderLayout.SOUTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void loadApplications() {
        try {
            List<Application> applications = applicationDao.getApplicationsBySeeker(seeker.getUserId());
            applicationModel.setRowCount(0);
            for (Application app : applications) {
                applicationModel.addRow(new Object[]{
                    app.getApplicationId(), app.getJob().getTitle(), app.getJob().getCompanyName(),
                    app.getJob().getLocation(), app.getAppliedDate(), app.getStatus()
                });
            }
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    // ==================================================================
    // SAVED JOBS
    // ==================================================================

    private JPanel createSavedPanel() {
        JPanel page = UIHelper.createPagePanel("Saved Jobs");

        UIHelper.setColumnWidths(savedTable, JOB_TABLE_WIDTHS);

        JButton detailsButton = UIHelper.createButton("View Details");
        detailsButton.addActionListener(e -> {
            Job job = getSelectedJob(savedTable, savedJobs);
            if (job != null) {
                showJobDetails(job);
            }
        });
        JButton applyButton = UIHelper.createButton("Apply Now", UIHelper.SUCCESS);
        applyButton.addActionListener(e -> {
            Job job = getSelectedJob(savedTable, savedJobs);
            if (job != null) {
                applyToJob(job);
            }
        });
        JButton removeButton = UIHelper.createButton("Remove", UIHelper.DANGER);
        removeButton.addActionListener(e -> removeSavedJob());

        JPanel buttons = UIHelper.createButtonBar();
        buttons.add(detailsButton);
        buttons.add(applyButton);
        buttons.add(removeButton);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(UIHelper.wrapTable(savedTable), BorderLayout.CENTER);
        body.add(buttons, BorderLayout.SOUTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void loadSavedJobs() {
        try {
            savedJobs = jobDao.getSavedJobs(seeker.getUserId());
            fillJobTable(savedModel, savedJobs);
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    private void removeSavedJob() {
        Job job = getSelectedJob(savedTable, savedJobs);
        if (job == null) {
            return;
        }
        try {
            jobDao.removeSavedJob(seeker.getUserId(), job.getJobId());
            loadSavedJobs();
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    // ==================================================================
    // MY PROFILE
    // ==================================================================

    private JPanel createProfilePanel() {
        JPanel page = UIHelper.createPagePanel("My Profile");

        emailField.setEditable(false);          // email cannot be changed
        resumeField.setEditable(false);         // resume is chosen with the Browse button

        JButton browseButton = UIHelper.createButton("Browse...", UIHelper.GRAY);
        browseButton.addActionListener(e -> chooseResume());
        JPanel resumeRow = new JPanel(new BorderLayout(6, 0));
        resumeRow.setOpaque(false);
        resumeRow.add(resumeField, BorderLayout.CENTER);
        resumeRow.add(browseButton, BorderLayout.EAST);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        UIHelper.addFormRow(form, 0, "Full Name", nameField);
        UIHelper.addFormRow(form, 1, "Email", emailField);
        UIHelper.addFormRow(form, 2, "Phone", phoneField);
        UIHelper.addFormRow(form, 3, "Skills (comma separated)", skillsField);
        UIHelper.addFormRow(form, 4, "Education", educationField);
        UIHelper.addFormRow(form, 5, "Experience (years)", experienceField);
        UIHelper.addFormRow(form, 6, "City", cityField);
        UIHelper.addFormRow(form, 7, "Resume", resumeRow);

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
        nameField.setText(seeker.getFullName());
        emailField.setText(seeker.getEmail());
        phoneField.setText(seeker.getPhone());
        skillsField.setText(seeker.getSkills());
        educationField.setText(seeker.getEducation());
        experienceField.setText(String.valueOf(seeker.getExperienceYears()));
        cityField.setText(seeker.getCity());
        resumeField.setText(seeker.getResumePath());
    }

    private void chooseResume() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Resume files (pdf, doc, docx, txt)",
                "pdf", "doc", "docx", "txt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String newPath = FileHandler.copyResume(chooser.getSelectedFile(), seeker.getUserId());
                resumeField.setText(newPath);
                UIHelper.showInfo(this, "Resume copied to:\n" + newPath
                        + "\n\nClick 'Save Profile' to store it in your profile.");
            } catch (IOException e) {
                UIHelper.showError(this, "Could not copy the resume file:\n" + e.getMessage());
            }
        }
    }

    private void saveProfile() {
        try {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String skills = skillsField.getText().trim();
            String education = educationField.getText().trim();
            String city = cityField.getText().trim();

            Validator.checkNotEmpty(name, "Full name");
            Validator.checkMaxLength(name, 100, "Full name");
            Validator.checkPhone(phone);
            Validator.checkNotEmpty(skills, "Skills");
            Validator.checkMaxLength(skills, 300, "Skills");
            Validator.checkMaxLength(education, 150, "Education");
            Validator.checkMaxLength(city, 60, "City");
            int years = Validator.parseNonNegativeInt(experienceField.getText(), "Experience");

            seeker.setFullName(name);
            seeker.setPhone(phone);
            seeker.setSkills(skills);
            seeker.setEducation(education);
            seeker.setExperienceYears(years);
            seeker.setCity(city);
            seeker.setResumePath(resumeField.getText().trim());

            userDao.updateJobSeeker(seeker);
            FileHandler.writeLog(seeker, "PROFILE UPDATED");
            UIHelper.showInfo(this, "Profile saved successfully.");

        } catch (ValidationException e) {
            UIHelper.showWarning(this, e.getMessage());
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    // ==================================================================
    // SHARED ACTIONS (used by Browse Jobs and Saved Jobs)
    // ==================================================================

    /** Fills a job table (both Browse and Saved use the same columns). */
    private void fillJobTable(DefaultTableModel model, List<Job> jobs) {
        model.setRowCount(0);
        for (Job job : jobs) {
            model.addRow(new Object[]{
                job.getJobId(), job.getTitle(), job.getCompanyName(), job.getLocation(),
                job.getJobType(), job.getSalaryLpa(), job.getExperienceRequired(),
                seeker.calculateMatch(job) + "%", job.getPostedDate()
            });
        }
    }

    /** Returns the job of the selected table row, or null (with a message) if no row is selected. */
    private Job getSelectedJob(JTable table, List<Job> jobs) {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIHelper.showInfo(this, "Please select a job from the table first.");
            return null;
        }
        return jobs.get(row);
    }

    private void showJobDetails(Job job) {
        StringBuilder text = new StringBuilder();
        text.append(job.getTitle()).append("\n");
        text.append("--------------------------------------------\n");
        text.append("Company      : ").append(job.getCompanyName()).append("\n");
        text.append("Location     : ").append(job.getLocation()).append("\n");
        text.append("Job Type     : ").append(job.getJobType()).append("\n");
        text.append("Salary       : ").append(job.getSalaryLpa()).append(" LPA\n");
        text.append("Experience   : ").append(job.getExperienceRequired()).append(" year(s)\n");
        text.append("Skills       : ").append(job.getRequiredSkills()).append("\n");
        text.append("Posted On    : ").append(job.getPostedDate()).append("\n");
        text.append("Status       : ").append(job.getStatus()).append("\n");
        text.append("Your match   : ").append(seeker.calculateMatch(job)).append("%\n\n");
        text.append(job.getDescription());

        JTextArea area = new JTextArea(text.toString(), 15, 42);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(UIHelper.FONT_NORMAL);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Job Details", JOptionPane.PLAIN_MESSAGE);
    }

    private void applyToJob(Job job) {
        if (!job.isOpen()) {
            UIHelper.showWarning(this, "This job is closed. You cannot apply for it.");
            return;
        }
        if (!UIHelper.confirm(this, "Apply for \"" + job.getTitle() + "\" at " + job.getCompanyName() + "?")) {
            return;
        }
        try {
            applicationDao.applyForJob(job.getJobId(), seeker.getUserId());
            FileHandler.writeLog(seeker, "APPLIED for job #" + job.getJobId() + " " + job.getTitle());
            // Tell the recruiter using a background thread (does not block the screen)
            NotificationService.sendAsync(job.getRecruiterId(),
                    seeker.getFullName() + " applied for " + job.getTitle());
            UIHelper.showInfo(this, "Application submitted successfully!\nYou can track it in 'My Applications'.");
        } catch (DuplicateRecordException e) {
            UIHelper.showWarning(this, e.getMessage());        // already applied
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    private void saveJob(Job job) {
        try {
            jobDao.saveJob(seeker.getUserId(), job.getJobId());
            UIHelper.showInfo(this, "Job saved. Find it under 'Saved Jobs'.");
        } catch (DuplicateRecordException e) {
            UIHelper.showWarning(this, e.getMessage());        // already saved
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }
}
