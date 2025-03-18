package it.univr.wbsmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The ServingWebContentApplication class serves as the entry point for the Time Tracking application.
 *
 * <p>This class is responsible for initializing the database and launching the login screen.
 * It ensures that the database structure is set up and then starts the user interface on the
 * Event Dispatch Thread (EDT) for thread safety.</p>
 */
@SpringBootApplication
public class ServingWebContentApplication {
    /**
     * The main method that starts the application.
     *
     * @param args command-line arguments (not used).
     */
    public static void main(String[] args) {

        SpringApplication.run(ServingWebContentApplication.class, args);

        /*

        // Initialize the database by creating tables if they do not already exist.
        DatabaseManager.setupDatabase();

        // Launch the login screen on the Event Dispatch Thread to ensure proper thread safety for GUI operations.
        javax.swing.SwingUtilities.invokeLater(() -> {
            // Create an instance of the LoginView which represents the login screen.
            LoginView loginView = new LoginView();

            // Create an instance of the LoginController and associate it with the LoginView.
            // The controller will handle user interactions and coordinate the login process.
            LoginController loginController = new LoginController(loginView);
        });

        */
    }
}
