package Controllers.Marketplace.Back;

import Entities.Marketplace.Categorie;
import Services.Marketplace.ServiceCategorie;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class AjouterCategorieController {

    @FXML
    private TextField tfNomCategorie;

    @FXML
    private TextField tfDescription;

    @FXML
    private javafx.scene.control.Label errorLabel;

    private ServiceCategorie serviceCategorie;

    public AjouterCategorieController() {
        serviceCategorie = new ServiceCategorie(); // initialize your service
    }

    @FXML
    private void ajouterCategorie() {
        String nom = tfNomCategorie.getText().trim();
        String description = tfDescription.getText() != null ? tfDescription.getText().trim() : "";

        // Clear previous error
        errorLabel.setVisible(false);
        errorLabel.setText("");

        if (nom.isEmpty()) {
            showError("Le nom de la catégorie ne peut pas être vide !");
            return;
        }

        if (nom.length() <= 3) {
            showError("Le nom de la catégorie doit avoir plus de 3 caractères !");
            return;
        }

        try {
            // Duplicate Check
            for (Categorie existing : serviceCategorie.getAll()) {
                if (existing.getNom().equalsIgnoreCase(nom)) {
                    showError("Cette catégorie existe déjà !");
                    return;
                }
            }

            Categorie c = new Categorie(0, nom, description);
            serviceCategorie.add(c);
            Utils.ValidationUtils.showSuccess("Succès", "Catégorie ajoutée avec succès !");
            javafx.stage.Stage stage = (javafx.stage.Stage) tfNomCategorie.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            showError("Impossible d'ajouter la catégorie : " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    @FXML
    private void annuler() {
        javafx.stage.Stage stage = (javafx.stage.Stage) tfNomCategorie.getScene().getWindow();
        stage.close();
    }


}