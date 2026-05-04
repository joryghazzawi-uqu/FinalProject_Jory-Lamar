package taskmanager.impl;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import taskmanager.api.SchedulePlanner;
import taskmanager.api.WeatherService;
import taskmanager.model.ScheduleRecommendation;
import taskmanager.model.Task;
import taskmanager.model.WeatherForecast;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link SchedulePlanner}.
 * <p>
 * Generates schedule recommendations by analyzing task weather sensitivity
 * together with forecast information.
 * </p>
 * <p>
 * Uses Reactor to offload recommendation generation to a background scheduler.
 * </p>
 */
public class DefaultSchedulePlanner implements SchedulePlanner {
    /** Service used to fetch weather forecasts when location-based planning is requested. */
    private final WeatherService weatherService;

    /**
     * Creates a schedule planner.
     *
     * @param weatherService service used to fetch weather when location is provided; must not be {@code null}
     */
    public DefaultSchedulePlanner(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * Generates recommendations using a provided weather forecast.
     * <p>
     * Preconditions: {@code tasks} must not be {@code null} and each task must
     * have valid scheduling information. {@code forecast} must not be {@code null}.
     * </p>
     * <p>
     * Postconditions: the returned {@link Mono} emits a list of recommendations
     * reflecting the provided forecast and task sensitivity.
     * </p>
     * <p>
     * Side effects: none beyond performing in-memory recommendation generation.
     * </p>
     *
     * @param tasks tasks to analyze
     * @param forecast weather forecast
     * @return Mono emitting schedule recommendations
     */
    @Override
    public Mono<List<ScheduleRecommendation>> suggestSchedule(
            List<Task> tasks,
            WeatherForecast forecast
    ) {
        return Mono.fromCallable(() ->
                        tasks.stream()
                                .map(task -> new ScheduleRecommendation(
                                        task,
                                        recommendationFor(task, forecast)
                                ))
                                .collect(Collectors.toList())
                )
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Fetches forecast by location then generates recommendations.
     * <p>
     * Preconditions: {@code tasks} must not be {@code null} and
     * {@code location} must be a valid location string.
     * </p>
     * <p>
     * Postconditions: the returned {@link Mono} emits recommendations based on
     * fetched weather data.
     * </p>
     * <p>
     * Side effects: delegates weather retrieval to {@link WeatherService}.
     * </p>
     *
     * @param tasks tasks to analyze
     * @param location location used for weather lookup
     * @return Mono emitting schedule recommendations
     * @throws taskmanager.exception.WeatherAPIException if weather data cannot be fetched
     */
    @Override
    public Mono<List<ScheduleRecommendation>> suggestScheduleForLocation(
            List<Task> tasks,
            String location
    ) {
        return weatherService.getForecast(location)
                .flatMap(forecast -> suggestSchedule(tasks, forecast));
    }

    /**
     * Creates one recommendation message for one task.
     * <p>
     * Preconditions: {@code task} and {@code forecast} must not be {@code null}.
     * </p>
     * <p>
     * Postconditions: returns a textual recommendation describing whether the
     * task should proceed based on current weather conditions.
     * </p>
     *
     * @param task task to analyze
     * @param forecast weather forecast
     * @return recommendation message
     */
    private String recommendationFor(Task task, WeatherForecast forecast) {
        if (!task.isWeatherSensitive()) {
            return "Task is not weather sensitive. Weather: " + forecast.getCondition();
        }

        if (forecast.getPrecipitationProbability() > 0.6) {
            return "High risk: postpone or move indoors. Weather: " + forecast.getCondition();
        }

        if (forecast.getTemperatureCelsius() >= 35) {
            return "High temperature: consider a cooler time. Temperature: "
                    + forecast.getTemperatureCelsius() + "\u00b0C";
        }

        return "Good to go as planned. Weather: " + forecast.getCondition();
    }
}
