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
            Utils.ValidationUtils.showAlert("Erreur", "Le nom ne peut pas être vide.");
            return;
        }

        if (nom.length() <= 3) {
            Utils.ValidationUtils.showAlert("Erreur", "Le nom du type doit avoir plus de 3 caractères !");
            return;
        }

        try {
            // Duplicate Check
            for (Type existing : serviceType.getAll()) {
                if (existing.getNom().equalsIgnoreCase(nom) && existing.getId() != type.getId()) {
                    Utils.ValidationUtils.showAlert("Doublon", "Un autre type porte déjà ce nom !");
                    return;
                }
            }

            type.setNom(nom);
            serviceType.update(type);
            Utils.ValidationUtils.showSuccess("Succès", "Type mis à jour avec succès !");
            closeWindow();
        } catch (SQLException e) {
            Utils.ValidationUtils.showAlert("Erreur", "Impossible de mettre à jour : " + e.getMessage());
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


}
