package taskmanager.ui.swing;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import taskmanager.api.SchedulePlanner;
import taskmanager.api.TaskManager;
import taskmanager.api.TaskService;
import taskmanager.impl.DefaultTaskManager;
import taskmanager.model.ScheduleRecommendation;
import taskmanager.model.Task;
import taskmanager.model.WeatherForecast;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple Swing GUI for the Smart Task Manager.
 *
 * This frame allows the user to add, delete, view tasks,
 * check weather for a selected task, and get schedule suggestions.
 *
 * Background work is handled using Reactor, and Swing UI updates
 * are done using SwingUtilities.invokeLater.
 */
public class SmartTaskManagerFrame extends JFrame {

        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        private final TaskManager taskManager;
        private final TaskService taskService;
        private final SchedulePlanner schedulePlanner;

        private final JTable taskTable;
        private final DefaultTableModel tableModel;
        private final JTextField locationField;
        private final JLabel statusLabel;

        private final JButton deleteButton;
        private final JButton weatherButton;

        private final String[] columnNames = {
                        "ID", "Title", "Due Time", "Weather Sensitive", "Status"
        };

        /**
         * Creates the Smart Task Manager Swing window.
         *
         * @param taskManager main API object used by the UI
         */
        public SmartTaskManagerFrame(TaskManager taskManager) {
                this.taskManager = taskManager;
                this.taskService = ((DefaultTaskManager) taskManager).getTaskService();
                this.schedulePlanner = taskManager.getPlanner();

                setTitle("Smart Task Manager");
                setDefaultCloseOperation(EXIT_ON_CLOSE);
                setSize(800, 500);
                setLocationRelativeTo(null);

                tableModel = new DefaultTableModel(columnNames, 0) {
                        @Override
                        public boolean isCellEditable(int row, int column) {
                                return false;
                        }
                };

                taskTable = new JTable(tableModel);
                JScrollPane scrollPane = new JScrollPane(taskTable);

                locationField = new JTextField("Jeddah", 15);
                statusLabel = new JLabel("Ready");

                JButton addButton = new JButton("Add Task");
                deleteButton = new JButton("Delete Task");
                weatherButton = new JButton("Check Weather");
                JButton suggestButton = new JButton("Get Suggestions");
                JButton refreshButton = new JButton("Refresh");

                deleteButton.setEnabled(false);
                weatherButton.setEnabled(false);

                JPanel topPanel = new JPanel(new BorderLayout(8, 8));
                topPanel.add(new JLabel("Location:"), BorderLayout.WEST);
                topPanel.add(locationField, BorderLayout.CENTER);
                topPanel.add(statusLabel, BorderLayout.EAST);

                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                buttonPanel.add(addButton);
                buttonPanel.add(deleteButton);
                buttonPanel.add(weatherButton);
                buttonPanel.add(suggestButton);
                buttonPanel.add(refreshButton);

                setLayout(new BorderLayout(8, 8));
                add(topPanel, BorderLayout.NORTH);
                add(scrollPane, BorderLayout.CENTER);
                add(buttonPanel, BorderLayout.SOUTH);

                loadTasks();

                taskTable.getSelectionModel().addListSelectionListener(event -> {
                        boolean selected = taskTable.getSelectedRow() >= 0;
                        deleteButton.setEnabled(selected);
                        weatherButton.setEnabled(selected);
                });

                addButton.addActionListener(event -> addTask());
                deleteButton.addActionListener(event -> deleteSelectedTask());
                weatherButton.addActionListener(event -> updateWeatherForSelectedTask());
                suggestButton.addActionListener(event -> suggestSchedule());
                refreshButton.addActionListener(event -> loadTasks());
        }

        /**
         * Loads tasks from {@link TaskManager} and displays them in the table.
         * <p>
         * Preconditions: the TaskManager instance must be initialized.
         * </p>
         * <p>
         * Postconditions: the table is refreshed with the current tasks and the
         * status label reflects the load result.
         * </p>
         * <p>
         * Side effects: performs background task loading and updates the Swing UI.
         * </p>
         */
        private void loadTasks() {
                statusLabel.setText("Loading tasks...");

                Mono.fromCallable(taskManager::getTasks)
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnNext(tasks -> SwingUtilities.invokeLater(() -> populateTable(tasks)))
                                .doOnError(error -> SwingUtilities.invokeLater(() -> showError("Load failed", error)))
                                .subscribe();
        }

