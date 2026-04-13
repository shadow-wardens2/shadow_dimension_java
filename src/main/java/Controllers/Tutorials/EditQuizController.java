package Controllers.Tutorials;

import Entities.Tutorials.Quiz;
import Services.Tutorials.ServiceQuiz;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EditQuizController {

    @FXML
    private TextField tfTitre;
    @FXML
    private TextField tfOrdre;

    private ServiceQuiz serviceQuiz = new ServiceQuiz();
    private Quiz quiz;

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        tfTitre.setText(quiz.getTitre());
        tfOrdre.setText(String.valueOf(quiz.getOrdre()));
    }

    @FXML
    private void sauvegarder() {
        String titre = tfTitre.getText().trim();
        String ordreStr = tfOrdre.getText().trim();

        if (titre.isEmpty() || ordreStr.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Les champs obligatoires ne peuvent pas être vides.");
            return;
        }

        int ordre;
        try {
            ordre = Integer.parseInt(ordreStr);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "L'ordre doit être un nombre valide.");
            return;
        }

        quiz.setOrdre(ordre);
        quiz.setTitre(titre);

        try {
            serviceQuiz.update(quiz);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Quiz mis à jour avec succès !");
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour le quiz.");
        }
    }

    @FXML
    private void annuler() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) tfTitre.getScene().getWindow();
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
