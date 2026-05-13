package Controllers.Tutorials;

import Services.Tutorials.FormationAiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class FormationRecommendationsController {

    @FXML
    private VBox vbLoading;
    @FXML
    private VBox vbResult;
    @FXML
    private Label lbRecommendationTitle;
    @FXML
    private Label lbReasoning;

    private FormationAiService aiService = new FormationAiService();

    @FXML
    public void initialize() {
        fetchRecommendation();
    }

    private void fetchRecommendation() {
        new Thread(() -> {
            String fullResponse = aiService.getPersonalizedRecommendation();

            Platform.runLater(() -> {
                vbLoading.setVisible(false);
                vbResult.setVisible(true);

                // Handle multiple recommendations separated by |||
                String[] recommendations = fullResponse.split("\\|\\|\\|");
                String firstRec = recommendations[0].trim();

                if (firstRec.contains("|")) {
                    String[] parts = firstRec.split("\\|");
                    String title = parts[0].replace("TITLE:", "").replace("AI RESPONSE:", "").trim();
                    String reason = parts[1].replace("REASON:", "").trim();

                    lbRecommendationTitle.setText(title.replace("[", "").replace("]", ""));
                    lbReasoning.setText(reason);
                } else {
                    lbRecommendationTitle.setText("A Path Emerges");
                    lbReasoning.setText(firstRec);
                }
            });
        }).start();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) vbResult.getScene().getWindow();
        stage.close();
    }
}
