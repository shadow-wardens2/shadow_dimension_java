package Controllers.Marketplace;

import Entities.Marketplace.Type;
import Services.Marketplace.ServiceType;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EditTypeController {

    @FXML
    private TextField tfNom;

    private ServiceType serviceType = new ServiceType();
    private Type type;

    public void setType(Type type) {
        this.type = type;
        tfNom.setText(type.getNom());
    }

    @FXML
    private void sauvegarder() {
        String nom = tfNom.getText().trim();
        if (nom.isEmpty()) {
            showAlert("Erreur", "Le nom ne peut pas être vide.");
            return;
        }

        type.setNom(nom);

        try {
            serviceType.update(type);
            showAlert("Succès", "Type mis à jour avec succès !");
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de mettre à jour : " + e.getMessage());
        }
    }

    @FXML
    private void annuler() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) tfNom.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
