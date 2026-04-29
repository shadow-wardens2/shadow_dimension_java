package Utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeApiManager {

    private static final String API_KEY = "YOUR_YOUTUBE_API_KEY_HERE"; // User should replace this
    private static final String API_URL = "https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails&id=%s&key=%s";

    public static Map<String, String> getVideoDetails(String videoUrl) {
        Map<String, String> details = new HashMap<>();
        String videoId = extractVideoId(videoUrl);

        if (videoId == null)
            return details;

        try {
            HttpClient client = HttpClient.newHttpClient();
            String url = String.format(API_URL, videoId, API_KEY);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray items = jsonResponse.getAsJsonArray("items");

                if (items.size() > 0) {
                    JsonObject item = items.get(0).getAsJsonObject();

                    // Thumbnail
                    JsonObject thumbnails = item.getAsJsonObject("snippet").getAsJsonObject("thumbnails");
                    String thumbnailUrl = thumbnails.getAsJsonObject("high").get("url").getAsString();
                    details.put("thumbnail", thumbnailUrl);

                    // Duration
                    String isoDuration = item.getAsJsonObject("contentDetails").get("duration").getAsString();
                    details.put("duration", formatDuration(isoDuration));

                    // Title
                    String title = item.getAsJsonObject("snippet").get("title").getAsString();
                    details.put("title", title);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return details;
    }

    public static String extractVideoId(String youtubeUrl) {
        String pattern = "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F)[^#&?\\n]*";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(youtubeUrl);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private static String formatDuration(String isoDuration) {
        try {
            Duration duration = Duration.parse(isoDuration);
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            long seconds = duration.toSecondsPart();

            if (hours > 0) {
                return String.format("%02d:%02d:%02d", hours, minutes, seconds);
            } else {
                return String.format("%02d:%02d", minutes, seconds);
            }
        } catch (Exception e) {
            return "00:00";
        }
    }
}
