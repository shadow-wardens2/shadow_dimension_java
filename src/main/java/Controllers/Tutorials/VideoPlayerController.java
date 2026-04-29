package Controllers.Tutorials;

import Entities.Tutorials.Lecon;
import Utils.YoutubeApiManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class VideoPlayerController implements Initializable {

    @FXML
    private WebView webView;

    @FXML
    private Label lbLessonTitle;

    private Lecon currentLecon;
    private Parent previousView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set a standard User-Agent to avoid YouTube "Erreur 153"
        webView.getEngine().setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        webView.getEngine().setJavaScriptEnabled(true);

        // Add listener for debugging
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.FAILED) {
                System.err.println("DEBUG: WebView failed to load URL: " + webView.getEngine().getLocation());
            }
            System.out.println("DEBUG: WebView State: " + newState);
        });
    }

    public void loadVideo(Lecon lecon, Parent previousView) {
        this.currentLecon = lecon;
        this.previousView = previousView;
        lbLessonTitle.setText(lecon.getTitre());

        String videoUrl = lecon.getVideoUrl();
        System.out.println("DEBUG: Loading video from URL: " + videoUrl);

        String videoId = YoutubeApiManager.extractVideoId(videoUrl);
        System.out.println("DEBUG: Extracted Video ID: " + videoId);

        if (videoId != null && !videoId.isEmpty()) {
            String embedUrl = "https://www.youtube.com/embed/" + videoId + "?rel=0&enablejsapi=1";
            System.out.println("DEBUG: Final Embed URL: " + embedUrl);
            webView.getEngine().load(embedUrl);

            webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldV, newV) -> {
                if (newV == javafx.concurrent.Worker.State.SUCCEEDED) {
                    System.out.println("DEBUG: Successfully loaded: " + webView.getEngine().getLocation());
                }
            });
        } else {
            System.err.println("DEBUG: FAILED to extract Video ID from: " + videoUrl);
            lbLessonTitle.setText("Invalid Video URL Archive");
        }
    }

    @FXML
    void handleBack() {
        webView.getEngine().load("about:blank"); // Stop video
        if (previousView != null) {
            webView.getScene().setRoot(previousView);
        } else {
            // Fallback to tutorials front
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/TutorialsFront.fxml"));
                webView.getScene().setRoot(loader.load());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
