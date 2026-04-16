package Services.event;

// Category model used when building AI context snapshots.
import Entities.event.Category;
// Event model used when building AI context snapshots.
import Entities.event.Event;

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

// Service that builds local event context and queries OpenRouter chat completion API.
public class EventAiAssistantService {

    // OpenRouter endpoint URL.
    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    // Model id requested from OpenRouter.
    private static final String MODEL = "openai/gpt-4o-mini";

    // Event data service used for context assembly.
    private final EventService eventService;
    // Category data service used for context assembly.
    private final CategoryService categoryService;
    // Shared HTTP client for outbound API calls.
    private final HttpClient httpClient;

    // Constructor initializes dependencies.
    public EventAiAssistantService() {
        this.eventService = new EventService();
        this.categoryService = new CategoryService();
        // Configures HTTP client with connection timeout.
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    // Main API used by chatbot controller to ask one question.
    public String askQuestion(String question) {
        // Reads API key from JVM property or environment variable.
        String apiKey = resolveApiKey();
        // Returns explicit message when key is missing.
        if (apiKey == null || apiKey.isBlank()) {
            return "AI key missing. Set OPENROUTER_API_KEY (or JVM property openrouter.api.key), then ask again.";
        }

        // Builds contextual dataset from local DB.
        String context;
        try {
            context = buildDataContext();
        } catch (SQLException e) {
            // Returns human-readable context loading error.
            return "I could not load event data right now: " + e.getMessage();
        }

        // System prompt constrains answer style and scope.
        String systemPrompt = "You are the Shadow Dimensions event oracle assistant. "
            + "Use only the provided data context. "
            + "If data is missing, say so clearly. "
            + "Give concise, useful answers with short suggestions when relevant.";

        // User prompt includes dynamic context and user question.
        String userPrompt = "DATA CONTEXT:\n" + context + "\n\nUSER QUESTION:\n" + question;

        // Builds raw JSON payload (manual escaping, no external serializer dependency).
        String payload = "{" +
            "\"model\":\"" + escapeJson(MODEL) + "\"," +
            "\"temperature\":0.35," +
            "\"messages\":[" +
            "{\"role\":\"system\",\"content\":\"" + escapeJson(systemPrompt) + "\"}," +
            "{\"role\":\"user\",\"content\":\"" + escapeJson(userPrompt) + "\"}" +
            "]}";

        // Constructs HTTP POST request with required OpenRouter headers.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENROUTER_URL))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "https://shadowdimensions.local")
                .header("X-Title", "ShadowDimensions Event Assistant")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            // Executes request synchronously.
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            // Handles non-2xx status codes.
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "AI service error (" + response.statusCode() + "). Please verify your OpenRouter key and model access.";
            }

            // Extracts assistant text from JSON response body.
            String content = extractAssistantContent(response.body());
            // Handles missing/empty assistant content.
            if (content == null || content.isBlank()) {
                return "No response was returned by the AI service.";
            }
            // Returns final assistant response text.
            return content;
        } catch (IOException | InterruptedException e) {
            // Handles network/thread interruption errors.
            return "Failed to call AI service: " + e.getMessage();
        }
    }

    // Starter suggestion list used to fill chatbot quick buttons.
    public String[] starterSuggestions() {
        return new String[]{
                "Show me event count by category",
                "Which categories have no events yet?",
                "Suggest how to improve event capacity planning"
        };
    }

    // Builds compact textual context from categories and events in database.
    private String buildDataContext() throws SQLException {
        // Loads all events/categories via services.
        List<Event> events = eventService.getAll();
        List<Category> categories = categoryService.getAll();

        // String builder used to assemble context document.
        StringBuilder sb = new StringBuilder();
        sb.append("Categories (latest up to 20):\n");
        // Limits category lines to avoid very large payloads.
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
        // Limits event lines to keep token usage reasonable.
        int eventLimit = Math.min(40, events.size());
        // Formatter for readable event date output.
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

        // Adds fallback line when no events exist.
        if (events.isEmpty()) {
            sb.append("- No events found.\n");
        }
        // Adds fallback line when no categories exist.
        if (categories.isEmpty()) {
            sb.append("- No categories found.\n");
        }

        // Returns assembled context text.
        return sb.toString();
    }

    // Resolves API key from system property first, then environment variable.
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

    // Normalizes free-text fields for concise context output.
    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        // Compresses whitespace and trims.
        String compact = value.replaceAll("\\s+", " ").trim();
        // Limits max visible length to avoid huge prompts.
        return compact.length() > 80 ? compact.substring(0, 80) + "..." : compact;
    }

    // Formats timestamp safely with fallback.
    private String formatTimestamp(Timestamp ts, SimpleDateFormat formatter) {
        return ts == null ? "N/A" : formatter.format(ts);
    }

    // Escapes string for manual JSON assembly.
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Minimal parser extracting first "content" string from response JSON.
    private String extractAssistantContent(String json) {
        // Guards against empty responses.
        if (json == null || json.isBlank()) {
            return null;
        }

        // Finds first content key occurrence.
        String marker = "\"content\"";
        int markerIndex = json.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }

        // Locates value separator and opening quote.
        int colonIndex = json.indexOf(':', markerIndex + marker.length());
        if (colonIndex < 0) {
            return null;
        }

        int firstQuote = json.indexOf('"', colonIndex + 1);
        if (firstQuote < 0) {
            return null;
        }

        // Rebuilds decoded text, handling basic escape sequences.
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = firstQuote + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaped) {
                switch (ch) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(ch);
                }
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                break;
            }
            sb.append(ch);
        }
        return sb.toString().trim();
    }
}
