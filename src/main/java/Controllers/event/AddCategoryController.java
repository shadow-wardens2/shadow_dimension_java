package Controllers.event;

// Host interface used for in-page navigation.
import Controllers.Marketplace.PageHost;
// Category model handled by this form.
import Entities.event.Category;
// Service used to persist category data.
import Services.event.CategoryService;
// Observable list factory helpers.
import javafx.collections.FXCollections;
// FXML injection annotation.
import javafx.fxml.FXML;
// Alert popup type for success/error messages.
import javafx.scene.control.Alert;
// Combo box UI control.
import javafx.scene.control.ComboBox;
// Label UI control (for inline errors).
import javafx.scene.control.Label;
// Text area UI control.
import javafx.scene.control.TextArea;
// Text field UI control.
import javafx.scene.control.TextField;
// Stage fallback when page host is missing.
import javafx.stage.Stage;

// SQL exception type from service calls.
import java.sql.SQLException;
// Timestamp type to set created_at.
import java.sql.Timestamp;
// Map interface for validator error mapping.
import java.util.Map;

// Controller for Add Category page.
public class AddCategoryController {

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

    // Service layer instance for category persistence.
    private final CategoryService categoryService = new CategoryService();
    // Dashboard host context for in-page navigation.
    private PageHost dashboardContext;

    // Injects dashboard host from parent controller.
    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    // JavaFX lifecycle init after FXML fields are injected.
    @FXML
    public void initialize() {
        // Populates pricing type options.
        cbTypeTarification.setItems(FXCollections.observableArrayList("FREE", "PAID"));
        // Reacts to pricing type changes.
        cbTypeTarification.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Clears old error for pricing type.
            setInlineError(lblTarificationError, "");
            // FREE disables and clears price input.
            if ("FREE".equals(newVal)) {
                tfPrix.clear();
                tfPrix.setDisable(true);
                setInlineError(lblPrixError, "");
            } else {
                // PAID re-enables price input.
                tfPrix.setDisable(false);
            }
        });

        // Clears name error while user types.
        tfNom.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblNomError, ""));
        // Clears description error while user types.
        taDescription.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblDescriptionError, ""));
        // Clears price error while user types.
        tfPrix.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblPrixError, ""));
    }

    // Save handler for "Ajouter" button.
    @FXML
    private void handleAjouter() {
        // Resets all inline errors before validation.
        clearInlineErrors();

        // Builds and validates category from form values.
        Category category = buildAndValidateCategory();
        // Stops when validation failed.
        if (category == null) {
            return;
        }

        try {
            // Sets creation timestamp for new row.
            category.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            // Persists category via service layer.
            categoryService.add(category);
            // Shows success feedback.
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Categorie ajoutee avec succes.");
            // Returns to list page.
            navigateBackToCategoryList();
        } catch (SQLException e) {
            // Reports SQL failure.
            showError("Erreur SQL: " + e.getMessage());
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
    }

    @FXML
    private void handleAnnuler() {
        navigateBackToCategoryList();
    }

    // Reads form values, validates them, and returns model when valid.
    private Category buildAndValidateCategory() {
        // Delegates all rules to centralized validator.
        CategoryFormValidator.Result validation = CategoryFormValidator.validate(
                tfNom.getText(),
                taDescription.getText(),
                cbTypeTarification.getValue(),
                tfPrix.getText()
        );

        // Maps validator errors to inline labels.
        if (!validation.isValid()) {
            applyValidationErrors(validation.getErrors());
            return null;
        }

        // Creates entity from normalized validator output.
        Category category = new Category();
        category.setNom(validation.getNom());
        category.setDescription(validation.getDescription());
        category.setTypeTarification(validation.getTarification());
        category.setPrix(validation.getPrix());
        // creator_type is auto-defaulted by service layer.
        category.setCreatorType(null);
        return category;
    }

    // Sends validator field errors to matching UI labels.
    private void applyValidationErrors(Map<String, String> errors) {
        setInlineError(lblNomError, errors.get(CategoryFormValidator.FIELD_NOM));
        setInlineError(lblDescriptionError, errors.get(CategoryFormValidator.FIELD_DESCRIPTION));
        setInlineError(lblTarificationError, errors.get(CategoryFormValidator.FIELD_TARIFICATION));
        setInlineError(lblPrixError, errors.get(CategoryFormValidator.FIELD_PRIX));
    }

    // Fallback close when controller is opened in standalone stage.
    private void closeWindow() {
        Stage stage = (Stage) tfNom.getScene().getWindow();
        stage.close();
    }

    // Returns to category management page.
    private void navigateBackToCategoryList() {
        // Preferred path: in-page navigation through dashboard host.
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/CategoryView.fxml");
            return;
        }
        // Fallback path: close modal stage.
        closeWindow();
    }

    // Shared alert utility for success/error feedback.
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Clears all inline validation labels.
    private void clearInlineErrors() {
        setInlineError(lblNomError, "");
        setInlineError(lblDescriptionError, "");
        setInlineError(lblTarificationError, "");
        setInlineError(lblPrixError, "");
    }

    // Sets or hides one inline validation label.
    private void setInlineError(Label label, String message) {
        // Guards against missing label references.
        if (label == null) {
            return;
        }
        // Decides whether to show label depending on message content.
        boolean show = message != null && !message.isBlank();
        label.setText(show ? message : "");
        label.setVisible(show);
        label.setManaged(show);
    }
}
