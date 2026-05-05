package taskmanager.api;

import reactor.core.publisher.Mono;
import taskmanager.impl.DefaultTaskManagerBuilder;
import taskmanager.model.Task;
import taskmanager.model.WeatherForecast;

import java.util.List;

/**
 * Main facade interface for the Smart Task Manager system.
 * <p>
 * This interface defines the primary operations exposed to the UI layer,
 * including task management, weather retrieval, and schedule planning.
 * </p>
 */
public interface TaskManager {

    /**
     * Adds a new task to the system.
     * <p>
     * Preconditions: {@code task} must not be {@code null} and should contain
     * valid identifiers and metadata.
     * </p>
     * <p>
     * Postconditions: the task is persisted and is available for subsequent
     * retrieval through {@link #getTasks()}.
     * </p>
     * <p>
     * Side effects: may validate the task and persist it to storage.
     * </p>
     *
     * @param task the task to add
     * @throws taskmanager.exception.InvalidTaskException if the task is {@code null}
     *         or contains invalid data
     */
    void addTask(Task task);

    /**
     * Removes a task by its unique ID.
     * <p>
     * Preconditions: {@code taskId} must not be {@code null} or blank.
     * </p>
     * <p>
     * Postconditions: the task is removed from persistent storage if it existed.
     * </p>
     * <p>
     * Side effects: may modify storage state and affect subsequent task retrieval.
     * </p>
     *
     * @param taskId the ID of the task to remove
     * @throws taskmanager.exception.InvalidTaskException if the task ID is null or empty
     * @throws taskmanager.exception.TaskNotFoundException if no task exists with the given ID
     */
    void removeTask(String taskId);

    /**
     * Returns all stored tasks.
     * <p>
     * Preconditions: the underlying task storage service must be available.
     * </p>
     * <p>
     * Postconditions: returns a snapshot list of all persisted tasks.
     * </p>
     *
     * @return a list containing all tasks; never {@code null}
     */
    List<Task> getTasks();

    /**
     * Fetches weather data asynchronously for a specific location.
     * <p>
     * Preconditions: {@code location} may be {@code null} or blank, but the
     * underlying service will validate the request.
     * </p>
     * <p>
     * Postconditions: the returned {@link Mono} either emits a valid forecast
     * or terminates with a {@link taskmanager.exception.WeatherAPIException}.
     * </p>
     *
     * @param location the city or location name
     * @return a Mono that emits the weather forecast
     * @throws taskmanager.exception.WeatherAPIException if the weather service fails
     */
    Mono<WeatherForecast> fetchWeather(String location);

    /**
     * Returns the schedule planner used to generate recommendations.
     * <p>
     * Preconditions: the planner must be configured during TaskManager construction.
     * </p>
     *
     * @return the schedule planner instance; never {@code null}
     */
    SchedulePlanner getPlanner();

    /**
     * Creates a new TaskManager builder.
     *
     * @return a builder for constructing a TaskManager instance
     */
    static TaskManagerBuilder builder() {
        return new DefaultTaskManagerBuilder();
    }

    /**
     * Builder interface for configuring and creating a TaskManager.
     */
    interface TaskManagerBuilder {

        /**
         * Sets the weather API key.
         *
         * @param apiKey the API key used by the weather service
         * @return the current builder instance
         */
        TaskManagerBuilder withWeatherApiKey(String apiKey);

        /**
         * Sets the file path used for task storage.
         *
         * @param path the storage file path
         * @return the current builder instance
         */
        TaskManagerBuilder withStoragePath(String path);

        /**
         * Builds the configured TaskManager.
         *
         * @return a ready-to-use TaskManager instance
         */
        TaskManager build();
    }
}
  