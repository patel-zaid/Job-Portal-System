package jobportal.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import jobportal.MainApp;
import jobportal.dao.NotificationDao;
import jobportal.model.User;
import jobportal.service.NotificationService;
import jobportal.util.FileHandler;

/**
 * ABSTRACT parent of the three dashboards:
 *
 *        BaseDashboard (abstract)
 *          |-- JobSeekerDashboard
 *          |-- RecruiterDashboard
 *          '-- AdminDashboard
 *
 * It builds everything that is common: header bar, sidebar menu, page switching (CardLayout),
 * logout, and the notification page. Each child adds its own pages with addPage(...)
 * and must implement the abstract method onPageShown(...).
 */
public abstract class BaseDashboard extends JFrame {

    protected final User user;
    protected final JLabel notificationLabel = new JLabel("Notifications: 0 new");

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JPanel menuPanel = new JPanel();
    private final Map<String, JButton> menuButtons = new HashMap<>();   // menu name -> button

    // Widgets of the notification page (used by both seeker and recruiter)
    private JList<String> notificationList;
    private JLabel threadInfoLabel;

    public BaseDashboard(User user) {
        this.user = user;

        setTitle("Job Portal System - " + user.getDashboardTitle());
        setSize(1200, 720);
        setMinimumSize(new Dimension(1050, 640));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // When the window is closed we stop threads and exit properly
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                MainApp.exitApplication();
            }
        });

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        contentPanel.setBackground(UIHelper.BACKGROUND);
        add(contentPanel, BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------
    // Abstract method: every dashboard decides what to load when a page is opened
    // ------------------------------------------------------------------
    protected abstract void onPageShown(String pageName);

    // ------------------------------------------------------------------
    // Building the common screen parts
    // ------------------------------------------------------------------

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIHelper.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel title = new JLabel("JOB PORTAL SYSTEM   |   " + user.getDashboardTitle());
        title.setFont(UIHelper.FONT_HEADING);
        title.setForeground(Color.WHITE);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        right.setOpaque(false);
        JLabel welcome = new JLabel(user.getWelcomeMessage());
        welcome.setFont(UIHelper.FONT_NORMAL);
        welcome.setForeground(Color.WHITE);
        JButton logoutButton = UIHelper.createButton("Logout", UIHelper.DANGER);
        logoutButton.addActionListener(e -> logout());
        right.add(welcome);
        right.add(logoutButton);

        header.add(title, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UIHelper.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(215, 0));

        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(UIHelper.SIDEBAR);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        notificationLabel.setFont(UIHelper.FONT_BOLD);
        notificationLabel.setForeground(new Color(255, 214, 120));
        notificationLabel.setBorder(BorderFactory.createEmptyBorder(12, 20, 15, 10));
        notificationLabel.setVisible(false);        // shown only for seeker and recruiter

        sidebar.add(menuPanel, BorderLayout.NORTH);
        sidebar.add(notificationLabel, BorderLayout.SOUTH);
        return sidebar;
    }

    // ------------------------------------------------------------------
    // Methods used by the child classes
    // ------------------------------------------------------------------

    /** Adds one page: creates a menu button in the sidebar and registers the page in the CardLayout. */
    protected void addPage(String menuName, JPanel page) {
        JButton button = new JButton(menuName);
        button.setFont(UIHelper.FONT_NORMAL);
        button.setForeground(Color.WHITE);
        button.setBackground(UIHelper.SIDEBAR);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 10));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setPreferredSize(new Dimension(215, 44));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.addActionListener(e -> showPage(menuName));

        menuPanel.add(button);
        menuButtons.put(menuName, button);
        contentPanel.add(page, menuName);
    }

    /** Shows one page and highlights its menu button. */
    protected void showPage(String menuName) {
        for (Map.Entry<String, JButton> entry : menuButtons.entrySet()) {
            if (entry.getKey().equals(menuName)) {
                entry.getValue().setBackground(UIHelper.SIDEBAR_ACTIVE);
            } else {
                entry.getValue().setBackground(UIHelper.SIDEBAR);
            }
        }
        cardLayout.show(contentPanel, menuName);
        onPageShown(menuName);           // polymorphism: the child's version is called
    }

    /** Called by seeker and recruiter dashboards: starts the background notification thread. */
    protected void enableNotifications() {
        notificationLabel.setVisible(true);
        NotificationService.startPolling(user.getUserId(), notificationLabel);
    }

    // ------------------------------------------------------------------
    // Notification page (shared by JobSeekerDashboard and RecruiterDashboard)
    // ------------------------------------------------------------------

    protected JPanel createNotificationPanel() {
        JPanel page = UIHelper.createPagePanel("Notifications");

        notificationList = new JList<>();
        notificationList.setFont(UIHelper.FONT_NORMAL);
        notificationList.setFixedCellHeight(30);
        JScrollPane scroll = new JScrollPane(notificationList);
        scroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER));

        JButton refreshButton = UIHelper.createButton("Refresh");
        refreshButton.addActionListener(e -> loadNotifications());
        JButton readButton = UIHelper.createButton("Mark All As Read", UIHelper.SUCCESS);
        readButton.addActionListener(e -> markAllRead());

        JPanel bar = UIHelper.createButtonBar();
        bar.add(refreshButton);
        bar.add(readButton);

        threadInfoLabel = new JLabel(" ");
        threadInfoLabel.setFont(UIHelper.FONT_SMALL);
        threadInfoLabel.setForeground(UIHelper.TEXT_MUTED);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(bar, BorderLayout.WEST);
        bottom.add(threadInfoLabel, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(scroll, BorderLayout.CENTER);
        body.add(bottom, BorderLayout.SOUTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    protected void loadNotifications() {
        try {
            Vector<String> messages = new NotificationDao().getNotifications(user.getUserId());
            if (messages.isEmpty()) {
                messages.add("You have no notifications yet.");
            }
            notificationList.setListData(messages);       // JList can show a Vector directly
            threadInfoLabel.setText("Background thread: " + NotificationService.getPollerState()
                    + "   |   Notifications sent in this session: " + NotificationService.getSentCount());
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    private void markAllRead() {
        try {
            new NotificationDao().markAllRead(user.getUserId());
            notificationLabel.setText("Notifications: 0 new");
            loadNotifications();
        } catch (SQLException e) {
            UIHelper.showDbError(this, e);
        }
    }

    // ------------------------------------------------------------------

    private void logout() {
        NotificationService.stopPolling();
        FileHandler.writeLog(user, "LOGOUT");
        dispose();                                  // close this window
        new LoginFrame().setVisible(true);          // go back to the login screen
    }
}
