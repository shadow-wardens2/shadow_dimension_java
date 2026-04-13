package Controllers.Tutorials;

import Entities.Tutorials.Formation;
import Services.Tutorials.ServiceFormation;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EditFormationController {

    @FXML
    private TextField tfTitre;
    @FXML
    private TextField tfDescription;
    @FXML
    private ComboBox<String> cbNiveau;
    @FXML
    private TextField tfImage;

    private ServiceFormation serviceFormation = new ServiceFormation();
    private Formation formation;

    @FXML
    public void initialize() {
        cbNiveau.setItems(FXCollections.observableArrayList("debutant", "intermediaire", "avance"));
    }

    public void setFormation(Formation formation) {
        this.formation = formation;
        tfTitre.setText(formation.getTitre());
        tfDescription.setText(formation.getDescription());
        cbNiveau.setValue(formation.getNiveau());
        tfImage.setText(formation.getImage());
    }

    @FXML
    private void sauvegarder() {
        String titre = tfTitre.getText().trim();
        String description = tfDescription.getText() != null ? tfDescription.getText().trim() : "";
        String niveau = cbNiveau.getValue();
        String image = tfImage.getText() != null ? tfImage.getText().trim() : "";

        if (titre.isEmpty() || niveau == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le titre et le niveau sont obligatoires.");
            return;
        }

        try {
            boolean exists = serviceFormation.getAll().stream()
                    .anyMatch(f -> f.getTitre().equalsIgnoreCase(titre) && f.getId() != formation.getId());
            if (exists) {
                showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Une formation avec ce titre existe déjà !");
                return;
            }

            formation.setTitre(titre);
            formation.setDescription(description);
            formation.setNiveau(niveau);
            formation.setImage(image);

            serviceFormation.update(formation);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Formation mise à jour avec succès !");
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour la formation.");
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
