package Controllers.Marketplace;

import Entities.Marketplace.Categorie;
import Services.Marketplace.ServiceCategorie;
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
            Utils.ValidationUtils.showAlert("Erreur", "Le nom de la catégorie ne peut pas être vide !");
            return;
        }

        if (nom.length() <= 3) {
            Utils.ValidationUtils.showAlert("Erreur", "Le nom de la catégorie doit avoir plus de 3 caractères !");
            return;
        }

        try {
            // Duplicate Check
            for (Categorie existing : serviceCategorie.getAll()) {
                if (existing.getNom().equalsIgnoreCase(nom)) {
                    Utils.ValidationUtils.showAlert("Doublon", "Cette catégorie existe déjà !");
                    return;
                }
            }

            Categorie c = new Categorie(0, nom, description);
            serviceCategorie.add(c);
            Utils.ValidationUtils.showSuccess("Succès", "Catégorie ajoutée avec succès !");
            javafx.stage.Stage stage = (javafx.stage.Stage) tfNomCategorie.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            Utils.ValidationUtils.showAlert("Erreur", "Impossible d'ajouter la catégorie : " + e.getMessage());
        }
    }

    @FXML
    private void annuler() {
        javafx.stage.Stage stage = (javafx.stage.Stage) tfNomCategorie.getScene().getWindow();
        stage.close();
    }


}