package Controllers.Tutorials;

import Entities.Tutorials.Question;
import Entities.Tutorials.Quiz;
import Services.Tutorials.ServiceQuestion;
import Services.Tutorials.ServiceQuiz;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class AjouterQuestionController {

    @FXML
    private TextArea taTexte;

    @FXML
    private ComboBox<Quiz> cbQuiz;

    @FXML
    private Label lbError;

    private ServiceQuestion serviceQuestion;
    private ServiceQuiz serviceQuiz;

    public AjouterQuestionController() {
        serviceQuestion = new ServiceQuestion();
        serviceQuiz = new ServiceQuiz();
    }

    @FXML
    public void initialize() {
        lbError.setText("");
        try {
            List<Quiz> quizzes = serviceQuiz.getAll();
            cbQuiz.setItems(FXCollections.observableArrayList(quizzes));
        } catch (SQLException e) {
            e.printStackTrace();
            lbError.setText("Erreur: Impossible de charger les quizzes.");
        }
    }

    public void setPreselectedQuiz(Quiz quiz) {
        cbQuiz.setValue(quiz);
    }

    @FXML
    private void ajouterQuestion() {
        String texte = taTexte.getText().trim();
        Quiz quiz = cbQuiz.getSelectionModel().getSelectedItem();

        if (texte.isEmpty() || quiz == null) {
            lbError.setText("Erreur: Le texte et le quiz sont obligatoires !");
            return;
        }

        try {
            Question question = new Question(0, texte, quiz);
            serviceQuestion.add(question);
            fermerFenetre();
        } catch (Exception e) {
            e.printStackTrace();
            lbError.setText("Erreur: Impossible d'ajouter la question.");
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
}
