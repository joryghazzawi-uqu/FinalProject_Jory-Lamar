package taskmanager.impl;

import reactor.core.publisher.Mono;
import taskmanager.api.SchedulePlanner;
import taskmanager.api.TaskManager;
import taskmanager.api.TaskService;
import taskmanager.api.WeatherService;
import taskmanager.exception.WeatherAPIException;
import taskmanager.model.Task;
import taskmanager.model.WeatherForecast;

import java.util.List;

/**
 * Default implementation of the {@link TaskManager} facade.
 * <p>
 * This class coordinates task persistence, schedule planning, and weather
 * lookup by delegating requests to the configured internal services.
 * It provides a synchronous facade for task operations while preserving
 * the reactive weather lookup semantics.
 * </p>
 */
public class DefaultTaskManager implements TaskManager {
    /** Service responsible for task CRUD operations and storage persistence. */
    private final TaskService taskService;

    /** Planner responsible for generating schedule recommendations. */
    private final SchedulePlanner planner;

    /** Weather service responsible for fetching forecast data. */
    private final WeatherService weatherService;

    /**
     * Constructs a {@code DefaultTaskManager} with the required service components.
     *
     * <p>Preconditions: none of the constructor parameters may be {@code null}.
     * The caller is responsible for providing fully initialized service instances.</p>
     *
     * @param taskService service used for task operations; must not be {@code null}
     * @param planner service used for schedule recommendations; must not be {@code null}
     * @param weatherService service used for weather lookup; must not be {@code null}
     * @postcondition the returned instance is ready to process task operations,
     *                weather lookups, and schedule planning requests.
     */
    public DefaultTaskManager(
            TaskService taskService,
            SchedulePlanner planner,
            WeatherService weatherService
    ) {
        this.taskService = taskService;
        this.planner = planner;
        this.weatherService = weatherService;
    }

    /**
     * Adds or updates a task in storage.
     * <p>
     * Preconditions: {@code task} must not be {@code null} and must satisfy
     * task validation rules defined by the underlying {@link TaskService}.
     * </p>
     * <p>
     * Postconditions: the task is persisted and will be returned by subsequent
     * {@link #getTasks()} calls if the underlying storage succeeds.
     * </p>
     * <p>
     * Side effects: may block until the reactive save operation completes.
     * </p>
     *
     * @param task the task to save
     * @throws taskmanager.exception.InvalidTaskException if the task is {@code null}
     *         or contains invalid data according to {@link TaskService}
     */
    @Override
    public void addTask(Task task) {
        taskService.addTask(task).block();
    }

    /**
     * Removes a task by its unique identifier.
     * <p>
     * Preconditions: {@code taskId} must not be {@code null} or blank.
     * </p>
     * <p>
     * Postconditions: if the task exists, it is deleted from storage and
     * subsequent calls to {@link #getTasks()} will no longer include it.
     * </p>
     * <p>
     * Side effects: may block while the underlying reactive removal and save
     * operations complete.
     * </p>
     *
     * @param taskId the task ID to remove
     * @throws taskmanager.exception.InvalidTaskException if {@code taskId} is null
     *         or blank
     * @throws taskmanager.exception.TaskNotFoundException if no task exists
     *         with the given ID
     */
    @Override
    public void removeTask(String taskId) {
        taskService.removeTask(taskId).block();
    }

    /**
     * Retrieves all stored tasks.
     * <p>
     * Preconditions: the underlying task storage service must be available.
     * </p>
     * <p>
     * Postconditions: returns a snapshot of the current task set in storage.
     * </p>
     * <p>
     * Side effects: may block while loading tasks from underlying storage.
     * </p>
     *
     * @return list of stored tasks; never {@code null}
     */
    @Override
    public List<Task> getTasks() {
        return taskService.findAllTasksAsList().block();
    }

    /**
     * Fetches weather data for a given location.
     * <p>
     * Preconditions: {@code location} may be {@code null} or blank, but the
     * underlying weather service will validate it before making a request.
     * </p>
     * <p>
     * Postconditions: returns a {@link Mono} that either emits a valid
     * {@link WeatherForecast} or fails with a {@link taskmanager.exception.WeatherAPIException}.
     * </p>
     * <p>
     * Side effects: delegates weather retrieval to the configured
     * {@link WeatherService} and may transform the resulting error type.
     * </p>
     *
     * @param location the location name used for weather lookup
     * @return Mono emitting weather forecast
     * @throws taskmanager.exception.WeatherAPIException if the weather lookup fails
     */
    @Override
    public Mono<WeatherForecast> fetchWeather(String location) {
        return weatherService.getForecast(location)
                .onErrorMap(error -> {
                    if (error instanceof WeatherAPIException) {
                        return error;
                    }
                    return new WeatherAPIException("Weather lookup failed", error);
                });
    }

    /**
     * Returns the internal task service.
     *
     * @return the task service instance
     */
    public TaskService getTaskService() {
        return taskService;
    }

    /**
     * Returns the configured schedule planner.
     * <p>
     * Preconditions: the planner must have been supplied at construction time.
     * </p>
     * <p>
     * Postconditions: the returned planner may be used to generate recommendations.
     * </p>
     *
     * @return the schedule planner instance; never {@code null}
     */
    @Override
    public SchedulePlanner getPlanner() {
        return planner;
    }
}
