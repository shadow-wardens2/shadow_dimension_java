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

                if (fullResponse.contains("\n")) {
                    String[] parts = fullResponse.split("\n", 2);
                    lbRecommendationTitle.setText(parts[0].trim().replace("[", "").replace("]", ""));
                    lbReasoning.setText(parts[1].trim());
                } else {
                    lbRecommendationTitle.setText("A Path Emerges");
                    lbReasoning.setText(fullResponse);
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
