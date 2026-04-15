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
import java.util.Map;

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
<<<<<<< HEAD
    private Label lblNomError;
    @FXML
    private Label lblDescriptionError;
    @FXML
    private Label lblTarificationError;
    @FXML
    private Label lblPrixError;
=======
    private javafx.scene.control.Label lblError;
>>>>>>> origin/gestion-produits-v5

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
        lblError.setVisible(false);
        if (category == null) {
            showError("Aucune categorie a modifier.");
            return;
        }

        clearInlineErrors();
        if (!updateAndValidateCategory()) {
            return;
        }

        try {
            categoryService.update(category);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Categorie mise a jour avec succes.");
<<<<<<< HEAD
            EventNavigationState.clearEditingCategory();
            navigateBackToCategoryList();
=======
            closeWindow();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
>>>>>>> origin/gestion-produits-v5
        } catch (SQLException e) {
            showError("Erreur SQL: " + e.getMessage());
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
    }

    @FXML
    private void annuler() {
        EventNavigationState.clearEditingCategory();
        navigateBackToCategoryList();
    }

    private boolean updateAndValidateCategory() {
        CategoryFormValidator.Result validation = CategoryFormValidator.validate(
                tfNom.getText(),
                taDescription.getText(),
                cbTypeTarification.getValue(),
                tfPrix.getText()
        );

        if (!validation.isValid()) {
            applyValidationErrors(validation.getErrors());
            return false;
        }

        category.setNom(validation.getNom());
        category.setDescription(validation.getDescription());
        category.setTypeTarification(validation.getTarification());
        category.setPrix(validation.getPrix());
        category.setCreatorType(null);
        return true;
    }

    private void applyValidationErrors(Map<String, String> errors) {
        setInlineError(lblNomError, errors.get(CategoryFormValidator.FIELD_NOM));
        setInlineError(lblDescriptionError, errors.get(CategoryFormValidator.FIELD_DESCRIPTION));
        setInlineError(lblTarificationError, errors.get(CategoryFormValidator.FIELD_TARIFICATION));
        setInlineError(lblPrixError, errors.get(CategoryFormValidator.FIELD_PRIX));
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
