package Services.event;

import Entities.event.Category;
import Entities.event.Event;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.List;

public class EventAiAssistantService {

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "openai/gpt-4o-mini";

    private final EventService eventService;
    private final CategoryService categoryService;
    private final HttpClient httpClient;

    public EventAiAssistantService() {
        this.eventService = new EventService();
        this.categoryService = new CategoryService();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String askQuestion(String question) {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return "AI key missing. Set OPENROUTER_API_KEY (or JVM property openrouter.api.key), then ask again.";
        }

        String context;
        try {
            context = buildDataContext();
        } catch (SQLException e) {
            return "I could not load event data right now: " + e.getMessage();
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("model", MODEL);
        payload.addProperty("temperature", 0.35);

        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content",
                "You are the Shadow Dimensions event oracle assistant. "
                        + "Use only the provided data context. "
                        + "If data is missing, say so clearly. "
                        + "Give concise, useful answers with short suggestions when relevant.");
        messages.add(systemMessage);

        JsonObject contextMessage = new JsonObject();
        contextMessage.addProperty("role", "user");
        contextMessage.addProperty("content", "DATA CONTEXT:\n" + context + "\n\nUSER QUESTION:\n" + question);
        messages.add(contextMessage);

        payload.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENROUTER_URL))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "https://shadowdimensions.local")
                .header("X-Title", "ShadowDimensions Event Assistant")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "AI service error (" + response.statusCode() + "). Please verify your OpenRouter key and model access.";
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                return "No response was returned by the AI service.";
            }

            JsonObject first = choices.get(0).getAsJsonObject();
            JsonObject message = first.getAsJsonObject("message");
            if (message == null || !message.has("content")) {
                return "AI response format was unexpected.";
            }

            return message.get("content").getAsString();
        } catch (IOException | InterruptedException e) {
            return "Failed to call AI service: " + e.getMessage();
        }
    }

    public String[] starterSuggestions() {
        return new String[]{
                "Show me event count by category",
                "Which categories have no events yet?",
                "Suggest how to improve event capacity planning"
        };
    }

    private String buildDataContext() throws SQLException {
        List<Event> events = eventService.getAll();
        List<Category> categories = categoryService.getAll();

        StringBuilder sb = new StringBuilder();
        sb.append("Categories (latest up to 20):\n");
        int categoryLimit = Math.min(20, categories.size());
        for (int i = 0; i < categoryLimit; i++) {
            Category c = categories.get(i);
            sb.append("- ID=").append(c.getId())
                    .append(", Name=").append(clean(c.getNom()))
                    .append(", Pricing=").append(clean(c.getTypeTarification()))
                    .append(", Price=").append(c.getPrix() == null ? "N/A" : c.getPrix())
                    .append("\n");
        }

        sb.append("\nEvents (latest up to 40):\n");
        int eventLimit = Math.min(40, events.size());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (int i = 0; i < eventLimit; i++) {
            Event e = events.get(i);
            sb.append("- ID=").append(e.getId())
                    .append(", Title=").append(clean(e.getTitle()))
                    .append(", Category=").append(clean(e.getCategoryName()))
                    .append(", Status=").append(clean(e.getStatus()))
                    .append(", Location=").append(clean(e.getLocation()))
                    .append(", Capacity=").append(e.getCapacity())
                    .append(", Start=").append(formatTimestamp(e.getStartDate(), dateFormat))
                    .append("\n");
        }

        if (events.isEmpty()) {
            sb.append("- No events found.\n");
        }
        if (categories.isEmpty()) {
            sb.append("- No categories found.\n");
        }

        return sb.toString();
    }

    private String resolveApiKey() {
        String fromProperty = System.getProperty("openrouter.api.key");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }

        String fromEnv = System.getenv("OPENROUTER_API_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        return null;
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() > 80 ? compact.substring(0, 80) + "..." : compact;
    }

    private String formatTimestamp(Timestamp ts, SimpleDateFormat formatter) {
        return ts == null ? "N/A" : formatter.format(ts);
    }
}
