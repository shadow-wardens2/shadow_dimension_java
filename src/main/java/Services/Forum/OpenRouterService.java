package Services.Forum;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class OpenRouterService {

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "openrouter/free";

    private String getApiKey() {
        Utils.AppConfig.loadDotEnv();
        String key = Utils.AppConfig.get("OPENROUTER_API_KEY");
        if (key == null || key.isBlank()) {
            throw new RuntimeException("OPENROUTER_API_KEY is missing from .env file. Please add it.");
        }
        return key;
    }

    /**
     * Sends the text to OpenRouter to correct spelling and grammar.
     * Returns ONLY the corrected text.
     */
    public String correctSpellingAndGrammar(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + getApiKey());
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("HTTP-Referer", "http://localhost/"); // OpenRouter recommendation
            conn.setRequestProperty("X-Title", "Shadow Dimensions Forum"); // OpenRouter recommendation
            conn.setDoOutput(true);

            // Construct JSON request body
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are an expert copy editor. Your only task is to fix spelling, grammar, and punctuation mistakes in the text provided by the user. Do NOT change the meaning or tone. Return ONLY the corrected text. Do not include quotes, explanations, or conversational filler like 'Here is the corrected text'.");

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", text);

            JSONArray messages = new JSONArray();
            messages.put(systemMessage);
            messages.put(userMessage);

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", MODEL);
            requestBody.put("messages", messages);

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                // Parse the response
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject message = firstChoice.getJSONObject("message");
                    return message.getString("content").trim();
                }
                throw new Exception("No choices found in the response.");
            } else {
                // Read error stream for debugging
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                errorReader.close();
                throw new Exception("API Error " + responseCode + ": " + errorResponse.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("AI Correction Failed: " + e.getMessage(), e);
        }
    }
}
