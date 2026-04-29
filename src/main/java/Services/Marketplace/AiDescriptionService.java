package Services.Marketplace;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import Utils.EnvConfig;

public class AiDescriptionService {
                    
    private static final String API_KEY = EnvConfig.get("OPENROUTER_API_KEY");
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    private static final String[] MODELS = {
        "openrouter/auto",
        "qwen/qwen-2.5-72b-instruct:free",
        "mistralai/mistral-7b-instruct:free",
        "meta-llama/llama-3.1-8b-instruct:free"
    };

    public String generateDescription(String name, String category) throws Exception {
        if (name == null || name.isEmpty()) {
            return "Veuillez d'abord saisir un nom de produit.";
        }
        
        category = (category == null || category.isEmpty()) ? "Inconnu" : category;

        StringBuilder prompt = new StringBuilder();
        prompt.append("Role: Expert en Marketing et Rédaction de Fiches Produits.\\n");
        prompt.append("Task: Rédiger une description professionnelle, réaliste et attractive pour un produit de vente en ligne.\\n\\n");
        prompt.append("Nom du Produit: ").append(name.replace("\"", "")).append("\\n");
        prompt.append("Catégorie: ").append(category.replace("\"", "")).append("\\n\\n");
        prompt.append("Règles:\\n");
        prompt.append("1. **Professionnalisme**: Utilisez un ton commercial, clair et persuasif. Pas de fantastique, pas de magie.\\n");
        prompt.append("2. **Contenu**: Mettez en avant les caractéristiques du produit, son utilité et ses avantages pour le client.\\n");
        prompt.append("3. **Style**: Phrases courtes et percutantes.\\n");
        prompt.append("4. **Longueur**: 1 à 2 paragraphes (max 100 mots).\\n");
        prompt.append("5. **Langue**: Français.\\n");
        prompt.append("6. Retournez UNIQUEMENT un objet JSON: {\\\"description\\\": \\\"Le texte ici\\\"}");

        String escapedPrompt = prompt.toString();

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

                    String responseBody = responseStr.toString();
                    System.out.println("AI Response Body: " + responseBody);
                    
                    // First, find the "content" field
                    int contentIdx = responseBody.indexOf("\"content\":");
                    if (contentIdx != -1) {
                        String afterContent = responseBody.substring(contentIdx + 10);
                        
                        // Try to find the description value regardless of escaping depth
                        // We look for description, then : , then a quote, then capture until the next quote
                        Pattern p = Pattern.compile("description.*?[:\\\"\\s]+(.*?)(?<!\\\\)\\\"", Pattern.DOTALL);
                        Matcher m = p.matcher(afterContent);
                        
                        if (m.find()) {
                            String desc = m.group(1);
                            // Clean up escapes
                            desc = desc.replaceAll("\\\\+", "\\\\"); // Reduce multiple backslashes
                            desc = desc.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").trim();
                            
                            // Remove accidental leading/trailing artifacts
                            if (desc.startsWith("\"")) desc = desc.substring(1);
                            if (desc.endsWith("\"")) desc = desc.substring(0, desc.length() - 1);
                            if (desc.endsWith("}")) desc = desc.substring(0, desc.length() - 1);
                            if (desc.endsWith("\"")) desc = desc.substring(0, desc.length() - 1); // Double check if quote was inside brace

                            return desc.trim();
                        }

                        // Ultimate fallback: return a cleaned version of the whole content if it's not too long
                        int startBrace = afterContent.indexOf("{");
                        int endBrace = afterContent.lastIndexOf("}");
                        if (startBrace != -1 && endBrace != -1 && endBrace > startBrace) {
                            String rawContent = afterContent.substring(startBrace, endBrace + 1);
                            // Simple cleanup for fallback
                            String cleaned = rawContent.replace("\\n", "\n").replace("\\\"", "\"").trim();
                            // Try to strip the JSON structure if it's visible
                            cleaned = cleaned.replaceAll("^\\{.*?description\":\\s*\"", "");
                            cleaned = cleaned.replaceAll("\"\\s*\\}$", "");
                            return cleaned.trim();
                        }
                    }
                    
                    return "Une énergie sombre empêche de lire la description (" + model + ").";
                } else {
                    System.err.println("AiDescriptionService: model " + model + " returned HTTP " + responseCode);
                }
            } catch (Exception e) {
                System.err.println("AiDescriptionService: model " + model + " failed: " + e.getMessage());
            }
        }

        throw new Exception("The Archival Shadows are too thick to read right now.");
    }
}
