package taskmanager.impl;

import taskmanager.api.StorageService;
import taskmanager.api.TaskManager;
import taskmanager.api.WeatherService;
import taskmanager.service.FileTaskStorageService;
import taskmanager.service.OpenWeatherService;

/**
 * Builder implementation for creating a configured {@link taskmanager.api.TaskManager}.
 * <p>
 * This builder centralizes configuration for the task manager components,
 * including weather API integration and persistent storage path selection.
 * </p>
 */
public class DefaultTaskManagerBuilder implements TaskManager.TaskManagerBuilder {
    /** OpenWeather API key used by {@link OpenWeatherService}. */
    private String apiKey;

    /** File path for JSON task storage; defaults to {@code tasks.json}. */
    private String storagePath = "tasks.json";

    /**
     * Sets the weather API key.
     *
     * @param apiKey weather API key; must not be {@code null}
     * @return current builder
     */
    @Override
    public TaskManager.TaskManagerBuilder withWeatherApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    /**
     * Sets the storage path.
     * <p>
     * Preconditions: {@code path} may be {@code null} or blank, in which case
     * the default path {@code tasks.json} is retained.
     * </p>
     *
     * @param path JSON file path for task storage
     * @return current builder
     */
    @Override
    public TaskManager.TaskManagerBuilder withStoragePath(String path) {
        if (path != null && !path.isBlank()) {
            this.storagePath = path;
        }
        return this;
    }

    /**
     * Builds a {@link taskmanager.api.TaskManager} with task storage, weather service,
     * and schedule planning.
     * <p>
     * Postconditions: returns a TaskManager configured with the selected API key
     * and storage path.
     * </p>
     *
     * @return configured TaskManager
     */
    @Override
    public TaskManager build() {
        WeatherService weatherService = createWeatherService();
        StorageService storageService = new FileTaskStorageService(storagePath);
        DefaultTaskService taskService = new DefaultTaskService(storageService);
        DefaultSchedulePlanner planner = new DefaultSchedulePlanner(weatherService);

        return new DefaultTaskManager(taskService, planner, weatherService);
    }

    /**
     * Creates a weather service instance based on builder configuration.
     *
     * @return weather service implementation using the configured API key
     */
    private WeatherService createWeatherService() {
        return new OpenWeatherService(apiKey);
    }
}
