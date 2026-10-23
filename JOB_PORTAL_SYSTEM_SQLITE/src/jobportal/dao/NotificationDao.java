package jobportal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Vector;

/**
 * DAO for the notifications table.
 * Notifications are returned as a Vector, because Swing's JList can show a Vector directly.
 */
public class NotificationDao {

    // SQLite stores created_at as text 'YYYY-MM-DD HH:MM:SS'; we show it as '21 Oct 2026, 14:35'
    private static final DateTimeFormatter STORED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SHOWN = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    public void addNotification(int userId, String message) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, message, is_read) VALUES (?, ?, 0)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, message);
            ps.executeUpdate();
        }
    }

    /** Latest 50 notifications of a user, newest first. Unread ones start with [NEW]. */
    public Vector<String> getNotifications(int userId) throws SQLException {
        Vector<String> notifications = new Vector<>();
        String sql = "SELECT message, is_read, created_at AS created "
                   + "FROM notifications WHERE user_id = ? ORDER BY notification_id DESC LIMIT 50";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StringBuilder line = new StringBuilder();
                    if (!rs.getBoolean("is_read")) {
                        line.append("[NEW]  ");
                    }
                    line.append(formatTime(rs.getString("created")));
                    line.append("   -   ");
                    line.append(rs.getString("message"));
                    notifications.add(line.toString());
                }
            }
        }
        return notifications;
    }

    public int countUnread(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void markAllRead(int userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /** Converts the stored text '2026-10-21 14:35:07' into '21 Oct 2026, 14:35' (unchanged if it cannot be parsed). */
    private static String formatTime(String stored) {
        if (stored == null) {
            return "";
        }
        try {
            return LocalDateTime.parse(stored, STORED).format(SHOWN);
        } catch (DateTimeParseException e) {
            return stored;
        }
    }
}
