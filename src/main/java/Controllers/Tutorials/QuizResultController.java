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
            FormationDetailsController detailsController) {
        this.quiz = quiz;
        this.previousView = previousView;
        this.detailsController = detailsController;

        lbScore.setText(score + " / " + total);

        double percent = total > 0 ? ((double) score / total) * 100 : 0;
        lbPercent.setText(String.format("%.1f%% Accuracy", percent));

        if (percent >= 80) {
            lbMessage.setText("Exceptional work! You are becoming a master of these arts.");
        } else if (percent >= 50) {
            lbMessage.setText("Good progress. A few more trials and you will achieve perfection.");
        } else {
            lbMessage.setText("The path is long and difficult. Do not be discouraged, try again.");
        }
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
