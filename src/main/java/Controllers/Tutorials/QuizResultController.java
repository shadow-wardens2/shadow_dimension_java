package Controllers.Tutorials;

import Entities.Tutorials.Quiz;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;

import java.io.IOException;

public class QuizResultController {

    @FXML
    private Label lbScore;
    @FXML
    private Label lbMessage;
    @FXML
    private Label lbPercent;

    private Quiz quiz;
    private Parent previousView;
    private FormationDetailsController detailsController;

    public void setResults(int score, int total, Quiz quiz, Parent previousView,
            FormationDetailsController detailsController, java.util.List<String> failedQuestions) {
        this.quiz = quiz;
        this.previousView = previousView;
        this.detailsController = detailsController;

        lbScore.setText(score + " / " + total);

        double percent = total > 0 ? ((double) score / total) * 100 : 0;
        lbPercent.setText(String.format("%.1f%% Accuracy", percent));

        lbMessage.setText("The Oracle is analyzing your trial...");
        
        new Thread(() -> {
            Services.Tutorials.AiQuizService aiService = new Services.Tutorials.AiQuizService();
            String feedback = aiService.getQuizFeedback(quiz.getTitre(), score, total, failedQuestions);
            javafx.application.Platform.runLater(() -> {
                lbMessage.setText(feedback);
            });
        }).start();
    }

    @FXML
    private void handleRestart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/QuizPlay.fxml"));
            Parent root = loader.load();

            QuizPlayController controller = loader.getController();
            controller.startQuiz(quiz, previousView, detailsController);

            lbScore.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFinish() {
        if (detailsController != null) {
            detailsController.refresh();
        }

        if (previousView != null) {
            lbScore.getScene().setRoot(previousView);
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/TutorialsFront.fxml"));
                lbScore.getScene().setRoot(loader.load());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
