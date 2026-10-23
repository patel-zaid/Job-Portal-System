package jobportal;

import java.io.IOException;
import java.sql.SQLException;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import jobportal.dao.Database;
import jobportal.service.NotificationService;
import jobportal.service.RestApiServer;
import jobportal.ui.LoginFrame;
import jobportal.ui.UIHelper;
import jobportal.util.FileHandler;

/**
 * Starting point of the whole project. Run this class.
 *
 * It (1) sets the look of Swing, (2) creates / opens the SQLite database (data/job_portal.db,
 * created automatically with sample data on the first start), (3) starts the small REST API
 * and (4) shows the login screen.
 */
public class MainApp {

    public static final int REST_PORT = 8080;

    private static RestApiServer restServer;

    public static void main(String[] args) {
        try {
            // Cross-platform ("Metal") look, so colours look the same on every computer
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Could not set look and feel: " + e.getMessage());
        }

        FileHandler.writeLog("APPLICATION STARTED");

        // Create the SQLite file, the tables and the sample data (only the first time).
        // This happens BEFORE the REST API and the screens use the database.
        try {
            Database.testConnection();
        } catch (SQLException e) {
            System.out.println("Cannot open the SQLite database: " + e.getMessage());
            FileHandler.writeLog("DATABASE ERROR: " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                UIHelper.showError(null, "Cannot open the SQLite database.\n\n" + e.getMessage()
                        + "\n\n1. Start the program from the project folder (use compile_and_run.bat)\n"
                        + "2. Check that the folders 'database' and 'lib' (with the sqlite-jdbc jar) exist\n"
                        + "3. Check that the folder 'data' can be written (not read-only)\n"
                        + "4. Close other copies of the program and try again");
                System.exit(1);
            });
            return;
        }

        startRestServer();

        // Swing screens must be created on the Swing thread
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    private static void startRestServer() {
        restServer = new RestApiServer(REST_PORT);
        try {
            restServer.start();
        } catch (IOException e) {
            System.out.println("REST API could not start (port " + REST_PORT + " busy?): " + e.getMessage());
        }
    }

    /** Called when a window is closed: stop threads and the REST server, then exit. */
    public static void exitApplication() {
        NotificationService.shutdown();
        if (restServer != null) {
            restServer.stop();
        }
        FileHandler.writeLog("APPLICATION CLOSED");
        System.exit(0);
    }
}
