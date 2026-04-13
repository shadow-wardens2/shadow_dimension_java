package Controllers.Tutorials;

import Entities.Tutorials.Formation;
import Entities.Tutorials.Quiz;
import Services.Tutorials.ServiceFormation;
import Services.Tutorials.ServiceQuiz;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class AjouterQuizController {

    @FXML
    private TextField tfTitre;

    @FXML
    private TextField tfOrdre;

    @FXML
    private ComboBox<Formation> cbFormation;

    private ServiceQuiz serviceQuiz;
    private ServiceFormation serviceFormation;

    public AjouterQuizController() {
        serviceQuiz = new ServiceQuiz();
        serviceFormation = new ServiceFormation();
    }

    @FXML
    public void initialize() {
        try {
            List<Formation> formations = serviceFormation.getAll();
            cbFormation.setItems(FXCollections.observableArrayList(formations));
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les formations.");
        }
    }

    @FXML
    private void ajouterQuiz() {
        String titre = tfTitre.getText().trim();
        Formation formation = cbFormation.getSelectionModel().getSelectedItem();

        String ordreStr = tfOrdre.getText().trim();
        int ordre;
        try {
            ordre = Integer.parseInt(ordreStr);
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "L'ordre doit être un nombre valide.");
            return;
        }

        if (titre.isEmpty() || formation == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Le titre et la formation sont obligatoires !");
            return;
        }

        try {
            Quiz quiz = new Quiz(0, titre, ordre, formation);
            serviceQuiz.add(quiz);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Le quiz a été ajouté avec succès !");
            fermerFenetre();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter le quiz.");
        }
    }

    @FXML
    private void annuler() {
        fermerFenetre();
    }

    private void fermerFenetre() {
        Stage stage = (Stage) tfTitre.getScene().getWindow();
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
