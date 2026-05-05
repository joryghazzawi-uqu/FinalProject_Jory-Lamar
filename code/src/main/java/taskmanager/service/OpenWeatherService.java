package taskmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import taskmanager.api.WeatherService;
import taskmanager.exception.WeatherAPIException;
import taskmanager.model.WeatherForecast;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link WeatherService} implementation that connects to the OpenWeatherMap
 * API.
 * <p>
 * This class fetches live weather data, converts the API JSON response into a
 * {@link WeatherForecast}, caches results, and executes network operations on
 * a background thread.
 * </p>
 */
public class OpenWeatherService implements WeatherService {
        private static final String WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather";

        /** Duration for which a cached forecast is considered valid. */
        private static final Duration CACHE_EXPIRATION = Duration.ofMinutes(15);

        /** API key for OpenWeatherMap requests. */
        private final String apiKey;

        /** HTTP client used to send requests. */
        private final HttpClient httpClient;

        /** JSON mapper used to parse API responses. */
        private final ObjectMapper objectMapper;

        /** Cache of recent forecasts keyed by lowercase location. */
        private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

        /**
         * Creates an OpenWeatherService.
         *
         * @param apiKey OpenWeatherMap API key; may be {@code null} to disable real API
         *               calls
         */
        public OpenWeatherService(String apiKey) {
                this.apiKey = apiKey;
                this.httpClient = HttpClient.newHttpClient();
                this.objectMapper = new ObjectMapper();
        }

        /**
         * Fetches a weather forecast for a location.
         * <p>
         * Preconditions: {@code location} must not be {@code null} or blank.
         * </p>
         * <p>
         * Postconditions: returns a {@link Mono} that emits a valid forecast or fails
         * with {@link taskmanager.exception.WeatherAPIException}.
         * </p>
         * <p>
         * Side effects: may perform network I/O and cache the received forecast.
         * </p>
         *
         * @param location city or location name
         * @return Mono emitting the weather forecast
         * @throws WeatherAPIException if the location is empty or the API request fails
         */
        @Override
        public Mono<WeatherForecast> getForecast(String location) {
                if (location == null || location.isBlank()) {
                        return Mono.error(new WeatherAPIException("Location must not be empty", null));
                }

                return Mono.fromCallable(() -> fetchForecast(location.trim()))
                                .subscribeOn(Schedulers.boundedElastic())
                                .retryWhen(
                                                Retry.backoff(2, Duration.ofSeconds(1))
                                                                .filter(error -> error instanceof IOException
                                                                                || error instanceof WeatherAPIException))
                                .onErrorMap(error -> {
                                        if (error instanceof WeatherAPIException) {
                                                return error;
                                        }
                                        return new WeatherAPIException(
                                                        "Failed to fetch weather for " + location,
                                                        error);
                                });
        }

        /**
         * Performs the HTTP request and parses the API response.
         * <p>
         * Preconditions: {@code location} must be a trimmed non-empty location string.
         * </p>
         * <p>
         * Postconditions: returns a parsed {@link WeatherForecast} and caches it for
         * future requests within the configured expiration window.
         * </p>
         * <p>
         * Side effects: may perform a blocking HTTP request and update the internal
         * cache.
         * </p>
         *
         * @param location city or location name
         * @return parsed weather forecast
         * @throws IOException          if network or JSON parsing fails
         * @throws InterruptedException if the HTTP request is interrupted
         */
        private WeatherForecast fetchForecast(String location)
                        throws IOException, InterruptedException {

                String cacheKey = location.toLowerCase();
                CacheEntry cached = cache.get(cacheKey);

                if (cached != null && LocalDateTime.now().isBefore(cached.expiration())) {
                        return cached.forecast();
                }

                String encodedLocation = URLEncoder.encode(
                                location,
                                StandardCharsets.UTF_8);

                URI uri = URI.create(
                                WEATHER_URL
                                                + "?q=" + encodedLocation
                                                + "&units=metric"
                                                + "&appid=" + apiKey);

                HttpRequest request = HttpRequest.newBuilder(uri)
                                .GET()
                                .timeout(Duration.ofSeconds(10))
                                .build();

                HttpResponse<String> response = httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400) {
                        throw new WeatherAPIException(
                                        "Weather API returned status " + response.statusCode(),
                                        null);
                }

                JsonNode node = objectMapper.readTree(response.body());

                double temperature = node.path("main").path("temp").asDouble(Double.NaN);
                String condition = readCondition(node);
                double precipitationProbability = calculatePrecipitationProbability(node);

                WeatherForecast forecast = new WeatherForecast(
                                location,
                                LocalDateTime.now(),
                                temperature,
                                condition,
                                precipitationProbability);

                cache.put(
                                cacheKey,
                                new CacheEntry(
                                                forecast,
                                                LocalDateTime.now().plus(CACHE_EXPIRATION)));

                return forecast;
        }

        /**
         * Reads the main weather condition from the JSON response.
         * <p>
         * Preconditions: {@code node} must contain the parsed API response tree.
         * </p>
         *
         * @param node API response JSON
         * @return weather condition text
         */
        private String readCondition(JsonNode node) {
                JsonNode weatherArray = node.path("weather");

                if (weatherArray.isArray() && !weatherArray.isEmpty()) {
                        return weatherArray.get(0).path("main").asText("Unknown");
                }

                return "Unknown";
        }

        /**
         * Calculates an approximate precipitation probability.
         * <p>
         * OpenWeather current weather API does not always provide probability,
         * so this method estimates it using rain, snow, and cloud data.
         * </p>
         *
         * @param node API response JSON
         * @return probability value between 0.0 and 1.0
         */
        private double calculatePrecipitationProbability(JsonNode node) {
                if (node.has("rain") || node.has("snow")) {
                        return 0.9;
                }

                return node.path("clouds").path("all").asDouble(0.0) / 100.0;
        }

        /**
         * Cache record storing a forecast and its expiration time.
         *
         * @param forecast   cached forecast
         * @param expiration cache expiration time
         */
        private record CacheEntry(
                        WeatherForecast forecast,
                        LocalDateTime expiration) {
        }
}
