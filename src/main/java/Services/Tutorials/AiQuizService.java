package Services.Tutorials;

import Entities.Tutorials.Option;
import Entities.Tutorials.Question;
import Entities.Tutorials.Quiz;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AiQuizService {

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "google/gemini-2.0-flash-001";
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public AiQuizService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public List<Question> generateQuestions(String deepContextText, String quizTitle, Quiz quiz, int numQuestions) {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("AI Quiz Forge: No API key found.");
            return Collections.emptyList();
        }

        String systemPrompt = "You are the Shadow Oracle. Return EXACTLY " + numQuestions + " questions for '" + quizTitle + "'.\n"
                + "Format: QUESTION: [text] followed by A:, B:, C:, D: options. Mark correct with '| CORRECT'.\n"
                + "Separate with '---'.";

        String userContext = "Context: " + deepContextText;
        String aiResponse = callAi(systemPrompt, userContext, apiKey);
        if (aiResponse == null || aiResponse.isBlank()) return Collections.emptyList();

        return parseAiResponse(aiResponse, quiz);
    }

    private List<Question> parseAiResponse(String response, Quiz quiz) {
        List<Question> questions = new ArrayList<>();
        String[] blocks = response.split("---");

        for (String block : blocks) {
            block = block.trim();
            if (block.isBlank()) continue;

            String[] lines = block.lines().map(String::trim).filter(l -> !l.isBlank()).toArray(String[]::new);
            String questionText = null;
            List<Option> options = new ArrayList<>();

            for (String line : lines) {
                String cleanLine = line.replace("**", "").replace("__", "").trim();
                if (cleanLine.toUpperCase().startsWith("QUESTION:")) {
                    questionText = cleanLine.substring("QUESTION:".length()).trim();
                } else if (cleanLine.matches("^(?i)[A-D][:.)].*")) {
                    String optionText = cleanLine.replaceFirst("^(?i)[A-D][:.)]\\s*", "").trim();
                    boolean isCorrect = optionText.toUpperCase().contains("| CORRECT") || optionText.toUpperCase().contains("(CORRECT)");
                    optionText = optionText.replace("| CORRECT", "").replace("(CORRECT)", "").trim();

                    Option opt = new Option();
                    opt.setTexte(optionText);
                    opt.setEstCorrecte(isCorrect);
                    options.add(opt);
                }
            }

            if (questionText != null && !options.isEmpty()) {
                Collections.shuffle(options);
                Question q = new Question();
                q.setTexte(questionText);
                q.setQuiz(quiz);
                q.setOptions(options);
                questions.add(q);
            }
        }
        return questions;
    }

    public String getQuizFeedback(String quizTitle, int score, int total, List<String> failedQuestions) {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) return "Oracle silent.";

        String systemPrompt = "You are the Shadow Dimensions Mentor. Give dark feedback.";
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Trial: '").append(quizTitle).append("'. Score: ").append(score).append("/").append(total).append(".\n");
        if (!failedQuestions.isEmpty()) {
            userPrompt.append("Failed: ").append(String.join(", ", failedQuestions));
        }

        return callAi(systemPrompt, userPrompt.toString(), apiKey);
    }

    private String callAi(String systemPrompt, String userPrompt, String apiKey) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", MODEL);
        
        JsonArray messages = new JsonArray();
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);
        
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);
        messages.add(userMsg);
        
        payload.add("messages", messages);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENROUTER_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "https://shadowdimensions.local")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            
            JsonObject root = gson.fromJson(response.body(), JsonObject.class);
            return root.getAsJsonArray("choices").get(0).getAsJsonObject().get("message").getAsJsonObject().get("content").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
