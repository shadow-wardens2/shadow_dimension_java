package Controllers.Marketplace;

import Entities.Marketplace.Categorie;
import Services.Marketplace.ServiceCategorie;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EditCategorieController {

    @FXML
    private TextField tfNom;
    @FXML
    private TextField tfDescription;

    private ServiceCategorie serviceCategorie = new ServiceCategorie();
    private Categorie categorie;

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
        tfNom.setText(categorie.getNom());
        tfDescription.setText(categorie.getDescription());
    }

    @FXML
    private void sauvegarder() {
        String nom = tfNom.getText().trim();
        String description = tfDescription.getText() != null ? tfDescription.getText().trim() : "";

        if (nom.isEmpty()) {
            showAlert("Erreur", "Le nom ne peut pas être vide.");
            return;
        }

        categorie.setNom(nom);
        categorie.setDescription(description);

        try {
            serviceCategorie.update(categorie);
            showAlert("Succès", "Catégorie mise à jour avec succès !");
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