        /**
         * Fills the table with task data.
         * <p>
         * Preconditions: {@code tasks} must not be {@code null}.
         * </p>
         * <p>
         * Postconditions: the table contains one row per task and the status label
         * reflects the number of loaded tasks.
         * </p>
         *
         * @param tasks list of tasks to display
         */
        private void populateTable(List<Task> tasks) {
                tableModel.setRowCount(0);

                for (Task task : tasks) {
                        tableModel.addRow(new Object[] {
                                        task.getId(),
                                        task.getTitle(),
                                        DATE_TIME_FORMATTER.format(task.getDueDateTime()),
                                        task.isWeatherSensitive(),
                                        "Not checked"
                        });
                }

                statusLabel.setText("Tasks loaded: " + tasks.size());
        }

        /**
         * Opens a dialog to collect new task details and submits the task for saving.
         * <p>
         * Preconditions: the caller may invoke this method at any time.
         * </p>
         * <p>
         * Postconditions: if the user confirms valid input, a new task is persisted
         * and the task list is refreshed.
         * </p>
         * <p>
         * Side effects: displays modal dialogs and performs background persistence.
         * </p>
         */
        private void addTask() {
                JTextField titleField = new JTextField();
                JTextField descriptionField = new JTextField();
                JTextField dueField = new JTextField(
                                DATE_TIME_FORMATTER.format(LocalDateTime.now().plusHours(1)));
                JCheckBox weatherSensitiveBox = new JCheckBox("Weather Sensitive");

                JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
                form.add(new JLabel("Title:"));
                form.add(titleField);
                form.add(new JLabel("Description:"));
                form.add(descriptionField);
                form.add(new JLabel("Due date/time (yyyy-MM-dd HH:mm):"));
                form.add(dueField);
                form.add(weatherSensitiveBox);

                int result = JOptionPane.showConfirmDialog(
                                this,
                                form,
                                "Add Task",
                                JOptionPane.OK_CANCEL_OPTION);

                if (result != JOptionPane.OK_OPTION) {
                        return;
                }

                try {
                        LocalDateTime dueDate = LocalDateTime.parse(
                                        dueField.getText().trim(),
                                        DATE_TIME_FORMATTER);

                        Task task = new Task(
                                        generateTaskId(),
                                        titleField.getText().trim(),
                                        dueDate,
                                        weatherSensitiveBox.isSelected());

                        task.setDescription(descriptionField.getText().trim());

                        taskService.addTask(task)
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .doOnSuccess(v -> SwingUtilities.invokeLater(() -> {
                                                statusLabel.setText("Task added.");
                                                loadTasks();
                                        }))
                                        .doOnError(error -> SwingUtilities
                                                        .invokeLater(() -> showError("Add task failed", error)))
                                        .subscribe();

                } catch (DateTimeParseException error) {
                        showError("Invalid date format", error);
                }
        }

        /**
         * Deletes the selected task from the table and storage.
         * <p>
         * Preconditions: a task row must be selected.
         * </p>
         * <p>
         * Postconditions: the selected task is removed from persistent storage
         * and the task list is refreshed.
         * </p>
         * <p>
         * Side effects: performs background deletion and updates the UI status.
         * </p>
         */
        private void deleteSelectedTask() {
                int selectedRow = taskTable.getSelectedRow();

                if (selectedRow < 0) {
                        return;
                }

                String taskId = (String) tableModel.getValueAt(selectedRow, 0);

                taskService.removeTask(taskId)
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnSuccess(v -> SwingUtilities.invokeLater(() -> {
                                        statusLabel.setText("Task deleted.");
                                        loadTasks();
                                }))
                                .doOnError(error -> SwingUtilities.invokeLater(() -> showError("Delete failed", error)))
                                .subscribe();
        }

