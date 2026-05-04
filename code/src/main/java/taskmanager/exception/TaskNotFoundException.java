package taskmanager.exception;

/**
 * Exception thrown when a task cannot be found.
 * <p>
 * This runtime exception indicates that an operation referenced a missing task
 * identifier.
 * </p>
 */
public class TaskNotFoundException extends RuntimeException {

    /**
     * Creates an exception for a missing task.
     *
     * @param taskId the missing task ID; may be {@code null}
     */
    public TaskNotFoundException(String taskId) {
        super("Task not found: " + taskId);
    }
}
