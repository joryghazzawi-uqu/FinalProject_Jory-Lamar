package taskmanager.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Represents a task in the Smart Task Manager.
 * <p>
 * A task contains an identifier, title, optional description, scheduled
 * due date/time, and a flag indicating whether its execution depends on weather.
 * </p>
 */
public class Task {
    /** Unique identifier for the task. */
    private final String id;

    /** User-facing title for the task. */
    private String title;

    /** Optional details describing the task. */
    private String description;

    /** Due date and time for the task. */
    private LocalDateTime dueDateTime;

    /** True when task execution depends on weather conditions. */
    private boolean weatherSensitive;

    /**
     * Creates a new task.
     *
     * @param id unique task ID
     * @param title task title
     * @param dueDateTime task due date and time
     * @param weatherSensitive true if the task depends on weather conditions
     */
    @JsonCreator
    public Task(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("dueDateTime") LocalDateTime dueDateTime,
            @JsonProperty("weatherSensitive") boolean weatherSensitive
    ) {
        this.id = id;
        this.title = title;
        this.dueDateTime = dueDateTime;
        this.weatherSensitive = weatherSensitive;
    }

    /**
     * @return the task ID
     */
    public String getId() {
        return id;
    }

    /**
     * @return the task title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Updates the task title.
     * <p>
     * Preconditions: {@code title} may be {@code null} or blank, but the task
     * may fail validation later if it is invalid.
     * </p>
     * <p>
     * Postconditions: {@link #getTitle()} returns the new value.
     * </p>
     *
     * @param title the new title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the task description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Updates the task description.
     * <p>
     * Postconditions: {@link #getDescription()} returns the updated description.
     * </p>
     *
     * @param description the new description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the task due date and time
     */
    public LocalDateTime getDueDateTime() {
        return dueDateTime;
    }

    /**
     * Updates the task due date and time.
     * <p>
     * Preconditions: {@code dueDateTime} may be {@code null}, but the task
     * may fail validation later if unset.
     * </p>
     * <p>
     * Postconditions: {@link #getDueDateTime()} returns the new value.
     * </p>
     *
     * @param dueDateTime the new due date and time
     */
    public void setDueDateTime(LocalDateTime dueDateTime) {
        this.dueDateTime = dueDateTime;
    }

    /**
     * @return true if the task depends on weather conditions
     */
    public boolean isWeatherSensitive() {
        return weatherSensitive;
    }

    /**
     * Updates whether the task is weather sensitive.
     * <p>
     * Postconditions: {@link #isWeatherSensitive()} reflects the new flag.
     * </p>
     *
     * @param weatherSensitive true if weather affects this task
     */
    public void setWeatherSensitive(boolean weatherSensitive) {
        this.weatherSensitive = weatherSensitive;
    }
}
