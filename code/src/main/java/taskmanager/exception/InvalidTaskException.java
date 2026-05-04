package taskmanager.exception;

/**
 * Exception thrown when a task contains invalid data.
 */
public class InvalidTaskException extends RuntimeException {

    /**
     * Creates an exception with a message.
     *
     * @param message explanation of the validation error
     */
    public InvalidTaskException(String message) {
        super(message);
    }

}
