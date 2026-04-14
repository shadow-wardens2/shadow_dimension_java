package Services.User;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class GoogleOAuthService {

    private static final String REDIRECT_URI = "http://127.0.0.1:8765";
    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://openidconnect.googleapis.com/v1/userinfo";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public GoogleProfile authenticate() throws Exception {
        String clientId = resolveClientId();
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        String state = generateState();

        String authUrl = buildAuthUrl(clientId, codeChallenge, state);
        String code = openBrowserAndWaitForCode(authUrl, state);
        String accessToken = exchangeCodeForToken(clientId, code, codeVerifier);
        return fetchUserProfile(accessToken);
    }

    private String buildAuthUrl(String clientId, String codeChallenge, String state) {
        return AUTH_ENDPOINT
                + "?response_type=code"
                + "&client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(REDIRECT_URI)
                + "&scope=" + encode("openid email profile")
                + "&code_challenge=" + encode(codeChallenge)
                + "&code_challenge_method=S256"
                + "&state=" + encode(state);
    }

    private String openBrowserAndWaitForCode(String authUrl, String expectedState) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> codeRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();
        AtomicReference<String> stateRef = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(8765), 0);
        server.createContext("/", exchange -> {
            try {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
                if (params.containsKey("error")) {
                    errorRef.set(params.get("error"));
                } else {
                    codeRef.set(params.get("code"));
                    stateRef.set(params.get("state"));
                }

                String html = "<html><body><h3>Google authentication complete.</h3><p>You can close this window.</p></body></html>";
                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } finally {
                latch.countDown();
            }
        });
        server.start();

        if (!Desktop.isDesktopSupported()) {
            server.stop(0);
            throw new IllegalStateException("Desktop browser is not supported on this machine.");
        }

        Desktop.getDesktop().browse(URI.create(authUrl));

        boolean received = latch.await(180, TimeUnit.SECONDS);
        server.stop(0);

        if (!received) {
            throw new IllegalStateException("Google authorization timeout.");
        }
        if (errorRef.get() != null) {
            throw new IllegalStateException("Google authorization failed: " + errorRef.get());
        }
        if (stateRef.get() == null || !stateRef.get().equals(expectedState)) {
            throw new IllegalStateException("Invalid OAuth state. Please try again.");
        }
        if (codeRef.get() == null || codeRef.get().isBlank()) {
            throw new IllegalStateException("Authorization code not received.");
        }

        return codeRef.get();
    }

    private String exchangeCodeForToken(String clientId, String code, String codeVerifier) throws IOException, InterruptedException {
        String clientSecret = resolveOptionalClientSecret();

        String body = "code=" + encode(code)
                + "&client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(REDIRECT_URI)
                + "&code_verifier=" + encode(codeVerifier)
                + "&grant_type=authorization_code";

        if (clientSecret != null && !clientSecret.isBlank()) {
            body += "&client_secret=" + encode(clientSecret);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_ENDPOINT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            String details = response.body();
            if (details.contains("client_secret is missing")) {
                throw new IllegalStateException(
                        "Token exchange failed: client_secret is missing. "
                                + "Your OAuth client is likely Web Application. "
                                + "Set GOOGLE_CLIENT_SECRET or switch to Desktop App credential. "
                                + "Google response: " + details
                );
            }
            throw new IllegalStateException("Token exchange failed: " + details);
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!json.has("access_token")) {
            throw new IllegalStateException("Google token response missing access_token.");
        }

        return json.get("access_token").getAsString();
    }

    private GoogleProfile fetchUserProfile(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USERINFO_ENDPOINT))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("Failed to fetch Google profile: " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        String sub = getJsonString(json, "sub");
        String email = getJsonString(json, "email");
        String name = getJsonString(json, "name");

        if (sub.isBlank() || email.isBlank()) {
            throw new IllegalStateException("Google profile is missing sub/email.");
        }

        return new GoogleProfile(sub, email, name);
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return map;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String key = decode(kv[0]);
            String value = kv.length > 1 ? decode(kv[1]) : "";
            map.put(key, value);
        }
        return map;
    }

    private String getenvRequired(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + key);
        }
        return value;
    }

    private String resolveClientId() {
        String fromEnv = System.getenv("GOOGLE_CLIENT_ID");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }

        String fromSystemProperty = System.getProperty("google.client.id");
        if (fromSystemProperty != null && !fromSystemProperty.isBlank()) {
            return fromSystemProperty;
        }

        throw new IllegalStateException("Missing GOOGLE_CLIENT_ID. Set env var GOOGLE_CLIENT_ID or JVM option -Dgoogle.client.id=...");
    }

    private String resolveOptionalClientSecret() {
        String fromEnv = System.getenv("GOOGLE_CLIENT_SECRET");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }

        String fromSystemProperty = System.getProperty("google.client.secret");
        if (fromSystemProperty != null && !fromSystemProperty.isBlank()) {
            return fromSystemProperty;
        }

        return null;
    }

    private String getJsonString(JsonObject json, String key) {
        return (json.has(key) && !json.get(key).isJsonNull()) ? json.get(key).getAsString() : "";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String generateCodeVerifier() {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate PKCE code challenge.", e);
        }
    }

    private String generateState() {
        byte[] random = new byte[16];
        new SecureRandom().nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    public record GoogleProfile(String googleId, String email, String fullName) {
    }
}
