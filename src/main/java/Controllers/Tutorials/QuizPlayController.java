package Controllers.Tutorials;

import Entities.Tutorials.Option;
import Entities.Tutorials.Question;
import Entities.Tutorials.Quiz;
import Services.Tutorials.ServiceOption;
import Services.Tutorials.ServiceQuestion;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QuizPlayController {

    @FXML
    private Label lbQuizTitle;
    @FXML
    private Label lbQuestionCounter;
    @FXML
    private Label lbQuestionText;
    @FXML
    private VBox vbOptionsContainer;
    @FXML
    private ProgressBar pbProgress;
    @FXML
    private Button btnNext;

    private Quiz currentQuiz;
    private List<Question> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int score = 0;
    private Parent previousView;
    private FormationDetailsController detailsController;
    private List<String> failedQuestions = new ArrayList<>();

    private ServiceQuestion serviceQuestion = new ServiceQuestion();
    private ServiceOption serviceOption = new ServiceOption();

    private Button selectedOptionButton = null;
    private boolean isCorrectSelected = false;

    public void startQuiz(Quiz quiz, Parent previousView, FormationDetailsController detailsController) {
        this.currentQuiz = quiz;
        this.previousView = previousView;
        this.detailsController = detailsController;
        lbQuizTitle.setText(quiz.getTitre());

        failedQuestions.clear();
        loadQuestions();
        showQuestion();
    }

    private void loadQuestions() {
        try {
            questions = serviceQuestion.getQuestionsByQuiz(currentQuiz.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showQuestion() {
        if (questions.isEmpty())
            return;

        Question q = questions.get(currentQuestionIndex);
        lbQuestionText.setText(q.getTexte());
        lbQuestionCounter.setText("Q " + (currentQuestionIndex + 1) + " / " + questions.size());
        pbProgress.setProgress((double) currentQuestionIndex / questions.size());

        vbOptionsContainer.getChildren().clear();
        btnNext.setDisable(true);
        selectedOptionButton = null;

        try {
            List<Option> options = serviceOption.getOptionsByQuestion(q.getId());
            for (Option o : options) {
                Button btn = new Button(o.getTexte());
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.getStyleClass().add("option-button");
                btn.setStyle(
                        "-fx-background-color: #1a1a24; -fx-text-fill: white; -fx-padding: 15; -fx-background-radius: 10; -fx-cursor: hand;");

                btn.setOnAction(e -> {
                    if (selectedOptionButton != null) {
                        selectedOptionButton.setStyle(
                                "-fx-background-color: #1a1a24; -fx-text-fill: white; -fx-padding: 15; -fx-background-radius: 10;");
                    }
                    selectedOptionButton = btn;
                    selectedOptionButton.setStyle(
                            "-fx-background-color: #6d28d9; -fx-text-fill: white; -fx-padding: 15; -fx-background-radius: 10; -fx-border-color: #a78bfa; -fx-border-radius: 10;");
                    isCorrectSelected = o.isEstCorrecte();
                    btnNext.setDisable(false);
                });

                vbOptionsContainer.getChildren().add(btn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNext() {
        if (isCorrectSelected) {
            score++;
        } else {
            failedQuestions.add(questions.get(currentQuestionIndex).getTexte());
        }

        currentQuestionIndex++;
        if (currentQuestionIndex < questions.size()) {
            showQuestion();
        } else {
            showResults();
        }
    }

    private void showResults() {
        // Save result to user_quiz_result
        int userId = (Utils.SessionManager.getCurrentUser() != null) ? Utils.SessionManager.getCurrentUser().getId()
                : -1;
        if (userId != -1) {
            double percent = questions.size() > 0 ? ((double) score / questions.size()) * 100 : 0;
            boolean isPassed = percent >= 80;
            new Services.Tutorials.ServiceQuizProgress().markAsCompleted(userId, currentQuiz.getId(), score, isPassed);
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/QuizResult.fxml"));
            Parent root = loader.load();

            QuizResultController controller = loader.getController();
            controller.setResults(score, questions.size(), currentQuiz, previousView, detailsController, failedQuestions);

            lbQuizTitle.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExit() {
        if (previousView != null) {
            lbQuizTitle.getScene().setRoot(previousView);
        }
    }
}
