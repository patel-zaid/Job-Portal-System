package jobportal.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import jobportal.MainApp;
import jobportal.dao.UserDao;
import jobportal.exception.JobPortalException;
import jobportal.exception.ValidationException;
import jobportal.model.Admin;
import jobportal.model.JobSeeker;
import jobportal.model.Recruiter;
import jobportal.model.User;
import jobportal.util.FileHandler;
import jobportal.util.Validator;

/**
 * Login screen. After a successful login it opens the dashboard that belongs to the user's role.
 */
public class LoginFrame extends JFrame {

    private final JTextField emailField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final UserDao userDao = new UserDao();

    public LoginFrame() {
        setTitle("Job Portal System - Login");
        setSize(900, 540);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2));            // left half + right half

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                MainApp.exitApplication();
            }
        });

        add(buildLeftPanel());
        add(buildRightPanel());
    }

    /** Blue side with the project name. */
    private JPanel buildLeftPanel() {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(UIHelper.PRIMARY);
        left.setBorder(BorderFactory.createEmptyBorder(120, 45, 40, 30));

        JLabel name = new JLabel("JOB PORTAL");
        name.setFont(new Font("SansSerif", Font.BOLD, 38));
        name.setForeground(Color.WHITE);

        JLabel tagline = new JLabel("Find jobs. Hire talent.");
        tagline.setFont(new Font("SansSerif", Font.PLAIN, 18));
        tagline.setForeground(new Color(190, 210, 245));

        left.add(name);
        left.add(Box.createVerticalStrut(8));
        left.add(tagline);
        left.add(Box.createVerticalStrut(50));
        left.add(createPoint("Job seekers: search, apply and track applications"));
        left.add(Box.createVerticalStrut(12));
        left.add(createPoint("Recruiters: post jobs and manage applicants"));
        left.add(Box.createVerticalStrut(12));
        left.add(createPoint("Admin: manage users, jobs and statistics"));
        left.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("OOPJ Mini Project  |  Java Swing + SQLite");
        footer.setFont(UIHelper.FONT_SMALL);
        footer.setForeground(new Color(160, 185, 230));
        left.add(footer);
        return left;
    }

    private JLabel createPoint(String text) {
        JLabel label = new JLabel("\u2022  " + text);
        label.setFont(UIHelper.FONT_NORMAL);
        label.setForeground(Color.WHITE);
        return label;
    }

    /** White side with the login form. */
    private JPanel buildRightPanel() {
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);

        JLabel heading = new JLabel("Sign In");
        heading.setFont(new Font("SansSerif", Font.BOLD, 28));
        heading.setForeground(UIHelper.PRIMARY);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);
        right.add(heading, gbc);

        JLabel sub = new JLabel("Login with your registered email");
        sub.setFont(UIHelper.FONT_NORMAL);
        sub.setForeground(UIHelper.TEXT_MUTED);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 22, 0);
        right.add(sub, gbc);

        gbc.insets = new Insets(5, 0, 2, 0);
        gbc.gridy = 2;
        right.add(createFieldLabel("Email"), gbc);

        emailField.setFont(UIHelper.FONT_NORMAL);
        emailField.setMargin(new Insets(6, 8, 6, 8));
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 10, 0);
        right.add(emailField, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(5, 0, 2, 0);
        right.add(createFieldLabel("Password"), gbc);

        passwordField.setFont(UIHelper.FONT_NORMAL);
        passwordField.setMargin(new Insets(6, 8, 6, 8));
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 18, 0);
        right.add(passwordField, gbc);

        JButton loginButton = UIHelper.createButton("LOGIN");
        loginButton.addActionListener(e -> handleLogin());
        getRootPane().setDefaultButton(loginButton);        // pressing Enter clicks Login
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 8, 0);
        right.add(loginButton, gbc);

        JButton registerButton = UIHelper.createButton("Create New Account", Color.WHITE);
        registerButton.setForeground(UIHelper.PRIMARY);
        registerButton.setBorderPainted(true);
        registerButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.PRIMARY),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        registerButton.addActionListener(e -> openRegisterDialog());
        gbc.gridy = 7;
        right.add(registerButton, gbc);

        JLabel hint = new JLabel("<html><div style='text-align:center'>Demo logins:<br>"
                + "admin@jobportal.com / admin123<br>"
                + "hr@technova.com / recruit123<br>"
                + "rahul@student.com / seeker123</div></html>");
        hint.setFont(UIHelper.FONT_SMALL);
        hint.setForeground(UIHelper.TEXT_MUTED);
        hint.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridy = 8;
        gbc.insets = new Insets(25, 0, 0, 0);
        right.add(hint, gbc);

        right.setBorder(BorderFactory.createEmptyBorder(0, 60, 0, 60));
        return right;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIHelper.FONT_BOLD);
        label.setForeground(UIHelper.TEXT_DARK);
        return label;
    }

    // ------------------------------------------------------------------
    // Button actions
    // ------------------------------------------------------------------

    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        try {
            Validator.checkNotEmpty(email, "Email");
            Validator.checkNotEmpty(password, "Password");

            User user = userDao.login(email, password);       // checks the database
            FileHandler.writeLog(user, "LOGIN");
            openDashboard(user);

        } catch (ValidationException e) {
            UIHelper.showWarning(this, e.getMessage());
        } catch (JobPortalException e) {
            UIHelper.showError(this, e.getMessage());          // wrong password / blocked user
            passwordField.setText("");
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    /**
     * POLYMORPHISM: the variable type is BaseDashboard (parent),
     * but the object is a JobSeekerDashboard, RecruiterDashboard or AdminDashboard (child).
     */
    private void openDashboard(User user) {
        BaseDashboard dashboard;
        if (user instanceof JobSeeker) {
            dashboard = new JobSeekerDashboard((JobSeeker) user);
        } else if (user instanceof Recruiter) {
            dashboard = new RecruiterDashboard((Recruiter) user);
        } else {
            dashboard = new AdminDashboard((Admin) user);
        }
        dashboard.setVisible(true);
        dispose();                                             // close the login window
    }

    private void openRegisterDialog() {
        RegisterDialog dialog = new RegisterDialog(this);
        dialog.setVisible(true);                               // modal: waits until it is closed
        if (dialog.getRegisteredEmail() != null) {
            emailField.setText(dialog.getRegisteredEmail());   // fill the email for the user
            passwordField.setText("");
            passwordField.requestFocus();
        }
    }
}
