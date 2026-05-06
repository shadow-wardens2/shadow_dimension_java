package Services.Forum;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GiphyService {

    private static final String API_KEY = "bkddeDiufbw1ileM08Raur6XV56LXwUY";
    private static final String BASE_URL = "https://api.giphy.com/v1/gifs";

    public List<String> getTrendingGifs(int limit) throws Exception {
        String endpoint = BASE_URL + "/trending?api_key=" + API_KEY + "&limit=" + limit + "&rating=g";
        return fetchGifs(endpoint);
    }

    public List<String> searchGifs(String query, int limit) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String endpoint = BASE_URL + "/search?api_key=" + API_KEY + "&q=" + encodedQuery + "&limit=" + limit + "&rating=g";
        return fetchGifs(endpoint);
    }

    private List<String> fetchGifs(String endpointUrl) throws Exception {
        URL url = new URL(endpointUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new Exception("Giphy API returned HTTP error: " + conn.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        conn.disconnect();

        List<String> gifUrls = new ArrayList<>();
        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONArray dataArray = jsonResponse.getJSONArray("data");

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject gifObject = dataArray.getJSONObject(i);
            JSONObject images = gifObject.getJSONObject("images");
            // Use fixed_height which is usually good for UI grids and performance
            JSONObject fixedHeight = images.getJSONObject("fixed_height");
            String gifUrl = fixedHeight.getString("url");
            gifUrls.add(gifUrl);
        }

        return gifUrls;
    }
}
