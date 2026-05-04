package taskmanager.exception;

/**
 * Exception thrown when a task contains invalid data.
 * <p>
 * This runtime exception indicates that a task failed validation prior to
 * storage or processing.
 * </p>
 */
public class InvalidTaskException extends RuntimeException {

    /**
     * Creates an exception with a message.
     *
     * @param message explanation of the validation error; must describe the invalid condition
     */
    public InvalidTaskException(String message) {
        super(message);
    }

}
