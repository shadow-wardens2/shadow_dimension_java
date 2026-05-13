package Services.Artworks;

import Entities.Artworks.PriceAnalysis;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
        String key = Utils.AppConfig.get("GEMINI_API_KEY");
        if (key == null || key.isBlank() || key.contains("PLACEHOLDER")) {
            return "AIzaSyBrYxGavTqAykLwNSki3a0SOTON_2tRBYk";
        }
        return key;
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
        imageUrl = normalizeImageUrl(imageUrl);
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
            ensureImageContentType(imgResp, imageUrl);
            base64 = java.util.Base64.getEncoder().encodeToString(imgResp.body());
            mime = extractMimeType(imgResp, imageUrl);
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
        imageUrl = normalizeImageUrl(imageUrl);
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
            ensureImageContentType(imgResp, imageUrl);
            base64 = java.util.Base64.getEncoder().encodeToString(imgResp.body());
            mime = extractMimeType(imgResp, imageUrl);
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

    public String generateVisualDescriptionFromUrl(String imageUrl) throws Exception {
        imageUrl = normalizeImageUrl(imageUrl);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        String base64;
        String mime;

        if (imageUrl.startsWith("data:")) {
            int commaIdx = imageUrl.indexOf(',');
            if (commaIdx == -1) {
                throw new Exception("Format d'URL de donnÃ©es invalide.");
            }
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
                throw new Exception("Impossible de tÃ©lÃ©charger l'image (HTTP " + imgResp.statusCode() + "): " + imageUrl);
            }
            ensureImageContentType(imgResp, imageUrl);
            base64 = java.util.Base64.getEncoder().encodeToString(imgResp.body());
            mime = extractMimeType(imgResp, imageUrl);
        }

        String visualPrompt =
                "You are an expert visual art critic. Analyze the image itself and write a description in French based only on visible elements. " +
                "Describe colors, textures, composition, shapes, materials, lighting, mood, and notable visual details. " +
                "Do not invent context outside the image. Respond with one rich gallery-style paragraph.";

        String json = "{\n" +
                "  \"contents\": [{\n" +
                "    \"parts\": [\n" +
                "      { \"text\": " + jsonString(visualPrompt) + " },\n" +
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
            }
            if ((apiResp.statusCode() == 503 || apiResp.statusCode() == 429) && attempt < MAX_RETRIES) {
                Thread.sleep(RETRY_DELAY_MS * attempt);
                lastException = new Exception("Gemini API error " + apiResp.statusCode() + ": " + apiResp.body());
                continue;
            }
            throw new Exception("L'analyse visuelle IA a Ã©chouÃ© (HTTP " + apiResp.statusCode() + "). Aucun descriptif fondÃ© sur l'image n'a Ã©tÃ© produit.");
        }

        throw lastException != null ? lastException : new Exception("L'analyse visuelle IA a Ã©chouÃ©.");
    }

    private String normalizeImageUrl(String imageUrl) {
        String trimmed = imageUrl == null ? "" : imageUrl.trim();
        if (trimmed.contains("imgres?imgurl=") || trimmed.contains("imgurl=")) {
            String extracted = extractQueryParameter(trimmed, "imgurl");
            if (extracted != null && !extracted.isBlank()) {
                return extracted;
            }
        }
        return trimmed;
    }

    private String extractQueryParameter(String url, String key) {
        int questionMark = url.indexOf('?');
        if (questionMark < 0 || questionMark >= url.length() - 1) {
            return null;
        }

        String query = url.substring(questionMark + 1);
        for (String pair : query.split("&")) {
            int equalsIndex = pair.indexOf('=');
            if (equalsIndex <= 0) {
                continue;
            }

            String paramKey = pair.substring(0, equalsIndex);
            if (!paramKey.equals(key)) {
                continue;
            }

            return URLDecoder.decode(pair.substring(equalsIndex + 1), StandardCharsets.UTF_8);
        }

        return null;
    }

    private void ensureImageContentType(HttpResponse<byte[]> response, String imageUrl) throws Exception {
        String mimeType = extractMimeType(response, imageUrl);
        if (!mimeType.startsWith("image/")) {
            throw new Exception("L'URL fournie ne pointe pas vers une image directe. Utilisez le lien direct de l'image, pas la page Google Images.");
        }
    }

    private String extractMimeType(HttpResponse<byte[]> response, String imageUrl) {
        String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase();
        if (!contentType.isBlank()) {
            int separator = contentType.indexOf(';');
            String mimeType = separator >= 0 ? contentType.substring(0, separator) : contentType;
            mimeType = mimeType.trim();
            if (!mimeType.isBlank()) {
                return mimeType;
            }
        }
        return guessMime(imageUrl);
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
        String lowerDesire = desire == null ? "" : desire.toLowerCase();
        List<Integer> ids = rankArtworkSuggestions(lowerDesire, available);
        String intro = buildCuratorIntro(desire, lowerDesire);
        String msg = "✨ Les échos du Shadow Dimension me murmurent quelque chose... (Mode Hors-ligne 🔮)\n\n" +
                     "J'ai ressenti une résonance avec votre demande : \"" + desire + "\".\n" +
                     "Voici les fragments que j'ai pu manifester pour vous :\n\n";
        msg = intro + "\n\n" + buildCuratorBridge(desire, lowerDesire) + "\n\n";
        for (Integer id : ids) {
            Entities.Artworks.Artworks match = available.stream().filter(a -> a.getId() == id).findFirst().orElse(null);
            if (match != null) msg += "   " + buildArtworkReason(match, lowerDesire) + "\n";
            if (match != null) msg += "🌌 " + match.getTitle() + " (ID: " + match.getId() + ") 💎\n";
        }
        msg += "\n🧡 Connectez mon essence à Google AI Studio dans le fichier .env pour une vision plus profonde !";
        msg = removeLinesContaining(msg, "Google AI Studio");
        msg += "\n" + buildCuratorOutro(lowerDesire);
        return new CuratorResponse(msg, ids);
    }

    private List<Integer> rankArtworkSuggestions(String lowerDesire, List<Entities.Artworks.Artworks> available) {
        List<ArtworkScore> scores = new ArrayList<>();

        for (Entities.Artworks.Artworks artwork : available) {
            String title = artwork.getTitle() == null ? "" : artwork.getTitle().toLowerCase();
            String description = artwork.getDescription() == null ? "" : artwork.getDescription().toLowerCase();
            String haystack = title + " " + description;

            int score = 0;
            for (String token : lowerDesire.split("\\s+")) {
                if (token.isBlank()) {
                    continue;
                }
                if (title.contains(token)) {
                    score += 6;
                }
                if (description.contains(token)) {
                    score += 4;
                }
            }

            score += keywordThemeScore(lowerDesire, haystack, artwork.getCategoryID());
            if (score > 0) {
                scores.add(new ArtworkScore(artwork.getId(), score));
            }
        }

        scores.sort((a, b) -> {
            int byScore = Integer.compare(b.score, a.score);
            if (byScore != 0) {
                return byScore;
            }
            return Integer.compare(a.id, b.id);
        });

        List<Integer> ids = new ArrayList<>();
        for (ArtworkScore score : scores) {
            ids.add(score.id);
            if (ids.size() >= 3) {
                break;
            }
        }

        if (ids.isEmpty()) {
            available.stream().limit(3).forEach(a -> ids.add(a.getId()));
        }

        return ids;
    }

    private int keywordThemeScore(String desire, String haystack, int categoryId) {
        int score = 0;

        if (containsAny(desire, "fantasy", "mystic", "ethereal", "gothic", "cosmic", "dark")) {
            score += countMatches(haystack, "magic", "ghost", "spirit", "shadow", "moon", "star", "cosmic", "mystic",
                    "fantasy", "dark", "gothic", "dream", "myth", "vampire", "death", "haunted");
        }

        if (containsAny(desire, "nature")) {
            score += countMatches(haystack, "nature", "forest", "flower", "garden", "sea", "ocean", "river", "animal",
                    "bird", "tree", "mountain", "earth", "green", "sun");
        }

        if (containsAny(desire, "cyberpunk")) {
            score += countMatches(haystack, "cyber", "neon", "robot", "future", "digital", "tech", "city", "machine");
        }

        if (containsAny(desire, "love")) {
            score += countMatches(haystack, "love", "heart", "romance", "beloved", "passion", "desire");
        }

        if (containsAny(desire, "action")) {
            score += countMatches(haystack, "battle", "war", "fight", "storm", "chase", "hero", "weapon", "power");
        }

        if (containsAny(desire, "surreal", "abstract")) {
            score += countMatches(haystack, "surreal", "abstract", "dream", "symbol", "strange", "fragment", "echo");
        }

        if (containsAny(desire, "fantasy", "mystic") && categoryId == 4) {
            score += 3;
        }

        return score;
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private int countMatches(String haystack, String... keywords) {
        int score = 0;
        for (String keyword : keywords) {
            if (haystack.contains(keyword)) {
                score += 2;
            }
        }
        return score;
    }

    private String removeLinesContaining(String text, String fragment) {
        StringBuilder cleaned = new StringBuilder();
        for (String line : text.split("\\R")) {
            if (!line.contains(fragment)) {
                if (cleaned.length() > 0) {
                    cleaned.append("\n");
                }
                cleaned.append(line);
            }
        }
        return cleaned.toString();
    }

    private static class ArtworkScore {
        private final int id;
        private final int score;

        private ArtworkScore(int id, int score) {
            this.id = id;
            this.score = score;
        }
    }

    private String buildCuratorIntro(String desire, String lowerDesire) {
        String label = (desire == null || desire.isBlank()) ? "vos envies" : desire;
        if (containsAny(lowerDesire, "fantasy", "mystic", "ethereal")) {
            return "🔮 Ah... une pulsation arcane. J'ai senti votre appel pour \"" + label + "\" traverser le voile.";
        }
        if (containsAny(lowerDesire, "action")) {
            return "⚔️ Votre énergie frappe fort. Pour \"" + label + "\", j'ai invoqué des œuvres qui avancent sans hésiter.";
        }
        if (containsAny(lowerDesire, "love")) {
            return "🫀 Une résonance plus douce s'est formée. Pour \"" + label + "\", j'ai suivi les battements les plus intenses.";
        }
        if (containsAny(lowerDesire, "nature")) {
            return "🌿 Le murmure des racines et des vents m'a guidé. Pour \"" + label + "\", j'ai cueilli des fragments plus vivants.";
        }
        if (containsAny(lowerDesire, "cyberpunk")) {
            return "⚡ Les néons du Void viennent de clignoter. Pour \"" + label + "\", j'ai capté les signaux les plus électriques.";
        }
        if (containsAny(lowerDesire, "gothic", "dark")) {
            return "🕯️ Une ombre élégante s'est penchée sur votre désir. Pour \"" + label + "\", j'ai retenu les pièces les plus nocturnes.";
        }
        if (containsAny(lowerDesire, "surreal", "abstract", "cosmic")) {
            return "🌀 Votre désir ouvre des portes étranges. Pour \"" + label + "\", j'ai suivi les distorsions les plus captivantes.";
        }
        return "✨ Le Shadow Curator a perçu votre demande pour \"" + label + "\" et a remué les échos les plus proches.";
    }

    private String buildCuratorBridge(String desire, String lowerDesire) {
        if (containsAny(lowerDesire, "action")) {
            return "Voici les œuvres qui portent le plus d'élan, de tension ou de mouvement pour nourrir cette quête.";
        }
        if (containsAny(lowerDesire, "love")) {
            return "Voici les pièces qui répondent le mieux à cette recherche d'émotion, d'attachement ou de vertige intime.";
        }
        if (containsAny(lowerDesire, "nature")) {
            return "Voici les fragments où la matière, le souffle et les paysages résonnent le plus avec votre envie.";
        }
        if (containsAny(lowerDesire, "cyberpunk")) {
            return "Voici les artefacts dont les textures, les contrastes ou l'aura numérique collent le mieux à votre signal.";
        }
        if (containsAny(lowerDesire, "fantasy", "mystic", "ethereal", "gothic", "dark", "surreal", "abstract", "cosmic")) {
            return "Voici les manifestations qui portent le plus clairement cette vibration dans leur titre, leur atmosphère ou leur récit.";
        }
        return "Voici les fragments que j'ai réussi à manifester pour votre désir actuel.";
    }

    private String buildArtworkReason(Entities.Artworks.Artworks artwork, String lowerDesire) {
        String title = artwork.getTitle() == null ? "Cette œuvre" : artwork.getTitle();

        if (containsAny(lowerDesire, "action")) {
            return "⚔️ " + title + " dégage le plus de tension et de poussée narrative dans la sélection.";
        }
        if (containsAny(lowerDesire, "love")) {
            return "🫀 " + title + " garde une charge émotionnelle qui répond bien à une envie plus sensible.";
        }
        if (containsAny(lowerDesire, "nature")) {
            return "🌿 " + title + " semble le plus proche d'une énergie organique, terrestre ou contemplative.";
        }
        if (containsAny(lowerDesire, "cyberpunk")) {
            return "⚡ " + title + " possède la vibration la plus artificielle, nerveuse ou futuriste parmi ces échos.";
        }
        if (containsAny(lowerDesire, "fantasy", "mystic", "ethereal")) {
            return "🔮 " + title + " porte une aura de mythe, de rituel ou d'invisible qui colle bien à votre sélection.";
        }
        if (containsAny(lowerDesire, "gothic", "dark")) {
            return "🕯️ " + title + " conserve une intensité sombre qui renforce très bien cette ambiance.";
        }
        if (containsAny(lowerDesire, "surreal", "abstract", "cosmic")) {
            return "🌀 " + title + " laisse la place à l'étrangeté, au symbole ou à une lecture plus flottante.";
        }
        return "✨ " + title + " m'a semblé être l'un des échos les plus proches de votre demande.";
    }

    private String buildCuratorOutro(String lowerDesire) {
        if (containsAny(lowerDesire, "action")) {
            return "⚡ Si vous voulez, je peux pousser la prochaine sélection vers quelque chose d'encore plus intense ou plus brutal.";
        }
        if (containsAny(lowerDesire, "love")) {
            return "💞 Si vous voulez, je peux affiner cette piste vers quelque chose de plus tendre, plus tragique ou plus passionné.";
        }
        if (containsAny(lowerDesire, "nature")) {
            return "🌱 Si vous voulez, je peux continuer vers quelque chose de plus sauvage, plus floral ou plus paisible.";
        }
        if (containsAny(lowerDesire, "cyberpunk")) {
            return "💠 Si vous voulez, je peux accentuer le côté néon, dystopique ou mécanique de la prochaine manifestation.";
        }
        if (containsAny(lowerDesire, "fantasy", "mystic", "ethereal")) {
            return "🪄 Si vous voulez, je peux approfondir vers un registre encore plus magique, spectral ou prophétique.";
        }
        if (containsAny(lowerDesire, "gothic", "dark")) {
            return "🕸️ Si vous voulez, je peux rendre la prochaine vision encore plus obscure, cérémonielle ou troublante.";
        }
        if (containsAny(lowerDesire, "surreal", "abstract", "cosmic")) {
            return "🌌 Si vous voulez, je peux dériver vers quelque chose de plus onirique, plus abstrait ou plus cosmique.";
        }
        return "🔮 Connectez mon essence à Google AI Studio dans le fichier .env pour des visions encore plus nuancées.";
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
        String localFallback = buildLocalPdfSummary(extractedText);
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
            return localFallback;
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

    private String buildLocalPdfSummary(String extractedText) {
        String normalized = extractedText
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isEmpty()) {
            return "📖 Analyse littéraire locale :\n\nLe contenu du PDF a été détecté, mais aucun passage exploitable n'a pu être résumé.";
        }

        String[] sentences = normalized.split("(?<=[.!?])\\s+");
        List<String> selected = new ArrayList<>();
        int totalLength = 0;

        for (String sentence : sentences) {
            String cleaned = sentence.trim();
            if (cleaned.length() < 40) {
                continue;
            }
            selected.add(cleaned);
            totalLength += cleaned.length();
            if (selected.size() >= 4 || totalLength >= 700) {
                break;
            }
        }

        if (selected.isEmpty()) {
            int end = Math.min(normalized.length(), 700);
            String excerpt = normalized.substring(0, end).trim();
            if (end < normalized.length()) {
                excerpt += "...";
            }
            return "📖 Analyse littéraire locale :\n\n" + excerpt;
        }

        StringBuilder summary = new StringBuilder("📖 Analyse littéraire locale :\n\n");
        for (int i = 0; i < selected.size(); i++) {
            if (i > 0) {
                summary.append("\n\n");
            }
            summary.append(selected.get(i));
        }
        return summary.toString();
    }
}
