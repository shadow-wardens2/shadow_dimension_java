package Controllers;

import Entities.Categorie;
import Services.ServiceCategorie;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class AjouterCategorieController {

    @FXML
    private TextField tfNomCategorie;

    @FXML
    private TextField tfDescription;

    private ServiceCategorie serviceCategorie;

    public AjouterCategorieController() {
        serviceCategorie = new ServiceCategorie(); // initialize your service
    }

    @FXML
    private void ajouterCategorie() {
        String nom = tfNomCategorie.getText().trim();
        String description = tfDescription.getText() != null ? tfDescription.getText().trim() : "";
        if (nom.isEmpty()) {
            showAlert("Erreur", "Le nom de la catégorie ne peut pas être vide !");
            return;
        }

        try {
            Categorie c = new Categorie(0, nom, description); // assuming ID is auto-generated
            serviceCategorie.add(c);
            showAlert("Succès", "Catégorie ajoutée avec succès !");
            javafx.stage.Stage stage = (javafx.stage.Stage) tfNomCategorie.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ajouter la catégorie.");
        }
    }

    @FXML
    private void annuler() {
        javafx.stage.Stage stage = (javafx.stage.Stage) tfNomCategorie.getScene().getWindow();
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