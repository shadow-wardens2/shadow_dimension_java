package Services.Marketplace;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import Utils.EnvConfig;

public class CurrencyConverterService {
    // --- CONFIGURATION ---
    // Get your free API key at: https://www.exchangerate-api.com/
    private static final String API_KEY = EnvConfig.get("EXCHANGERATE_API_KEY");
    private static final String BASE_CURRENCY = "TND";
    private static final String API_URL_TEMPLATE = "https://v6.exchangerate-api.com/v6/%s/latest/%s";
    private static final long CACHE_TTL_MS = 3600 * 1000; // 1 hour

    // --- STATE ---
    private static Map<String, Double> cachedRates = new HashMap<>();
    private static long lastFetchedTime = 0;
    private static String currentCurrency = BASE_CURRENCY;

    // --- METHODS ---

    /**
     * Fetches exchange rates from the API or returns cached ones if still valid.
     */
    public static Map<String, Double> getExchangeRates() {
        long currentTime = System.currentTimeMillis();
        
        // Return cache if it's still fresh
        if (!cachedRates.isEmpty() && (currentTime - lastFetchedTime < CACHE_TTL_MS)) {
            return cachedRates;
        }

        try {
            String urlString = String.format(API_URL_TEMPLATE, API_KEY, BASE_CURRENCY);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                if ("success".equals(jsonResponse.getString("result"))) {
                    JSONObject ratesJson = jsonResponse.getJSONObject("conversion_rates");
                    Map<String, Double> newRates = new HashMap<>();
                    for (String key : ratesJson.keySet()) {
                        newRates.put(key, ratesJson.getDouble(key));
                    }
                    
                    cachedRates = newRates;
                    lastFetchedTime = currentTime;
                    return cachedRates;
                }
            }
            
            System.err.println("Currency API Error: Response code " + responseCode);
        } catch (Exception e) {
            System.err.println("Currency API Request failed: " + e.getMessage());
        }

        // Fallback: If API fails, return current cache (if any) or a default basic map
        if (cachedRates.isEmpty()) {
            cachedRates.put("TND", 1.0);
            cachedRates.put("EUR", 0.3); // Approximate fallback
            cachedRates.put("USD", 0.32); // Approximate fallback
        }
        return cachedRates;
    }

    /**
     * Converts an amount from TND to the target currency.
     */
    public static double convert(double amount, String toCurrency) {
        if (toCurrency == null) toCurrency = currentCurrency;
        if (BASE_CURRENCY.equals(toCurrency)) {
            return amount;
        }

        Map<String, Double> rates = getExchangeRates();
        double rate = rates.getOrDefault(toCurrency, 1.0);

        return amount * rate;
    }

    public static String getCurrentCurrency() {
        return currentCurrency;
    }

    public static void setCurrentCurrency(String currency) {
        // You can add more supported currencies here
        if (currency.equals("EUR") || currency.equals("USD") || currency.equals("TND")) {
            currentCurrency = currency;
        }
    }

    public static String getCurrencySymbol(String currency) {
        if (currency == null) currency = currentCurrency;
        
        return switch (currency) {
            case "EUR" -> "€";
            case "USD" -> "$";
            case "TND" -> "TND";
            default -> currency;
        };
    }

    public static String getBaseCurrency() {
        return BASE_CURRENCY;
    }
}
