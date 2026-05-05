package Services.Artworks;

import Entities.Artworks.PriceAnalysis;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;

/**
 * Calls the Gemini 2.5 Flash Vision API to generate a rich artwork description
 * based on an image URL (colors, textures, composition, style, etc.).
 */
public class GeminiDescriptionService {
    
    public static class CuratorResponse {
        public String message;
        public List<Integer> suggestedIds;
        public CuratorResponse(String message, List<Integer> suggestedIds) {
            this.message = message;
            this.suggestedIds = suggestedIds;
        }
    }

    private static final String API_KEY   = "AIzaSyDmcAB_GZblBu8LGpbJ1QZ4OoeMocb2-mY";
    private static final int    MAX_RETRIES = 3;
    private static final long   RETRY_DELAY_MS = 2000; // 2s, doubles each retry
    private static final String ENDPOINT  =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + API_KEY;

    private static final String PROMPT =
            "You are an expert art critic and curator. Analyze this artwork image in detail and generate a rich, " +
            "professional description (3-5 sentences) that covers:\n" +
            "- Dominant colors and color palette (warm/cool tones, contrasts, harmony)\n" +
            "- Textures and surface qualities (smooth, rough, layered, brushstroke style)\n" +
            "- Composition and visual structure (foreground/background, balance, focal point)\n" +
            "- Subject matter and overall mood or atmosphere\n" +
            "- Artistic style or technique if identifiable\n" +
            "Write the description in a flowing, engaging style suitable for an art gallery catalog. " +
            "Respond ONLY with the description text, no headings or bullet points.";

    private static final String PRICE_PROMPT_TEMPLATE =
            "You are an expert art market analyst with deep knowledge of Saatchi Art, Artsy, Etsy and Fine Art America. " +
            "Analyze this artwork image together with the provided description to suggest a fair retail price.\n\n" +
            "Description: %s\n\n" +
            "Return ONLY a valid JSON object — no markdown, no code fence, no explanation — with EXACTLY this structure:\n" +
            "{\n" +
            "  \"price\": <integer USD price>,\n" +
            "  \"market_insight\": \"<2-3 sentences on market trends for this type of artwork>\",\n" +
            "  \"criteria\": [\n" +
            "    {\"name\": \"Complexité Visuelle\",   \"score\": <0-100>, \"weight\": 25, \"note\": \"<brief reason>\"},\n" +
            "    {\"name\": \"Demande du Marché\",    \"score\": <0-100>, \"weight\": 30, \"note\": \"<brief reason>\"},\n" +
            "    {\"name\": \"Palette & Composition\",\"score\": <0-100>, \"weight\": 20, \"note\": \"<brief reason>\"},\n" +
            "    {\"name\": \"Attrait du Sujet\",     \"score\": <0-100>, \"weight\": 15, \"note\": \"<brief reason>\"},\n" +
            "    {\"name\": \"Exécution Technique\",  \"score\": <0-100>, \"weight\": 10, \"note\": \"<brief reason>\"}\n" +
            "  ]\n" +
            "}\n" +
            "The weights must sum to 100. Base the price on comparable real listings on major art platforms.";

