package taskmanager.exception;

/**
 * Exception thrown when weather data cannot be retrieved or processed.
 * <p>
 * This exception indicates a failure in the weather lookup pipeline,
 * such as network errors, invalid API responses, or missing location data.
 * </p>
 */
public class WeatherAPIException extends RuntimeException {

    /**
     * Creates an exception with a message and original cause.
     *
     * @param message explanation of the weather API error; should be user-friendly
     * @param cause original exception cause; may be {@code null}
     */
    public WeatherAPIException(String message, Throwable cause) {
        super(message, cause);
    }
}
