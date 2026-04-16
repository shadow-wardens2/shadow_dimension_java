package Controllers.event;

// Host interface used to navigate within dashboard content area.
import Controllers.Marketplace.PageHost;
// Category entity being edited.
import Entities.event.Category;
// Service for loading/saving category data.
import Services.event.CategoryService;
// Shared navigation state carrying selected category for edit pages.
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

// Controller for Edit Category page.
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

    // Service instance used to persist updates.
    private final CategoryService categoryService = new CategoryService();
    // Currently edited category model.
    private Category category;
    // Dashboard host context for in-page back navigation.
    private PageHost dashboardContext;

    // Injects dashboard host from parent page.
    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    // JavaFX initialization callback.
    @FXML
    public void initialize() {
        // Populates pricing options.
        cbTypeTarification.setItems(FXCollections.observableArrayList("FREE", "PAID"));
        // Handles dynamic UI behavior when pricing type changes.
        cbTypeTarification.valueProperty().addListener((obs, oldVal, newVal) -> {
            setInlineError(lblTarificationError, "");
            if ("FREE".equals(newVal)) {
                // FREE: clear and disable price field.
                tfPrix.clear();
                tfPrix.setDisable(true);
                setInlineError(lblPrixError, "");
            } else {
                // PAID: enable price input.
                tfPrix.setDisable(false);
            }
        });

        // Clears inline errors while user edits fields.
        tfNom.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblNomError, ""));
        taDescription.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblDescriptionError, ""));
        tfPrix.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblPrixError, ""));

        // Loads category selected from list page through shared state.
        Category editingCategory = EventNavigationState.getEditingCategory();
        if (editingCategory != null) {
            setCategory(editingCategory);
        }
    }

    // Pre-fills form controls using selected category data.
    public void setCategory(Category category) {
        this.category = category;
        tfNom.setText(category.getNom());
        taDescription.setText(category.getDescription());
        cbTypeTarification.setValue(category.getTypeTarification());
        tfPrix.setText(category.getPrix() == null ? "" : String.valueOf(category.getPrix()));
        tfPrix.setDisable("FREE".equals(category.getTypeTarification()));
    }

    // Save button handler for update operation.
    @FXML
    private void sauvegarder() {
        // Guard when no category is loaded.
        if (category == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucune categorie a modifier.");
            return;
        }

        // Clears previous validation errors.
        clearInlineErrors();
        // Applies validator before persistence.
        if (!updateAndValidateCategory()) {
            return;
        }

        try {
            // Persists update through service layer.
            categoryService.update(category);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Categorie mise a jour avec succes.");
            // Clears shared edit state and returns to list page.
            EventNavigationState.clearEditingCategory();
            navigateBackToCategoryList();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // Cancel button handler.
    @FXML
    private void annuler() {
        // Clears edit state then goes back to list.
        EventNavigationState.clearEditingCategory();
        navigateBackToCategoryList();
    }

    // Validates form values and updates entity when valid.
    private boolean updateAndValidateCategory() {
        // Delegates validation rules to shared validator class.
        CategoryFormValidator.Result validation = CategoryFormValidator.validate(
                tfNom.getText(),
                taDescription.getText(),
                cbTypeTarification.getValue(),
                tfPrix.getText()
        );

        // Maps errors to inline labels when invalid.
        if (!validation.isValid()) {
            applyValidationErrors(validation.getErrors());
            return false;
        }

        // Copies validated values back into the current entity.
        category.setNom(validation.getNom());
        category.setDescription(validation.getDescription());
        category.setTypeTarification(validation.getTarification());
        category.setPrix(validation.getPrix());
        category.setCreatorType(null);
        return true;
    }

    // Sends validator errors to field-specific labels.
    private void applyValidationErrors(Map<String, String> errors) {
        setInlineError(lblNomError, errors.get(CategoryFormValidator.FIELD_NOM));
        setInlineError(lblDescriptionError, errors.get(CategoryFormValidator.FIELD_DESCRIPTION));
        setInlineError(lblTarificationError, errors.get(CategoryFormValidator.FIELD_TARIFICATION));
        setInlineError(lblPrixError, errors.get(CategoryFormValidator.FIELD_PRIX));
    }

    // Fallback close when controller is used in standalone stage.
    private void closeWindow() {
        Stage stage = (Stage) tfNom.getScene().getWindow();
        stage.close();
    }

    // Returns to category list through host, else closes local stage.
    private void navigateBackToCategoryList() {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/CategoryView.fxml");
            return;
        }
        closeWindow();
    }

    // Shared utility to display feedback alerts.
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Clears all inline validation messages.
    private void clearInlineErrors() {
        setInlineError(lblNomError, "");
        setInlineError(lblDescriptionError, "");
        setInlineError(lblTarificationError, "");
        setInlineError(lblPrixError, "");
    }

    // Shows or hides one inline validation label.
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
