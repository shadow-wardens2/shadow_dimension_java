package Services.Tutorials;

import Entities.Tutorials.Option;
import Entities.Tutorials.Question;
import Entities.Tutorials.Quiz;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI service that generates quiz questions and options for a given
 * formation/quiz context.
 * Uses OpenRouter (Gemini 2.0 Flash) and reads the API key from api_key.txt or
 * env vars.
 */
public class AiQuizService {

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "google/gemini-2.0-flash-001";
    private final HttpClient httpClient;

    public AiQuizService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Generates a list of Questions (with 4 Options each, 1 correct) for the given
     * quiz.
     * 
     * @param deepContextText Deep contextual string (Game Details, Formation
     *                        Description, Lessons content)
     * @param quizTitle       The title of the quiz
     * @param quiz            The quiz entity to attach to
     * @param numQuestions    How many questions to generate
     */
    public List<Question> generateQuestions(String deepContextText, String quizTitle, Quiz quiz, int numQuestions) {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("AI Quiz Forge: No API key found.");
            return Collections.emptyList();
        }

        String prompt = "You are the Shadow Oracle, an ancient and slightly dark AI entity that generates detailed game quizzes.\n"
                + "Generate exactly " + numQuestions + " multiple-choice questions for the quiz titled '" + quizTitle
                + "'.\n\n"
                + "Context: Video Game -> " + deepContextText + "\n\n"
                + "IMPORTANT INSTRUCTION: Provide challenging, general-knowledge questions EXCLUSIVELY about the video game '"
                + deepContextText
                + "'. Test the player's knowledge of its lore, characters, mechanics, and world. Do NOT mention any courses, chapters, or lessons.\n\n"
                + "For each question, provide EXACTLY 4 answer options, with exactly 1 correct answer.\n"
                + "Format your response EXACTLY like this (repeat for each question):\n"
                + "QUESTION: [question text]\n"
                + "A: [option text] | CORRECT\n"
                + "B: [option text]\n"
                + "C: [option text]\n"
                + "D: [option text]\n"
                + "---\n"
                + "Mark the correct answer with '| CORRECT' at the end of that line.\n"
                + "Do not add any extra text outside this format.";

        String aiResponse = callAi(prompt, apiKey);
        if (aiResponse == null || aiResponse.isBlank())
            return Collections.emptyList();

        return parseAiResponse(aiResponse, quiz);
    }

    private List<Question> parseAiResponse(String response, Quiz quiz) {
        List<Question> questions = new ArrayList<>();
        String[] blocks = response.split("---");

        for (String block : blocks) {
            block = block.trim();
            if (block.isBlank())
                continue;

            String[] lines = block.lines().map(String::trim).filter(l -> !l.isBlank()).toArray(String[]::new);
            String questionText = null;
            List<Option> options = new ArrayList<>();

            for (String line : lines) {
                if (line.startsWith("QUESTION:")) {
                    questionText = line.replace("QUESTION:", "").trim();
                } else if (line.matches("^[A-D]:.*")) {
                    String optionText = line.replaceFirst("^[A-D]:\\s*", "").trim();
                    boolean isCorrect = optionText.endsWith("| CORRECT");
                    optionText = optionText.replace("| CORRECT", "").trim();

                    Option opt = new Option();
                    opt.setTexte(optionText);
                    opt.setEstCorrecte(isCorrect);
                    options.add(opt);
                }
            }

            if (questionText != null && !options.isEmpty()) {
                // Shuffle options so correct answer position is randomized
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

    private String callAi(String userPrompt, String apiKey) {
        String escapedPrompt = userPrompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");

        String payload = "{" +
                "\"model\":\"" + MODEL + "\"," +
                "\"messages\":[" +
                "{\"role\":\"user\",\"content\":\"" + escapedPrompt + "\"}" +
                "]}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENROUTER_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "https://shadowdimensions.local")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("AI Quiz Forge error: " + response.statusCode() + " " + response.body());
                return null;
            }
            return extractContent(response.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String extractContent(String json) {
        try {
            int start = json.indexOf("\"content\":") + 11;
            int end = json.indexOf("\"", start);
            while (json.charAt(end - 1) == '\\')
                end = json.indexOf("\"", end + 1);
            return json.substring(start, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveApiKey() {
        return Utils.EnvConfig.get("OPENROUTER_API_KEY");
    }
}
