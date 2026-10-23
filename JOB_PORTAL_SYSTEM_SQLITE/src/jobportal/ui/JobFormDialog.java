package jobportal.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import jobportal.exception.ValidationException;
import jobportal.model.Job;
import jobportal.util.TextUtil;
import jobportal.util.Validator;

/**
 * One form used for BOTH "Post New Job" and "Edit Job".
 * The dialog only collects and validates the data. The recruiter dashboard saves it in the database.
 */
public class JobFormDialog extends JDialog {

    private final JTextField titleField = new JTextField(28);
    private final JTextField locationField = new JTextField(28);
    private final JComboBox<String> typeCombo = new JComboBox<>(Job.JOB_TYPES);
    private final JTextField salaryField = new JTextField(28);
    private final JTextField experienceField = new JTextField(28);
    private final JTextField skillsField = new JTextField(28);
    private final JTextArea descriptionArea = new JTextArea(6, 28);

    private final int recruiterId;
    private final Job existingJob;        // null when posting a new job
    private Job savedJob = null;          // stays null if the user cancels

    public JobFormDialog(JFrame parent, int recruiterId, Job existingJob) {
        super(parent, existingJob == null ? "Post New Job" : "Edit Job", true);
        this.recruiterId = recruiterId;
        this.existingJob = existingJob;

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(UIHelper.FONT_NORMAL);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(15, 20, 5, 20));
        UIHelper.addFormRow(form, 0, "Job Title", titleField);
        UIHelper.addFormRow(form, 1, "Location", locationField);
        UIHelper.addFormRow(form, 2, "Job Type", typeCombo);
        UIHelper.addFormRow(form, 3, "Salary (LPA)", salaryField);
        UIHelper.addFormRow(form, 4, "Experience (years)", experienceField);
        UIHelper.addFormRow(form, 5, "Required Skills", skillsField);
        UIHelper.addFormRow(form, 6, "Description", new JScrollPane(descriptionArea));

        skillsField.setToolTipText("Separate skills with commas, example: java, sql, git");

        if (existingJob != null) {
            titleField.setText(existingJob.getTitle());
            locationField.setText(existingJob.getLocation());
            typeCombo.setSelectedItem(existingJob.getJobType());
            salaryField.setText(String.valueOf(existingJob.getSalaryLpa()));
            experienceField.setText(String.valueOf(existingJob.getExperienceRequired()));
            skillsField.setText(existingJob.getRequiredSkills());
            descriptionArea.setText(existingJob.getDescription());
        } else {
            salaryField.setText("0");
            experienceField.setText("0");
        }

        JButton saveButton = UIHelper.createButton("Save Job", UIHelper.SUCCESS);
        saveButton.addActionListener(e -> save());
        JButton cancelButton = UIHelper.createButton("Cancel", UIHelper.GRAY);
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 15));
        buttons.add(cancelButton);
        buttons.add(saveButton);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    /** The job entered by the user, or null if the dialog was cancelled. */
    public Job getSavedJob() {
        return savedJob;
    }

    private void save() {
        try {
            String title = titleField.getText().trim();
            String location = locationField.getText().trim();
            String skills = skillsField.getText().trim();
            String description = descriptionArea.getText().trim();

            Validator.checkNotEmpty(title, "Job title");
            Validator.checkMaxLength(title, 100, "Job title");
            Validator.checkNotEmpty(location, "Location");
            Validator.checkMaxLength(location, 80, "Location");
            Validator.checkNotEmpty(skills, "Required skills");
            Validator.checkMaxLength(skills, 300, "Required skills");
            Validator.checkNotEmpty(description, "Description");
            Validator.checkMaxLength(description, 1000, "Description");

            double salary = Validator.parseNonNegativeDouble(salaryField.getText(), "Salary");
            if (salary > 999) {
                throw new ValidationException("Salary (in lakhs per annum) must be less than 1000.");
            }
            int experience = Validator.parseNonNegativeInt(experienceField.getText(), "Experience");
            String jobType = (String) typeCombo.getSelectedItem();

            Job job = new Job(recruiterId, title, description, location, jobType,
                    salary, TextUtil.clean(skills), experience);
            if (existingJob != null) {
                job.setJobId(existingJob.getJobId());     // editing: keep the same id
            }
            savedJob = job;
            dispose();

        } catch (ValidationException e) {
            UIHelper.showWarning(this, e.getMessage());
        }
    }
}
