package Controllers.event;

import Controllers.Marketplace.PageHost;
import Entities.event.Category;
import Services.event.CategoryService;
import Utils.EventNavigationState;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
    @FXML
    private Label lblNomError;
    @FXML
    private Label lblDescriptionError;
    @FXML
    private Label lblTarificationError;
    @FXML
    private Label lblPrixError;

    private final CategoryService categoryService = new CategoryService();
    private Category category;
    private PageHost dashboardContext;

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @FXML
    public void initialize() {
        cbTypeTarification.setItems(FXCollections.observableArrayList("FREE", "PAID"));
        cbTypeTarification.valueProperty().addListener((obs, oldVal, newVal) -> {
            setInlineError(lblTarificationError, "");
            if ("FREE".equals(newVal)) {
                tfPrix.clear();
                tfPrix.setDisable(true);
                setInlineError(lblPrixError, "");
            } else {
                tfPrix.setDisable(false);
            }
        });

        tfNom.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblNomError, ""));
        taDescription.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblDescriptionError, ""));
        tfPrix.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblPrixError, ""));

        Category editingCategory = EventNavigationState.getEditingCategory();
        if (editingCategory != null) {
            setCategory(editingCategory);
        }
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

        clearInlineErrors();
        if (!updateAndValidateCategory()) {
            return;
        }

        try {
            categoryService.update(category);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Categorie mise a jour avec succes.");
            EventNavigationState.clearEditingCategory();
            navigateBackToCategoryList();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void annuler() {
        EventNavigationState.clearEditingCategory();
        navigateBackToCategoryList();
    }

    private boolean updateAndValidateCategory() {
        String nom = tfNom.getText() != null ? tfNom.getText().trim() : "";
        String description = taDescription.getText() != null ? taDescription.getText().trim() : "";
        String tarification = cbTypeTarification.getValue();
        boolean hasInputError = false;

        if (nom.isEmpty()) {
            setInlineError(lblNomError, "Le nom est obligatoire.");
            hasInputError = true;
        }
        if (description.isEmpty()) {
            setInlineError(lblDescriptionError, "La description est obligatoire.");
            hasInputError = true;
        }
        if (tarification == null || tarification.isBlank()) {
            setInlineError(lblTarificationError, "Le type de tarification est obligatoire.");
            hasInputError = true;
        }

        Double prix = null;
        if ("PAID".equals(tarification)) {
            String prixText = tfPrix.getText() != null ? tfPrix.getText().trim() : "";
            if (prixText.isEmpty()) {
                setInlineError(lblPrixError, "Le prix est obligatoire pour une categorie payante.");
                hasInputError = true;
            }
            try {
                if (!prixText.isEmpty()) {
                    prix = Double.parseDouble(prixText);
                }
            } catch (NumberFormatException e) {
                setInlineError(lblPrixError, "Le prix doit etre un nombre valide.");
                hasInputError = true;
            }
            if (prix != null && prix <= 0) {
                setInlineError(lblPrixError, "Le prix doit etre superieur a 0.");
                hasInputError = true;
            }
        }

        if (hasInputError) {
            return false;
        }

        category.setNom(nom);
        category.setDescription(description);
        category.setTypeTarification(tarification);
        category.setPrix(prix);
        category.setCreatorType(null);
        return true;
    }

    private void closeWindow() {
        Stage stage = (Stage) tfNom.getScene().getWindow();
        stage.close();
    }

    private void navigateBackToCategoryList() {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/CategoryView.fxml");
            return;
        }
        closeWindow();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearInlineErrors() {
        setInlineError(lblNomError, "");
        setInlineError(lblDescriptionError, "");
        setInlineError(lblTarificationError, "");
        setInlineError(lblPrixError, "");
    }

    private void setInlineError(Label label, String message) {
        if (label == null) {
            return;
        }
        boolean show = message != null && !message.isBlank();
        label.setText(show ? message : "");
        label.setVisible(show);
        label.setManaged(show);
    }
}