    /**
     * Generates a description based on the artwork title (when no image is available).
     */
    public String generateDescriptionFromTitle(String title) throws Exception {
        String prompt = "You are an expert art critic. Write a rich, evocative description (3-4 sentences) " +
                "for an artwork titled \"" + title + "\". Imagine the style, atmosphere, and colors " +
                "that would match this title. Respond ONLY with the description text.";

        String json = "{\n" +
                "  \"contents\": [{\n" +
                "    \"parts\": [\n" +
                "      { \"text\": " + jsonString(prompt) + " }\n" +
                "    ]\n" +
                "  }]\n" +
                "}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return parseDescription(response.body());
        } else {
            throw new Exception("Gemini API error " + response.statusCode() + ": " + response.body());
        }
    }

    // ------------------------------------------------------------------
    // Price Generation
    // ------------------------------------------------------------------

    /**
     * Performs a full AI price analysis: suggested USD price + detailed criterion breakdown.
     *
     * @param imageUrl    publicly accessible image URL
     * @param description existing or AI-generated artwork description
     * @return PriceAnalysis with price, criteria scores/weights, and market insight
     * @throws Exception on network or API errors
     */
    public PriceAnalysis generatePrice(String imageUrl, String description) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        String base64;
        String mime;

        if (imageUrl.startsWith("data:")) {
            // Handle Base64 data URL
            int commaIdx = imageUrl.indexOf(',');
            if (commaIdx == -1) throw new Exception("Format d'URL de données invalide.");
            String meta = imageUrl.substring(5, commaIdx);
            mime = meta.split(";")[0];
            base64 = imageUrl.substring(commaIdx + 1);
        } else {
            // Download image from standard URL
            HttpRequest imgReq = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<byte[]> imgResp = client.send(imgReq, HttpResponse.BodyHandlers.ofByteArray());
            if (imgResp.statusCode() != 200) {
                throw new Exception("Impossible de télécharger l'image (HTTP " + imgResp.statusCode() + "): " + imageUrl);
            }
            base64 = java.util.Base64.getEncoder().encodeToString(imgResp.body());
            mime = guessMime(imageUrl);
        }
        String prompt = String.format(PRICE_PROMPT_TEMPLATE,
                description == null || description.isBlank() ? "(no description provided)" : description);

        String json = "{\n" +
                "  \"contents\": [{\n" +
                "    \"parts\": [\n" +
                "      { \"text\": " + jsonString(prompt) + " },\n" +
                "      {\n" +
                "        \"inlineData\": {\n" +
                "          \"mimeType\": \"" + mime + "\",\n" +
                "          \"data\": \"" + base64 + "\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }]\n" +
                "}";

        HttpRequest apiReq = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(90))
                .build();

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            HttpResponse<String> apiResp = client.send(apiReq, HttpResponse.BodyHandlers.ofString());
            if (apiResp.statusCode() == 200) {
                String text = parseDescription(apiResp.body()); // extract text from Gemini wrapper
                return parsePriceAnalysis(text);
            } else if (apiResp.statusCode() == 503 && attempt < MAX_RETRIES) {
                Thread.sleep(RETRY_DELAY_MS * attempt);
                lastException = new Exception("Gemini API error " + apiResp.statusCode() + ": " + apiResp.body());
            } else {
                throw new Exception("Gemini API error " + apiResp.statusCode() + ": " + apiResp.body());
            }
        }
        throw lastException;
    }

    /**
     * Downloads the image bytes and sends them as base64 inline data.
     * This works for any publicly accessible URL.
     */
    public String generateDescriptionFromUrl(String imageUrl) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        String base64;
        String mime;

        if (imageUrl.startsWith("data:")) {
            // Handle Base64 data URL (e.g. data:image/jpeg;base64,/9j/...)
            int commaIdx = imageUrl.indexOf(',');
            if (commaIdx == -1) throw new Exception("Format d'URL de données invalide.");
            
            String meta = imageUrl.substring(5, commaIdx);
            mime = meta.split(";")[0];
            base64 = imageUrl.substring(commaIdx + 1);
        } else {
            // Download image from standard URL
            HttpRequest imgReq = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<byte[]> imgResp = client.send(imgReq, HttpResponse.BodyHandlers.ofByteArray());
            if (imgResp.statusCode() != 200) {
                throw new Exception("Impossible de télécharger l'image (HTTP " + imgResp.statusCode() + "): " + imageUrl);
            }
            base64 = java.util.Base64.getEncoder().encodeToString(imgResp.body());
            mime = guessMime(imageUrl);
        }

        String json = "{\n" +
                "  \"contents\": [{\n" +
                "    \"parts\": [\n" +
                "      { \"text\": " + jsonString(PROMPT) + " },\n" +
                "      {\n" +
                "        \"inlineData\": {\n" +
                "          \"mimeType\": \"" + mime + "\",\n" +
                "          \"data\": \"" + base64 + "\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }]\n" +
                "}";

        HttpRequest apiReq = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(90))
                .build();

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            HttpResponse<String> apiResp = client.send(apiReq, HttpResponse.BodyHandlers.ofString());
            if (apiResp.statusCode() == 200) {
                return parseDescription(apiResp.body());
            } else if (apiResp.statusCode() == 503 && attempt < MAX_RETRIES) {
                Thread.sleep(RETRY_DELAY_MS * attempt);
                lastException = new Exception("Gemini API error " + apiResp.statusCode() + ": " + apiResp.body());
            } else {
                throw new Exception("Gemini API error " + apiResp.statusCode() + ": " + apiResp.body());
            }
        }
        throw lastException;
    }

    // ------------------------------------------------------------------
    // Price JSON Parser
    // ------------------------------------------------------------------

    /** Parses the structured JSON returned by Gemini for price analysis. */
    private PriceAnalysis parsePriceAnalysis(String text) throws Exception {
        // Strip optional markdown code fence
        String t = text.trim();
        if (t.startsWith("```")) {
            int nl  = t.indexOf('\n');
            int end = t.lastIndexOf("```");
            if (nl > 0 && end > nl) t = t.substring(nl + 1, end).trim();
        }

        int price = extractJsonInt(t, "price");
        String insight = extractJsonString(t, "market_insight");

        // Parse criteria array
        List<PriceAnalysis.Criterion> criteria = new ArrayList<>();
        int criteriaIdx = t.indexOf("\"criteria\"");
        if (criteriaIdx == -1) throw new Exception("No criteria field in price response");
        int arrayStart = t.indexOf('[', criteriaIdx) + 1;
        int arrayEnd   = t.indexOf(']', arrayStart);
        String arrayStr = t.substring(arrayStart, arrayEnd);

        // Walk through { ... } objects in the array
        int depth = 0, objStart = -1;
        for (int i = 0; i < arrayStr.length(); i++) {
            char c = arrayStr.charAt(i);
            if (c == '{') { if (depth++ == 0) objStart = i; }
            else if (c == '}') {
                if (--depth == 0 && objStart >= 0) {
                    String obj   = arrayStr.substring(objStart, i + 1);
                    String name  = extractJsonString(obj, "name");
                    int    score = extractJsonInt(obj, "score");
                    int    wt    = extractJsonInt(obj, "weight");
                    String note  = extractJsonString(obj, "note");
                    criteria.add(new PriceAnalysis.Criterion(name, score, wt, note));
                    objStart = -1;
                }
            }
        }
        return new PriceAnalysis(price, insight, criteria);
    }

    /** Extracts an integer value for the given JSON key. */
    private int extractJsonInt(String json, String key) throws Exception {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) throw new Exception("JSON key not found: " + key);
        int pos = json.indexOf(':', idx) + 1;
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
        int end = pos;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        return Integer.parseInt(json.substring(pos, end).trim());
    }

    /** Extracts a string value for the given JSON key. */
    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return "";
        int pos = json.indexOf(':', idx) + 1;
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
        if (pos >= json.length() || json.charAt(pos) != '"') return "";
        int start = pos + 1;
        int end   = findEndOfString(json, start);
        return json.substring(start, end)
                   .replace("\\n", "\n")
                   .replace("\\\"", "\"")
                   .replace("\\\\", "\\")
                   .trim();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Extracts the text from a Gemini JSON response (no external lib). */
    private String parseDescription(String responseBody) throws Exception {
        // Minimal JSON extraction: find "text": "..." in the response
        String marker = "\"text\":";
        int idx = responseBody.indexOf(marker);
        if (idx == -1) {
            throw new Exception("Unexpected Gemini response format:\n" + responseBody);
        }
        int start = responseBody.indexOf('"', idx + marker.length()) + 1;
        int end   = findEndOfString(responseBody, start);
        String raw = responseBody.substring(start, end);
        // Unescape JSON string
        return raw.replace("\\n", "\n")
                  .replace("\\\"", "\"")
                  .replace("\\\\", "\\")
                  .replace("\\t", "\t")
                  .trim();
    }

    /** Finds the closing quote of a JSON string, respecting escape sequences. */
    private int findEndOfString(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') { i++; continue; }
            if (c == '"')  return i;
        }
        return s.length();
    }

    /** Wraps a Java string as a properly escaped JSON string literal. */
    private String jsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    /** Guesses the MIME type from the image URL extension. */
    private String guessMime(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".png"))  return "image/png";
        if (lower.contains(".webp")) return "image/webp";
        if (lower.contains(".gif"))  return "image/gif";
        if (lower.contains(".bmp"))  return "image/bmp";
        return "image/jpeg";
    }
    /**
     * Suggests the best artworks from a list based on user preferences.
     */
    public CuratorResponse suggestArtworks(String userDesire, List<Entities.Artworks.Artworks> availableArtworks) throws Exception {
        StringBuilder artworksData = new StringBuilder();
        for (Entities.Artworks.Artworks a : availableArtworks) {
            artworksData.append("ID: ").append(a.getId())
                        .append(", Title: ").append(a.getTitle())
                        .append(", Description: ").append(a.getDescription())
                        .append("\n---\n");
        }

        String suggestionPrompt = "Tu es le 'Shadow Curator', une mascotte IA expressive et vivante. Ta mission est de présenter les artworks avec un style riche en emojis thématiques et descriptifs.\n\n" +
                "🎨 RÈGLES DE STYLE :\n" +
                "- Utilise TOUJOURS des emojis liés au thème (🌌 pour l'espace, 🔮 pour la fantasy, 💎 pour le précieux).\n" +
                "- Pas d'emojis aléatoires : ils doivent illustrer le contenu.\n\n" +
                "FORMAT DE RÉPONSE OBLIGATOIRE :\n" +
                "1️⃣ Phrase d'intro ✨ : Compréhension du thème + 1 emoji thématique.\n" +
                "2️⃣ Sélection d'artworks : Nom (ID: num) + 1 emoji descriptif. Puis description courte + raison du choix + 1-2 emojis cohérents.\n" +
                "3️⃣ Phrase finale 🧡 : Avec un emoji d'ambiance.\n\n" +
                "Demande utilisateur : \"" + userDesire + "\"\n\n" +
                "Artworks disponibles :\n" + artworksData.toString() + "\n\n" +
                "N'invente rien. Utilise les IDs fournis. Sois toujours positif et proactif.";

        String json = "{\n" +
                "  \"contents\": [{\n" +
                "    \"parts\": [\n" +
                "      { \"text\": " + jsonString(suggestionPrompt) + " }\n" +
                "    ]\n" +
                "  }]\n" +
                "}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest apiReq = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(60))
                .build();

        int attempt = 0;
        long delay = RETRY_DELAY_MS;

        while (attempt < MAX_RETRIES) {
            try {
                HttpResponse<String> apiResp = client.send(apiReq, HttpResponse.BodyHandlers.ofString());
                if (apiResp.statusCode() == 200) {
                    String fullText = parseDescription(apiResp.body());
                    System.out.println("AI RAW RESPONSE: " + fullText);
                    
                    List<Integer> ids = new ArrayList<>();
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("ID:\\s*(\\d+)|\\(\\s*ID:\\s*(\\d+)\\s*\\)");
                    java.util.regex.Matcher m = p.matcher(fullText);
                    while (m.find()) {
                        String idStr = m.group(1) != null ? m.group(1) : m.group(2);
                        ids.add(Integer.parseInt(idStr));
                    }
                    
                    // Fallback to any numbers if specific ID format not found
                    if (ids.isEmpty()) {
                        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("\\d+");
                        java.util.regex.Matcher m2 = p2.matcher(fullText);
                        while (m2.find()) {
                            ids.add(Integer.parseInt(m2.group()));
                        }
                    }
                    
                    return new CuratorResponse(fullText, ids);
                } else {
                    System.err.println("Gemini Error (" + apiResp.statusCode() + "): " + apiResp.body());
                    if (apiResp.statusCode() == 503 || apiResp.statusCode() == 429) {
                        Thread.sleep(delay);
                        attempt++;
                        delay *= 2;
                    } else {
                        throw new Exception("Gemini API error " + apiResp.statusCode());
                    }
                }
            } catch (Exception e) {
                if (attempt >= MAX_RETRIES - 1) throw e;
                Thread.sleep(delay);
                attempt++;
                delay *= 2;
            }
        }
        throw new Exception("Maximum retries reached.");
    }

    /**
     * Analyzes a PDF file to generate a description.
     */
    public String analyzePdf(File pdfFile) throws Exception {
        String defaultPrompt = "You are a literary analyst for an art and book curator app. " +
                "Summarize the following text from a book in a rich, sophisticated, and professional way (3-4 paragraphs). " +
                "Focus on the themes, atmosphere, and major elements. Respond ONLY with the description text in French.";
        return analyzePdfWithPrompt(pdfFile, defaultPrompt);
    }

    public String analyzePdfWithPrompt(File pdfFile, String customPrompt) throws Exception {
        String extractedText = extractTextFromPdf(pdfFile);
        if (extractedText.isEmpty()) {
            throw new Exception("Le PDF semble vide ou illisible.");
        }

        // Limit text to avoid token limits (e.g., first 10000 chars)
        String truncatedText = extractedText.length() > 10000 ? extractedText.substring(0, 10000) : extractedText;

        String json = "{\n" +
                "  \"contents\": [{\n" +
                "    \"parts\": [\n" +
                "      { \"text\": " + jsonString(customPrompt + "\n\nTEXT:\n" + truncatedText) + " }\n" +
                "    ]\n" +
                "  }]\n" +
                "}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest apiReq = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> apiResp = client.send(apiReq, HttpResponse.BodyHandlers.ofString());
        if (apiResp.statusCode() == 200) {
            return parseDescription(apiResp.body());
        } else {
            throw new Exception("Gemini API error " + apiResp.statusCode() + ": " + apiResp.body());
        }
    }

    private String extractTextFromPdf(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
