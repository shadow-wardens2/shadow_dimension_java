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
    @FXML
    private javafx.scene.control.Label lblError;

    // Initialize your service
    private InterfaceServiceProduit<Type> typeService = new ServiceType();

    // Called when user clicks "Ajouter"
    @FXML
    private void handleAjouterType() {
        lblError.setVisible(false);
        String nom = tfNomType.getText().trim();

        if (nom.isEmpty()) {
            showError("Le nom du type ne peut pas être vide.");
            return;
        }

        if (nom.length() <= 3) {
            showError("Le nom du type doit avoir plus de 3 caractères !");
            return;
        }

        try {
            // Duplicate Check
            for (Type existing : ((Services.Marketplace.ServiceType)typeService).getAll()) {
                if (existing.getNom().equalsIgnoreCase(nom)) {
                    showError("Ce type existe déjà !");
                    return;
                }
            }

            Type type = new Type();
            type.setNom(nom);
            typeService.add(type);
            Utils.ValidationUtils.showSuccess("Succès", "Type ajouté avec succès !");
            Stage stage = (Stage) tfNomType.getScene().getWindow();
            stage.close();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
    }

    // Called when user clicks "Annuler"
    @FXML
    private void handleAnnuler() {
        Stage stage = (Stage) tfNomType.getScene().getWindow();
        stage.close();
    }

    // Helper method to show alert

}