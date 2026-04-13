package Controllers.Tutorials;

import Entities.Tutorials.Question;
import Services.Tutorials.ServiceQuestion;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EditQuestionController {

    @FXML
    private TextArea taTexte;

    private ServiceQuestion serviceQuestion = new ServiceQuestion();
    private Question question;

    public void setQuestion(Question question) {
        this.question = question;
        taTexte.setText(question.getTexte());
    }

    @FXML
    private void sauvegarder() {
        String texte = taTexte.getText().trim();

        if (texte.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le texte ne peut pas être vide.");
            return;
        }

        question.setTexte(texte);

        try {
            serviceQuestion.update(question);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Question mise à jour avec succès !");
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour la question.");
        }
    }

    @FXML
    private void annuler() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) taTexte.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
