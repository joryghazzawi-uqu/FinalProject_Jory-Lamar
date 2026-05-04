package taskmanager.api;

import reactor.core.publisher.Mono;
import taskmanager.model.WeatherForecast;

/**
 * Provides weather forecast data asynchronously.
 * <p>
 * Implementations may fetch live weather data from external APIs or provide
 * fallback forecasts when network access is unavailable.
 * </p>
 */
public interface WeatherService {

    /**
     * Fetches weather data for a location.
     * <p>
     * Preconditions: {@code location} must contain a valid location string.
     * </p>
     * <p>
     * Postconditions: the returned {@link Mono} emits a weather forecast
     * or terminates with an error.
     * </p>
     *
     * @param location the city or location name
     * @return a Mono emitting the weather forecast
     */
    Mono<WeatherForecast> getForecast(String location);
}