        /**
         * Checks weather for the selected task and updates the Status column.
         * <p>
         * Preconditions: a task row must be selected and a location must be entered.
         * </p>
         * <p>
         * Postconditions: the task status column is updated based on the fetched
         * forecast.
         * </p>
         * <p>
         * Side effects: performs an asynchronous weather lookup.
         * </p>
         */
        private void updateWeatherForSelectedTask() {
         int selectedRow = taskTable.getSelectedRow();

          if (selectedRow < 0) {
           return;
                }

          String taskId = (String) tableModel.getValueAt(selectedRow, 0);
          String location = locationField.getText().trim();

        statusLabel.setText("Checking weather...");

         taskManager.fetchWeather(location)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(forecast -> taskService.findTaskById(taskId)
                 .flatMap(task -> schedulePlanner.suggestSchedule(List.of(task),
                   forecast))
                 .map(recs -> new Object[] { forecast, recs.get(0).recommendation() }))
                .doOnNext(result -> SwingUtilities.invokeLater(() -> {
                 WeatherForecast forecast = (WeatherForecast) result[0];
                 String recommendation = (String) result[1];
                 updateTaskStatusInTable(taskId, recommendation);
                statusLabel.setText(String.format(
                 "Weather: %s, %.1f°C",
                 forecast.getCondition(),
                forecast.getTemperatureCelsius()));
                                }))
                                .doOnError(error -> SwingUtilities
                                                .invokeLater(() -> showError("Weather check failed", error)))
                                .subscribe();
        }

        /**
         * Generates schedule suggestions for all tasks.
         * <p>
         * Preconditions: the current location text is used to fetch weather data.
         * </p>
         * <p>
         * Postconditions: a recommendations dialog is displayed if tasks are present.
         * </p>
         * <p>
         * Side effects: performs background recommendation generation and updates the
         * UI.
         * </p>
         */
        private void suggestSchedule() {
                String location = locationField.getText().trim();
                statusLabel.setText("Generating suggestions...");

                Mono.fromCallable(taskManager::getTasks)
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(tasks -> schedulePlanner.suggestScheduleForLocation(tasks, location))
                                .doOnNext(recommendations -> SwingUtilities
                                                .invokeLater(() -> showRecommendations(recommendations)))
                                .doOnError(error -> SwingUtilities
                                                .invokeLater(() -> showError("Suggestion failed", error)))
                                .subscribe();
        }

        /**
         * Displays schedule recommendations in a popup.
         * <p>
         * Preconditions: {@code recommendations} must not be {@code null}.
         * </p>
         * <p>
         * Postconditions: a modal dialog is shown containing the recommendation text.
         * </p>
         *
         * @param recommendations recommendations to display
         */
        private void showRecommendations(List<ScheduleRecommendation> recommendations) {
                if (recommendations.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                        this,
                                        "No tasks available.",
                                        "Schedule Suggestions",
                                        JOptionPane.INFORMATION_MESSAGE);
                        return;
                }

                String message = recommendations.stream()
                                .map(rec -> rec.task().getTitle() + ": " + rec.recommendation())
                                .collect(Collectors.joining("\n"));

                JTextArea area = new JTextArea(message);
                area.setEditable(false);
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                area.setOpaque(false);
                area.setBorder(null);

                JScrollPane scrollPane = new JScrollPane(area);
                scrollPane.setPreferredSize(new Dimension(550, 300));
                scrollPane.setBorder(null);
                scrollPane.getViewport().setOpaque(false);

                JOptionPane.showMessageDialog(
                                this,
                                scrollPane,
                                "Schedule Suggestions",
                                JOptionPane.INFORMATION_MESSAGE);

                statusLabel.setText("Suggestions ready.");
        }

        /**
         * Updates the Status column for a specific task.
         * <p>
         * Preconditions: {@code taskId} must match a task row currently displayed.
         * </p>
         * <p>
         * Postconditions: the status text for the matching task row is updated.
         * </p>
         *
         * @param taskId task ID
         * @param status status text
         */
        private void updateTaskStatusInTable(String taskId, String status) {
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                        String idInTable = (String) tableModel.getValueAt(i, 0);

                        if (idInTable.equals(taskId)) {
                                tableModel.setValueAt(status, i, 4);
                                break;
                        }
                }
        }

        /**
         * Generates a simple unique task ID.
         * <p>
         * Postconditions: returns a string that is likely unique based on the current
         * timestamp.
         * </p>
         *
         * @return generated task ID
         */
        private String generateTaskId() {
                return "task-" + System.currentTimeMillis();
        }

        /**
         * Shows an error message in the UI.
         * <p>
         * Preconditions: {@code title} must describe the error context.
         * </p>
         * <p>
         * Postconditions: the status label is updated and an error dialog is displayed.
         * </p>
         *
         * @param title error title
         * @param error exception to display
         */
        private void showError(String title, Throwable error) {
                statusLabel.setText(title + ": " + error.getMessage());

                JOptionPane.showMessageDialog(
                                this,
                                error.getMessage(),
                                title,
                                JOptionPane.ERROR_MESSAGE);
        }

}
