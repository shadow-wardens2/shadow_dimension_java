package Controllers.event;

import Entities.event.Category;
import Services.event.CategoryService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EditCategoryController {

    @FXML
    private TextField tfNom;
    @FXML
    private TextArea taDescription;
    @FXML
    private ComboBox<String> cbTypeTarification;
    @FXML
    private TextField tfPrix;

    private final CategoryService categoryService = new CategoryService();
    private Category category;

    @FXML
    public void initialize() {
        cbTypeTarification.setItems(FXCollections.observableArrayList("FREE", "PAID"));
        cbTypeTarification.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("FREE".equals(newVal)) {
                tfPrix.clear();
                tfPrix.setDisable(true);
            } else {
                tfPrix.setDisable(false);
            }
        });
    }

    public void setCategory(Category category) {
        this.category = category;
        tfNom.setText(category.getNom());
        taDescription.setText(category.getDescription());
        cbTypeTarification.setValue(category.getTypeTarification());
        tfPrix.setText(category.getPrix() == null ? "" : String.valueOf(category.getPrix()));
        tfPrix.setDisable("FREE".equals(category.getTypeTarification()));
    }

    @FXML
    private void sauvegarder() {
        if (category == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucune categorie a modifier.");
            return;
        }

        try {
            updateAndValidateCategory();
            categoryService.update(category);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Categorie mise a jour avec succes.");
            closeWindow();
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", e.getMessage());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void annuler() {
        closeWindow();
    }

    private void updateAndValidateCategory() {
        String nom = tfNom.getText() != null ? tfNom.getText().trim() : "";
        String description = taDescription.getText() != null ? taDescription.getText().trim() : "";
        String tarification = cbTypeTarification.getValue();

        if (nom.isEmpty()) {
            throw new IllegalArgumentException("Le nom est obligatoire.");
        }
        if (description.isEmpty()) {
            throw new IllegalArgumentException("La description est obligatoire.");
        }
        if (tarification == null || tarification.isBlank()) {
            throw new IllegalArgumentException("Le type de tarification est obligatoire.");
        }

        Double prix = null;
        if ("PAID".equals(tarification)) {
            String prixText = tfPrix.getText() != null ? tfPrix.getText().trim() : "";
            if (prixText.isEmpty()) {
                throw new IllegalArgumentException("Le prix est obligatoire pour une categorie payante.");
            }
            try {
                prix = Double.parseDouble(prixText);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Le prix doit etre un nombre valide.");
            }
            if (prix <= 0) {
                throw new IllegalArgumentException("Le prix doit etre superieur a 0.");
            }
        }

        category.setNom(nom);
        category.setDescription(description);
        category.setTypeTarification(tarification);
        category.setPrix(prix);
        category.setCreatorType(null);
    }

    private void closeWindow() {
        Stage stage = (Stage) tfNom.getScene().getWindow();
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
