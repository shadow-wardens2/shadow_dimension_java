package Controllers.Tutorials;

import Entities.Tutorials.Question;
import Entities.Tutorials.Quiz;
import Services.Tutorials.ServiceQuestion;
import Services.Tutorials.ServiceQuiz;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class AjouterQuestionController {

    @FXML
    private TextArea taTexte;

    @FXML
    private ComboBox<Quiz> cbQuiz;

    private ServiceQuestion serviceQuestion;
    private ServiceQuiz serviceQuiz;

    public AjouterQuestionController() {
        serviceQuestion = new ServiceQuestion();
        serviceQuiz = new ServiceQuiz();
    }

    @FXML
    public void initialize() {
        try {
            List<Quiz> quizzes = serviceQuiz.getAll();
            cbQuiz.setItems(FXCollections.observableArrayList(quizzes));
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les quizzes.");
        }
    }

    public void setPreselectedQuiz(Quiz quiz) {
        if (quiz != null && cbQuiz.getItems() != null) {
            for (Quiz q : cbQuiz.getItems()) {
                if (q.getId() == quiz.getId()) {
                    cbQuiz.getSelectionModel().select(q);
                    break;
                }
            }
        }
    }

    @FXML
    private void ajouterQuestion() {
        String texte = taTexte.getText().trim();
        Quiz quiz = cbQuiz.getSelectionModel().getSelectedItem();

        if (texte.isEmpty() || quiz == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Le texte et le quiz sont obligatoires !");
            return;
        }

        try {
            Question question = new Question(0, texte, quiz);
            serviceQuestion.add(question);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "La question a été ajoutée avec succès !");
            fermerFenetre();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter la question.");
        }
    }

    @FXML
    private void annuler() {
        fermerFenetre();
    }

    private void fermerFenetre() {
        Stage stage = (Stage) taTexte.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
