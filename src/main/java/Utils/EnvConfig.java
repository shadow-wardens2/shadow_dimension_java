package Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EnvConfig {
    private static final Map<String, String> env = new HashMap<>();

    static {
        loadEnv();
    }

    private static void loadEnv() {
        try (BufferedReader br = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx != -1) {
                    String key = line.substring(0, eqIdx).trim();
                    String value = line.substring(eqIdx + 1).trim();
                    env.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load .env file. " + e.getMessage());
        }
    }

    public static String get(String key) {
        return env.get(key);
    }

    public static String get(String key, String defaultValue) {
        return env.getOrDefault(key, defaultValue);
    }
}
