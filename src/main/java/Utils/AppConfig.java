package Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AppConfig {

    private static volatile boolean dotEnvLoaded = false;

    private AppConfig() {
    }

    // Loads key=value entries from .env into JVM properties (dev convenience).
    public static synchronized void loadDotEnv() {
        if (dotEnvLoaded) {
            return;
        }

        Path dotEnvPath = findDotEnvPath();
        if (dotEnvPath == null) {
            dotEnvLoaded = true;
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(dotEnvPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int separatorIndex = trimmed.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }

                String key = trimmed.substring(0, separatorIndex).trim();
                String value = trimmed.substring(separatorIndex + 1).trim();

                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }

                if (key.isEmpty()) {
                    continue;
                }

                // Keep .env deterministic for local development: it overrides inherited process env.
                System.setProperty(key, value);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read .env file: " + e.getMessage(), e);
        }

        dotEnvLoaded = true;
    }

    private static Path findDotEnvPath() {
        Path start = Paths.get("").toAbsolutePath().normalize();
        Path current = start;

        while (current != null) {
            Path candidate = current.resolve(".env");
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }

        return null;
    }

    public static String get(String key) {
        String fromProperty = System.getProperty(key);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }

        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }

        return null;
    }

    public static String required(String key) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing configuration key: " + key);
        }
        return value;
    }

    public static String getOrDefault(String key, String defaultValue) {
        String value = get(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
