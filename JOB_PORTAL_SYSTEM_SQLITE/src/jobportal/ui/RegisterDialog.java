package jobportal.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import jobportal.dao.UserDao;
import jobportal.exception.DuplicateRecordException;
import jobportal.exception.ValidationException;
import jobportal.model.Company;
import jobportal.model.JobSeeker;
import jobportal.model.Recruiter;
import jobportal.model.User;
import jobportal.util.FileHandler;
import jobportal.util.TextUtil;
import jobportal.util.Validator;

/**
 * Registration form for new Job Seekers and Recruiters.
 */
public class RegisterDialog extends JDialog {

    private final JComboBox<String> roleCombo = new JComboBox<>(new String[]{"Job Seeker", "Recruiter"});
    private final JTextField nameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JTextField phoneField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JPasswordField confirmField = new JPasswordField(20);
    private final JTextField companyField = new JTextField(20);
    private final JTextField designationField = new JTextField("HR Manager", 20);

    private final UserDao userDao = new UserDao();
    private String registeredEmail = null;      // stays null if registration is cancelled

    public RegisterDialog(JFrame parent) {
        super(parent, "Create New Account", true);      // true = modal dialog

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(15, 20, 5, 20));
        UIHelper.addFormRow(form, 0, "Register as", roleCombo);
        UIHelper.addFormRow(form, 1, "Full Name", nameField);
        UIHelper.addFormRow(form, 2, "Email", emailField);
        UIHelper.addFormRow(form, 3, "Phone (10 digits)", phoneField);
        UIHelper.addFormRow(form, 4, "Password", passwordField);
        UIHelper.addFormRow(form, 5, "Confirm Password", confirmField);
        UIHelper.addFormRow(form, 6, "Company Name", companyField);
        UIHelper.addFormRow(form, 7, "Designation", designationField);

        // Company fields are needed only for recruiters
        companyField.setEnabled(false);
        designationField.setEnabled(false);
        roleCombo.addActionListener(e -> {
            boolean isRecruiter = roleCombo.getSelectedIndex() == 1;
            companyField.setEnabled(isRecruiter);
            designationField.setEnabled(isRecruiter);
        });

        JButton registerButton = UIHelper.createButton("Register", UIHelper.SUCCESS);
        registerButton.addActionListener(e -> register());
        JButton cancelButton = UIHelper.createButton("Cancel", UIHelper.GRAY);
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 15));
        buttons.add(cancelButton);
        buttons.add(registerButton);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    /** Email of the newly registered user, or null if nobody registered. */
    public String getRegisteredEmail() {
        return registeredEmail;
    }

    private void register() {
        try {
            // ---- 1. validation (throws ValidationException when something is wrong) ----
            String name = nameField.getText().trim();
            String email = emailField.getText().trim().toLowerCase();
            String phone = phoneField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirm = new String(confirmField.getPassword());

            Validator.checkNotEmpty(name, "Full name");
            Validator.checkEmail(email);
            Validator.checkPhone(phone);
            Validator.checkPassword(password);
            if (!password.equals(confirm)) {
                throw new ValidationException("Password and Confirm Password do not match.");
            }

            String passwordHash = User.hashPassword(password);     // never store the real password
            boolean isRecruiter = roleCombo.getSelectedIndex() == 1;

            // ---- 2. create the correct object and save it ----
            if (isRecruiter) {
                String companyName = companyField.getText().trim();
                Validator.checkNotEmpty(companyName, "Company name");
                String designation = TextUtil.clean(designationField.getText());
                Recruiter recruiter = new Recruiter(name, email, passwordHash, phone,
                        new Company(companyName), designation);
                userDao.registerRecruiter(recruiter);
                FileHandler.writeLog(recruiter, "REGISTERED");
            } else {
                JobSeeker seeker = new JobSeeker(name, email, passwordHash, phone);
                userDao.registerJobSeeker(seeker);
                FileHandler.writeLog(seeker, "REGISTERED");
            }

            registeredEmail = email;
            UIHelper.showInfo(this, "Registration successful! You can login now.");
            dispose();

        } catch (ValidationException e) {
            UIHelper.showWarning(this, e.getMessage());
        } catch (DuplicateRecordException e) {
            UIHelper.showWarning(this, e.getMessage());        // email already registered
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }
}
