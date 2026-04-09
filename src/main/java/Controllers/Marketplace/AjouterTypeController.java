package Controllers.Marketplace;

import Interfaces.InterfaceServiceProduit;
import Services.Marketplace.ServiceType;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import Entities.Marketplace.Type; // your Type model class
import java.sql.SQLException;

public class AjouterTypeController {

    @FXML
    private TextField tfNomType;

    // Initialize your service
    private InterfaceServiceProduit<Type> typeService = new ServiceType();

    // Called when user clicks "Ajouter"
    @FXML
    private void handleAjouterType() {
        String nom = tfNomType.getText().trim();

        if (nom.isEmpty()) {
            showAlert("Erreur", "Le nom du type ne peut pas être vide.");
            return;
        }

        Type type = new Type();
        type.setNom(nom);

        try {
            typeService.add(type);
            showAlert("Succès", "Type ajouté avec succès !");
            // Close the window
            Stage stage = (Stage) tfNomType.getScene().getWindow();
            stage.close();
        } catch (SQLException e) {
            showAlert("Erreur SQL", e.getMessage());
        }
    }

    // Called when user clicks "Annuler"
    @FXML
    private void handleAnnuler() {
        Stage stage = (Stage) tfNomType.getScene().getWindow();
        stage.close();
    }

    // Helper method to show alert
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}