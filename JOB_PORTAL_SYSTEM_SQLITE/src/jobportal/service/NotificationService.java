package jobportal.service;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import jobportal.dao.NotificationDao;
import jobportal.util.FileHandler;

/**
 * MULTITHREADING in this project:
 *
 *  1. Poller (extends Thread)        - a background thread that checks for new notifications
 *                                      every 5 seconds and updates the label in the sidebar.
 *  2. SendTask (implements Runnable) - saves a notification to the SQLite database in the background,
 *                                      run by an ExecutorService thread pool (2 threads),
 *                                      so the screen never freezes.
 *  3. synchronized methods           - protect the shared counter "sentCount".
 *  4. interrupt()                    - stops the poller thread gracefully when the user logs out.
 */
public class NotificationService {

    private static final ExecutorService pool = Executors.newFixedThreadPool(2);   // thread pool
    private static Poller poller;                                                  // the background thread
    private static int sentCount = 0;                                              // shared data

    // ------------------------------------------------------------------
    // Thread pool part: send a notification without blocking the screen
    // ------------------------------------------------------------------

    /** Task that is run by one of the pool threads. */
    private static class SendTask implements Runnable {
        private final int userId;
        private final String message;

        SendTask(int userId, String message) {
            this.userId = userId;
            this.message = message;
        }

        @Override
        public void run() {
            try {
                new NotificationDao().addNotification(userId, message);
                increaseSentCount();
                FileHandler.writeLog("NOTIFICATION to user " + userId + " : " + message);
            } catch (SQLException e) {
                System.out.println("Could not save notification: " + e.getMessage());
            }
        }
    }

    /** Called by the screens. Returns immediately; the work is done in the background. */
    public static void sendAsync(int userId, String message) {
        pool.submit(new SendTask(userId, message));
    }

    // synchronized: only one thread at a time can change/read sentCount
    private static synchronized void increaseSentCount() {
        sentCount++;
    }

    public static synchronized int getSentCount() {
        return sentCount;
    }

    // ------------------------------------------------------------------
    // Background thread part: unread notification counter
    // ------------------------------------------------------------------

    /** Thread created by extending the Thread class. */
    private static class Poller extends Thread {
        private final int userId;
        private final JLabel badgeLabel;
        private final NotificationDao dao = new NotificationDao();

        Poller(int userId, JLabel badgeLabel) {
            super("notification-poller");
            this.userId = userId;
            this.badgeLabel = badgeLabel;
            setDaemon(true);      // daemon thread: it does not keep the program alive
        }

        @Override
        public void run() {
            // Loop until somebody calls interrupt() on this thread
            while (!isInterrupted()) {
                updateBadge();
                try {
                    Thread.sleep(5000);          // thread state: TIMED_WAITING
                } catch (InterruptedException e) {
                    // sleep() clears the interrupt flag, so we set it again -> the while loop ends
                    interrupt();
                }
            }
            // run() finished -> thread state: TERMINATED
        }

        private void updateBadge() {
            try {
                int unread = dao.countUnread(userId);
                final String text = "Notifications: " + unread + " new";
                // Swing components must be changed on the Swing (event dispatch) thread
                SwingUtilities.invokeLater(() -> badgeLabel.setText(text));
            } catch (SQLException e) {
                System.out.println("Notification check failed: " + e.getMessage());
            }
        }
    }

    public static void startPolling(int userId, JLabel badgeLabel) {
        stopPolling();                                   // never keep two pollers running
        poller = new Poller(userId, badgeLabel);         // thread state: NEW
        poller.start();                                  // thread state: RUNNABLE
    }

    public static void stopPolling() {
        if (poller != null) {
            poller.interrupt();                          // ask the thread to stop
            poller = null;
        }
    }

    /** Shows the lifecycle state of the poller thread (NEW, RUNNABLE, TIMED_WAITING, TERMINATED). */
    public static String getPollerState() {
        if (poller == null) {
            return "not running";
        }
        return poller.getState().toString();
    }

    /** Called when the application closes. */
    public static void shutdown() {
        stopPolling();
        pool.shutdown();                                 // no new tasks; old tasks may finish
        try {
            pool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
