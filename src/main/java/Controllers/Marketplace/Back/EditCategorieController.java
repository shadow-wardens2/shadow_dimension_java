package Controllers.Marketplace.Back;

import Entities.Marketplace.Categorie;
import Services.Marketplace.ServiceCategorie;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EditCategorieController {

    @FXML
    private TextField tfNom;
    @FXML
    private TextField tfDescription;


    @FXML
    private javafx.scene.control.Label errorLabel;

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

        // Clear previous error
        errorLabel.setVisible(false);
        errorLabel.setText("");

        if (nom.isEmpty()) {
            showError("Le nom ne peut pas être vide.");
            return;
        }

        if (nom.length() <= 3) {
            showError("Le nom de la catégorie doit avoir plus de 3 caractères !");
            return;
        }

        try {
            // Duplicate Check
            for (Categorie existing : serviceCategorie.getAll()) {
                if (existing.getNom().equalsIgnoreCase(nom) && existing.getId() != categorie.getId()) {
                    showError("Une autre catégorie porte déjà ce nom !");
                    return;
                }
            }

            categorie.setNom(nom);
            categorie.setDescription(description);

            serviceCategorie.update(categorie);
            Utils.ValidationUtils.showSuccess("Succès", "Catégorie mise à jour avec succès !");
            closeWindow();
        } catch (SQLException e) {
            showError("Impossible de mettre à jour : " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
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
