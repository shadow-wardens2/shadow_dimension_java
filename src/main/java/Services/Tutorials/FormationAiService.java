package Services.Tutorials;

import Entities.Tutorials.Formation;
import Entities.User.User;
import Utils.SessionManager;

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
        if (totalPassed >= 3)
            rank = "Adept";
        if (totalPassed >= 10)
            rank = "Shadow Master";

        StringBuilder sb = new StringBuilder();
        sb.append("User: ").append(user.getFullName()).append("\n");
        sb.append("Current Rank: ").append(rank).append("\n");

        sb.append("\nCONQUERED PATHS (Do NOT recommend these, user finished them):\n");
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

        sb.append("\nCURRENTLY STUDYING (Encourage finishing these):\n");
        for (Formation f : allFormations) {
            if (startedIds.contains(f.getId())) {
                // Only if not already finished
                long totalQuizzes = quizService.getAll().stream()
                        .filter(q -> q.getFormation() != null && q.getFormation().getId() == f.getId()).count();
                long passedQuizzes = quizService.getAll().stream()
                        .filter(q -> q.getFormation() != null && q.getFormation().getId() == f.getId())
                        .filter(q -> progressService.isQuizCompleted(user.getId(), q.getId()))
                        .count();
                if (passedQuizzes < totalQuizzes) {
                    sb.append("- ").append(f.getTitre()).append("\n");
                }
            }
        }

        sb.append("\nAVAILABLE FORMATIONS TO PICK FROM:\n");
        for (Formation f : allFormations) {
            sb.append("- ").append(f.getTitre()).append(" (Level: ").append(f.getNiveau()).append(")\n");
        }

        return sb.toString();
    }

    private String callAi(String context) throws IOException, InterruptedException {
        String apiKey = resolveApiKey();

        String systemPrompt = "You are the Shadow Dimensions Mentor. "
                + "Return exactly 5 formation recommendations from the 'AVAILABLE FORMATIONS' list. "
                + "IMPORTANT: Never recommend formations listed under 'CONQUERED PATHS'. "
                + "Use the User's Current Rank and 'CURRENTLY STUDYING' list to personalize the choices. "
                + "If they started a formation but didn't finish it, encourage them to complete their trial. "
                + "For each, provide the [TITRE] followed by a mysterious, dark-themed reason WHY this fits their current progress. "
                + "Format your response EXACTLY as follows for each item, separated by '|||':\n"
                + "TITLE: [Formation Title] | REASON: [Your mysterious 1-sentence reasoning]\n";

        String payload = "{" +
                "\"model\":\"" + MODEL + "\"," +
                "\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"" + escapeJson(systemPrompt) + "\"}," +
                "{\"role\":\"user\",\"content\":\"" + escapeJson("CONTEXT:\n" + context) + "\"}" +
                "]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENROUTER_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "https://shadowdimensions.local")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return "Error from Oracle API: " + response.statusCode();
        }

        return extractContent(response.body());
    }

    private String resolveApiKey() {
        return Utils.EnvConfig.get("OPENROUTER_API_KEY");
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String extractContent(String json) {
        // Very basic JSON extraction for "content"
        try {
            int start = json.indexOf("\"content\":") + 11;
            int end = json.indexOf("\"", start);
            while (json.charAt(end - 1) == '\\') {
                end = json.indexOf("\"", end + 1);
            }
            return json.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
        } catch (Exception e) {
            return "Could not parse Oracle's whisper.";
        }
    }
}
