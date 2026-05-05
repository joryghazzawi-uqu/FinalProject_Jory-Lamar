package taskmanager.app;

import taskmanager.api.TaskManager;
import taskmanager.model.Task;
import taskmanager.ui.swing.SmartTaskManagerFrame;

import java.time.LocalDateTime;

/**
 * Application entry point for the Smart Task Manager.
 * <p>
 * Initializes the application components, creates sample tasks, and launches
 * the Swing user interface.
 * </p>
 */
public class MainApp {

    /**
     * Starts the Smart Task Manager application.
     * <p>
     * Preconditions: the environment must support Swing and the configured
     * weather API key may be absent.
     * </p>
     * <p>
     * Postconditions: the application window is displayed and sample tasks are
     * persisted to storage.
     * </p>
     *
     * @param args command-line arguments, not used in this application
     */
    public static void main(String[] args) {
        String apiKey = "bcd27f96c4d0c0e34e6876798a6db7fa";

        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Warning: OPENWEATHER_API_KEY not configured.");
        }

        TaskManager tm = TaskManager.builder()
                .withWeatherApiKey(apiKey)
                .withStoragePath("tasks.json")
                .build();

        Task task1 = new Task(
                "task-001",
                "Morning run",
                LocalDateTime.now().plusHours(2),
                true
        );
        task1.setDescription("Outdoor run if weather allows.");

        Task task2 = new Task(
                "task-002",
                "Coding session",
                LocalDateTime.now().plusHours(4),
                false
        );
        task2.setDescription("Complete the Smart Task Manager design.");

        tm.addTask(task1);
        tm.addTask(task2);

        SmartTaskManagerFrame frame = new SmartTaskManagerFrame(tm);
        javax.swing.SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }
}
  