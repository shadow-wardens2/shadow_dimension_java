package Services.Tutorials;

import Entities.Tutorials.Formation;
import Entities.User.User;
import Utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;

public class FormationAiService {

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "google/gemini-2.0-flash-001";

    private final ServiceFormation formationService;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public FormationAiService() {
        this.formationService = new ServiceFormation();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String getPersonalizedRecommendation() {
        User user = SessionManager.getCurrentUser();
        if (user == null)
            return "Unlock the shadows by logging in first.";

        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return "AI Oracle connection lost. Set OPENROUTER_API_KEY to reconnect.";
        }

        try {
            String context = buildUserContext(user);
            return callAi(context);
        } catch (Exception e) {
            return "The Oracle is silent: " + e.getMessage();
        }
    }

    private String buildUserContext(User user) throws SQLException {
        List<Formation> allFormations = formationService.getAll();
        ServiceQuizProgress progressService = new ServiceQuizProgress();
        ServiceQuiz quizService = new ServiceQuiz();

        int totalPassed = progressService.getCompletedQuizzesCount(user.getId());
        List<Integer> startedIds = progressService.getStartedFormations(user.getId());

        String rank = "Neophyte";
        if (totalPassed >= 3) rank = "Adept";
        if (totalPassed >= 10) rank = "Shadow Master";

        StringBuilder sb = new StringBuilder();
        sb.append("User: ").append(user.getFullName()).append("\n");
        sb.append("Current Rank: ").append(rank).append("\n");

        sb.append("\nCONQUERED PATHS (Do NOT recommend these):\n");
        for (Formation f : allFormations) {
            long totalQuizzes = quizService.getAll().stream()
                    .filter(q -> q.getFormation() != null && q.getFormation().getId() == f.getId()).count();
            long passedQuizzes = quizService.getAll().stream()
                    .filter(q -> q.getFormation() != null && q.getFormation().getId() == f.getId())
                    .filter(q -> progressService.isQuizCompleted(user.getId(), q.getId()))
                    .count();

            if (totalQuizzes > 0 && totalQuizzes == passedQuizzes) {
                sb.append("- ").append(f.getTitre()).append("\n");
            }
        }

        sb.append("\nCURRENTLY STUDYING:\n");
        for (Formation f : allFormations) {
            if (startedIds.contains(f.getId())) {
                sb.append("- ").append(f.getTitre()).append("\n");
            }
        }

        sb.append("\nAVAILABLE FORMATIONS:\n");
        for (Formation f : allFormations) {
            sb.append("- ").append(f.getTitre()).append(" (Level: ").append(f.getNiveau()).append(")\n");
        }

        return sb.toString();
    }

    private String callAi(String context) throws IOException, InterruptedException {
        String apiKey = resolveApiKey();
        if (apiKey == null) return "Error: API Key not found.";

        String systemPrompt = "You are the Shadow Dimensions Mentor. Return exactly 5 recommendations. "
                + "Format: TITLE: [Title] | REASON: [Reason] ||| ...";

        JsonObject payload = new JsonObject();
        payload.addProperty("model", MODEL);
        
        JsonArray messages = new JsonArray();
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);
        
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", "CONTEXT:\n" + context);
        messages.add(userMsg);
        
        payload.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENROUTER_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "https://shadowdimensions.local")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) return "Error: " + response.statusCode();
        
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        return root.getAsJsonArray("choices").get(0).getAsJsonObject().get("message").getAsJsonObject().get("content").getAsString();
    }

    private String resolveApiKey() {
        String key = System.getenv("OPENROUTER_API_KEY");
        if (key != null && !key.isBlank()) return key.trim();

        String[] paths = { ".env", "api_key.txt", "src/main/resources/.env" };
        for (String path : paths) {
            java.io.File file = new java.io.File(path);
            if (file.exists()) {
                try {
                    List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
                    for (String line : lines) {
                        if (line.contains("OPENROUTER_API_KEY=")) {
                            return line.split("=")[1].replace("\"", "").replace("'", "").trim();
                        }
                    }
                } catch (IOException ignored) {}
            }
        }
        return null;
    }
}
