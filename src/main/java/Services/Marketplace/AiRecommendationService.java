package Services.Marketplace;

import Entities.Marketplace.Produit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AiRecommendationService {

    private static final String API_KEY = "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"; // REPLACE THIS
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    private static final String[] MODELS = {
        "openrouter/auto",
        "qwen/qwen-2.5-72b-instruct:free",
        "mistralai/mistral-7b-instruct:free",
        "meta-llama/llama-3.1-8b-instruct:free"
    };

    public List<Produit> getRecommendations(List<Produit> allProducts, List<Produit> pastOrders) {
        if (pastOrders == null) pastOrders = new ArrayList<>();
        
        List<Integer> purchasedIds = pastOrders.stream().map(Produit::getId).collect(Collectors.toList());
        
        List<Produit> available = allProducts.stream()
            .filter(p -> !purchasedIds.contains(p.getId()))
            .collect(Collectors.toList());
            
        Collections.shuffle(available);
        List<Produit> subset = available.subList(0, Math.min(50, available.size()));

        if (API_KEY.equals("YOUR_OPENROUTER_API_KEY")) {
            System.err.println("API Key not set for OpenRouter. Using fallback.");
            return getFallback(available, 4);
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Role: Master Sales Oracle for 'Shadow Dimensions'.\\n");
        prompt.append("Task: Select 4 artifacts.\\n");
        prompt.append("IMPORTANT: The user wants to be SURPRISED.\\n");
        if (!pastOrders.isEmpty()) {
            prompt.append("User has previously bought: ");
            for (Produit p : pastOrders) {
                prompt.append(p.getNom().replace("\"", "")).append(", ");
            }
            prompt.append("\\nMix items from similar categories with unexpected discoveries.\\n");
        } else {
            prompt.append("The user is new. Recommend the best general artifacts.\\n");
        }
        
        prompt.append("\\nCatalog (Selection of random artifacts):\\n");
        for (Produit p : subset) {
            prompt.append("{\\\"id\\\": ").append(p.getId()).append(", \\\"name\\\": \\\"").append(p.getNom().replace("\"", "\\\"")).append("\\\"}\\n");
        }
        prompt.append("\\nReturn ONLY JSON: {\\\"recommendations\\\": [{\\\"id\\\": X, \\\"reason\\\": \\\"message\\\"}]}");

        String escapedPrompt = prompt.toString().replace("\n", "\\n");

        for (String model : MODELS) {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);

                String jsonBody = "{"
                    + "\"model\": \"" + model + "\","
                    + "\"messages\": [{\"role\": \"user\", \"content\": \"" + escapedPrompt + "\"}],"
                    + "\"response_format\": {\"type\": \"json_object\"}"
                    + "}";

                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder responseStr = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        responseStr.append(line);
                    }
                    br.close();

                    String aiText = responseStr.toString();
                    List<Produit> recommendations = new ArrayList<>();
                    
                    Matcher m = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(aiText);
                    while (m.find()) {
                        int recId = Integer.parseInt(m.group(1));
                        allProducts.stream()
                                .filter(p -> p.getId() == recId && !purchasedIds.contains(recId))
                                .findFirst()
                                .ifPresent(p -> {
                                    if (!recommendations.contains(p)) {
                                        recommendations.add(p);
                                    }
                                });
                        if (recommendations.size() >= 4) break;
                    }
                    
                    if (recommendations.size() < 4) {
                        recommendations.addAll(getFallback(available, 4 - recommendations.size(), recommendations));
                    }
                    
                    return recommendations.subList(0, Math.min(4, recommendations.size()));
                } else {
                    System.err.println("OpenRouter Model " + model + " failed with HTTP " + responseCode);
                }
            } catch (Exception e) {
                System.err.println("OpenRouter Model " + model + " error: " + e.getMessage());
            }
        }
        
        // If all models fail, return fallback
        System.err.println("All AI models failed, using fallback.");
        return getFallback(available, 4);
    }
    
    private List<Produit> getFallback(List<Produit> available, int limit) {
        return getFallback(available, limit, new ArrayList<>());
    }
    
    private List<Produit> getFallback(List<Produit> available, int limit, List<Produit> exclude) {
        List<Produit> result = new ArrayList<>();
        List<Produit> pool = available.stream()
            .filter(p -> !exclude.contains(p))
            .collect(Collectors.toList());
        Collections.shuffle(pool);
        
        for (int i = 0; i < Math.min(limit, pool.size()); i++) {
            result.add(pool.get(i));
        }
        return result;
    }
}
