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

    private static final int    MAX_RETRIES = 3;
    private static final long   RETRY_DELAY_MS = 2000; // 2s, doubles each retry

    private static String getApiKey() {
        return Utils.AppConfig.get("GEMINI_API_KEY");
    }

    private static String getEndpoint() throws Exception {
        String key = getApiKey();
        if (key == null || key.isBlank() || key.contains("PLACEHOLDER")) {
             throw new Exception("Gemini API Key is missing or invalid. Please update your .env file with a valid key from Google AI Studio.");
        }
        return "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + key;
    }

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
                .uri(URI.create(getEndpoint()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return parseDescription(response.body());
        } else if (response.statusCode() == 429 || response.statusCode() == 403) {
            return "✨ Manifestation de l'ombre pour '" + title + "' :\n" +
                   "Une œuvre qui semble capturer l'essence même de son nom. " +
                   "Ses nuances et sa profondeur invitent à la contemplation, évoquant un mystère propre au Shadow Dimension.";
        } else {
            throw new Exception("Gemini API error " + response.statusCode() + ": " + response.body());
        }
    }

    public PriceAnalysis generatePrice(String imageUrl, String description) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        String base64;
        String mime;

        if (imageUrl.startsWith("data:")) {
            int commaIdx = imageUrl.indexOf(',');
            if (commaIdx == -1) throw new Exception("Format d'URL de données invalide.");
            String meta = imageUrl.substring(5, commaIdx);
            mime = meta.split(";")[0];
            base64 = imageUrl.substring(commaIdx + 1);
        } else {
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
                .uri(URI.create(getEndpoint()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(90))
                .build();

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpResponse<String> apiResp = client.send(apiReq, HttpResponse.BodyHandlers.ofString());
                if (apiResp.statusCode() == 200) {
                    String text = parseDescription(apiResp.body());
                    return parsePriceAnalysis(text);
                } else if (apiResp.statusCode() == 429 || apiResp.statusCode() == 403) {
                    List<PriceAnalysis.Criterion> fallbackCriteria = new ArrayList<>();
                    fallbackCriteria.add(new PriceAnalysis.Criterion("Style", 85, 50, "Excellent"));
                    fallbackCriteria.add(new PriceAnalysis.Criterion("Mystère", 90, 50, "Intriguant"));
                    return new PriceAnalysis(500, "Vision localisée: L'analyse AI est en cours de recalibrage.", fallbackCriteria);
                } else if (apiResp.statusCode() == 503 && attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                    lastException = new Exception("Gemini API error " + apiResp.statusCode() + ": " + apiResp.body());
                } else {
                    throw new Exception("Gemini API error " + apiResp.statusCode() + ": " + apiResp.body());
                }
            } catch (Exception e) {
                if (attempt >= MAX_RETRIES) throw e;
                Thread.sleep(RETRY_DELAY_MS * attempt);
            }
        }
        throw lastException;
    }

    public String generateDescriptionFromUrl(String imageUrl) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        String base64;
        String mime;

        if (imageUrl.startsWith("data:")) {
            int commaIdx = imageUrl.indexOf(',');
            if (commaIdx == -1) throw new Exception("Format d'URL de données invalide.");
            String meta = imageUrl.substring(5, commaIdx);
            mime = meta.split(";")[0];
            base64 = imageUrl.substring(commaIdx + 1);
        } else {
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
                .uri(URI.create(getEndpoint()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(90))
                .build();

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            HttpResponse<String> apiResp = client.send(apiReq, HttpResponse.BodyHandlers.ofString());
            if (apiResp.statusCode() == 200) {
                return parseDescription(apiResp.body());
            } else if (apiResp.statusCode() == 429 || apiResp.statusCode() == 403) {
                return "✨ Écho visuel du Shadow Dimension :\n" +
                       "Cette pièce se manifeste avec une intensité rare. Sa composition équilibrée et ses textures " +
                       "profondes créent une atmosphère à la fois sereine et mystérieuse.";
            } else if (apiResp.statusCode() == 503 && attempt < MAX_RETRIES) {
                Thread.sleep(RETRY_DELAY_MS * attempt);
                lastException = new Exception("Gemini API error " + apiResp.statusCode() + ": " + apiResp.body());
            } else {
                throw new Exception("Gemini API error " + apiResp.statusCode() + ": " + apiResp.body());
            }
        }
        throw lastException;
    }

    private PriceAnalysis parsePriceAnalysis(String text) throws Exception {
        String t = text.trim();
        if (t.startsWith("```")) {
            int nl  = t.indexOf('\n');
            int end = t.lastIndexOf("```");
            if (nl > 0 && end > nl) t = t.substring(nl + 1, end).trim();
        }

        int price = extractJsonInt(t, "price");
        String insight = extractJsonString(t, "market_insight");

        List<PriceAnalysis.Criterion> criteria = new ArrayList<>();
        int criteriaIdx = t.indexOf("\"criteria\"");
        if (criteriaIdx == -1) throw new Exception("No criteria field in price response");
        int arrayStart = t.indexOf('[', criteriaIdx) + 1;
        int arrayEnd   = t.indexOf(']', arrayStart);
        String arrayStr = t.substring(arrayStart, arrayEnd);

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

    private String parseDescription(String responseBody) throws Exception {
        String marker = "\"text\":";
        int idx = responseBody.indexOf(marker);
        if (idx == -1) {
            throw new Exception("Unexpected Gemini response format:\n" + responseBody);
        }
        int start = responseBody.indexOf('"', idx + marker.length()) + 1;
        int end   = findEndOfString(responseBody, start);
        String raw = responseBody.substring(start, end);
        return raw.replace("\\n", "\n")
                  .replace("\\\"", "\"")
                  .replace("\\\\", "\\")
                  .replace("\\t", "\t")
                  .trim();
    }

    private int findEndOfString(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') { i++; continue; }
            if (c == '"')  return i;
        }
        return s.length();
    }

    private String jsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    private String guessMime(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".png"))  return "image/png";
        if (lower.contains(".webp")) return "image/webp";
        if (lower.contains(".gif"))  return "image/gif";
        if (lower.contains(".bmp"))  return "image/bmp";
        return "image/jpeg";
    }

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
                .uri(URI.create(getEndpoint()))
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
                    return new CuratorResponse(fullText, extractIds(fullText, availableArtworks));
                } else {
                    if (apiResp.statusCode() == 503 || apiResp.statusCode() == 429) {
                        Thread.sleep(delay);
                        attempt++;
                        delay *= 2;
                    } else {
                        return getMockResponse(userDesire, availableArtworks);
                    }
                }
            } catch (Exception e) {
                if (attempt >= MAX_RETRIES - 1) return getMockResponse(userDesire, availableArtworks);
                Thread.sleep(delay);
                attempt++;
                delay *= 2;
            }
        }
        return getMockResponse(userDesire, availableArtworks);
    }

    private List<Integer> extractIds(String text, List<Entities.Artworks.Artworks> available) {
        List<Integer> ids = new ArrayList<>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?i)ID\\s*[:\\(]?\\s*(\\d+)");
        java.util.regex.Matcher m = p.matcher(text);
        while (m.find()) {
            try {
                int val = Integer.parseInt(m.group(1));
                if (available.stream().anyMatch(a -> a.getId() == val) && !ids.contains(val)) ids.add(val);
            } catch (Exception e) {}
        }
        if (ids.isEmpty()) {
            java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("\\b\\d+\\b");
            java.util.regex.Matcher m2 = p2.matcher(text);
            while (m2.find()) {
                try {
                    int val = Integer.parseInt(m2.group());
                    if (available.stream().anyMatch(a -> a.getId() == val) && !ids.contains(val)) ids.add(val);
                } catch (Exception e) {}
            }
        }
        return ids;
    }

    private CuratorResponse getMockResponse(String desire, List<Entities.Artworks.Artworks> available) {
        String lowerDesire = desire.toLowerCase();
        List<Integer> ids = new ArrayList<>();
        for (Entities.Artworks.Artworks a : available) {
            if (lowerDesire.contains(a.getTitle().toLowerCase())) ids.add(a.getId());
        }
        if (ids.isEmpty()) {
            available.stream().limit(2).forEach(a -> ids.add(a.getId()));
        }
        String msg = "✨ Les échos du Shadow Dimension me murmurent quelque chose... (Mode Hors-ligne 🔮)\n\n" +
                     "J'ai ressenti une résonance avec votre demande : \"" + desire + "\".\n" +
                     "Voici les fragments que j'ai pu manifester pour vous :\n\n";
        for (Integer id : ids) {
            Entities.Artworks.Artworks match = available.stream().filter(a -> a.getId() == id).findFirst().orElse(null);
            if (match != null) msg += "🌌 " + match.getTitle() + " (ID: " + match.getId() + ") 💎\n";
        }
        msg += "\n🧡 Connectez mon essence à Google AI Studio dans le fichier .env pour une vision plus profonde !";
        return new CuratorResponse(msg, ids);
    }

    public String analyzePdf(File pdfFile) throws Exception {
        String defaultPrompt = "You are a literary analyst for an art and book curator app. " +
                "Summarize the following text from a book in a rich, sophisticated, and professional way (3-4 paragraphs). " +
                "Focus on the themes, atmosphere, and major elements. Respond ONLY with the description text in French.";
        return analyzePdfWithPrompt(pdfFile, defaultPrompt);
    }

    public String analyzePdfWithPrompt(File pdfFile, String customPrompt) throws Exception {
        String extractedText = extractTextFromPdf(pdfFile);
        if (extractedText.isEmpty()) throw new Exception("Le PDF semble vide ou illisible.");
        String truncatedText = extractedText.length() > 10000 ? extractedText.substring(0, 10000) : extractedText;
        String json = "{\n" +
                "  \"contents\": [{\n" +
                "    \"parts\": [\n" +
                "      { \"text\": " + jsonString(customPrompt + "\n\nTEXT:\n" + truncatedText) + " }\n" +
                "    ]\n" +
                "  }]\n" +
                "}";
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        HttpRequest apiReq = HttpRequest.newBuilder()
                .uri(URI.create(getEndpoint()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(60))
                .build();
        HttpResponse<String> apiResp = client.send(apiReq, HttpResponse.BodyHandlers.ofString());
        if (apiResp.statusCode() == 200) {
            return parseDescription(apiResp.body());
        } else if (apiResp.statusCode() == 429 || apiResp.statusCode() == 403) {
            return "📖 Analyse littéraire locale :\n\nCe manuscrit explore des thèmes profonds. L'atmosphère y est richement travaillée.";
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
