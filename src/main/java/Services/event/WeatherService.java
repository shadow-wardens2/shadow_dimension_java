package Services.event;

import Utils.AppConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeatherService {

    private static final int FORECAST_DAYS = 7;
    private final HttpClient client = HttpClient.newHttpClient();

    public WeatherService() {
        AppConfig.loadDotEnv();
    }

    public ForecastResult getForecast(String location) throws IOException, InterruptedException {
        if (location == null || location.isBlank()) {
            throw new IOException("Location is required.");
        }

        GeocodeResult geocode = geocode(location.trim());
        if (geocode == null) {
            throw new IOException("Unable to resolve location.");
        }

        return fetchForecast(geocode);
    }

    private GeocodeResult geocode(String location) throws IOException, InterruptedException {
        String baseUrl = AppConfig.getOrDefault("NOMINATIM_API_URL", "https://nominatim.openstreetmap.org/search");
        String url = baseUrl
                + "?format=json&limit=1&q="
                + URLEncoder.encode(location, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "ShadowDimensions/1.0")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return null;
        }

        JSONArray results = new JSONArray(response.body());
        if (results.isEmpty()) {
            return null;
        }

        JSONObject first = results.getJSONObject(0);
        double latitude = Double.parseDouble(first.getString("lat"));
        double longitude = Double.parseDouble(first.getString("lon"));
        String displayName = first.optString("display_name", location);

        return new GeocodeResult(latitude, longitude, displayName);
    }

    private ForecastResult fetchForecast(GeocodeResult geocode) throws IOException, InterruptedException {
        String baseUrl = AppConfig.getOrDefault("OPEN_METEO_API_URL", "https://api.open-meteo.com/v1/forecast");
        String url = String.format(Locale.ROOT,
                "%s?latitude=%.5f&longitude=%.5f&daily=weathercode,temperature_2m_max,temperature_2m_min,precipitation_sum&timezone=auto",
                baseUrl, geocode.latitude(), geocode.longitude());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Forecast request failed.");
        }

        JSONObject root = new JSONObject(response.body());
        JSONObject daily = root.optJSONObject("daily");
        if (daily == null) {
            throw new IOException("Forecast data unavailable.");
        }

        JSONArray dates = daily.optJSONArray("time");
        JSONArray codes = daily.optJSONArray("weathercode");
        JSONArray maxTemps = daily.optJSONArray("temperature_2m_max");
        JSONArray minTemps = daily.optJSONArray("temperature_2m_min");
        JSONArray precipitation = daily.optJSONArray("precipitation_sum");

        List<ForecastDay> days = new ArrayList<>();
        int total = dates == null ? 0 : Math.min(FORECAST_DAYS, dates.length());
        for (int i = 0; i < total; i++) {
            LocalDate date = LocalDate.parse(dates.getString(i));
            int code = codes != null ? codes.optInt(i, -1) : -1;
            double max = maxTemps != null ? maxTemps.optDouble(i, 0) : 0;
            double min = minTemps != null ? minTemps.optDouble(i, 0) : 0;
            double rain = precipitation != null ? precipitation.optDouble(i, 0) : 0;
            String description = describeWeather(code);
            boolean badWeather = isBadWeather(code, rain);

            days.add(new ForecastDay(date, max, min, rain, code, description, badWeather));
        }

        return new ForecastResult(geocode.displayName(), geocode.latitude(), geocode.longitude(), days);
    }

    private boolean isBadWeather(int code, double precipitation) {
        if (precipitation >= 5.0) {
            return true;
        }
        return switch (code) {
            case 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77,
                 80, 81, 82, 85, 86, 95, 96, 99 -> true;
            default -> false;
        };
    }

    private String describeWeather(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1, 2, 3 -> "Partly cloudy";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75 -> "Snow";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Unknown";
        };
    }

    private record GeocodeResult(double latitude, double longitude, String displayName) {
    }

    public record ForecastDay(LocalDate date, double maxTemp, double minTemp, double precipitation,
                              int weatherCode, String description, boolean badWeather) {
    }

    public record ForecastResult(String locationName, double latitude, double longitude,
                                 List<ForecastDay> days) {
    }
}
