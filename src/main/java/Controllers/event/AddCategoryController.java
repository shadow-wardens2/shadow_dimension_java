package Controllers.event;

import Controllers.Marketplace.PageHost;
import Entities.event.Category;
import Services.event.CategoryService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

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

    private final CategoryService categoryService = new CategoryService();
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
    }

    @FXML
    private void handleAjouter() {
        clearInlineErrors();

        Category category = buildAndValidateCategory();
        if (category == null) {
            return;
        }

        try {
            category.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            categoryService.add(category);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Categorie ajoutee avec succes.");
            navigateBackToCategoryList();
        } catch (SQLException e) {
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

    private Category buildAndValidateCategory() {
        CategoryFormValidator.Result validation = CategoryFormValidator.validate(
                tfNom.getText(),
                taDescription.getText(),
                cbTypeTarification.getValue(),
                tfPrix.getText()
        );

        if (!validation.isValid()) {
            applyValidationErrors(validation.getErrors());
            return null;
        }

        Category category = new Category();
        category.setNom(validation.getNom());
        category.setDescription(validation.getDescription());
        category.setTypeTarification(validation.getTarification());
        category.setPrix(validation.getPrix());
        category.setCreatorType(null);
        return category;
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
