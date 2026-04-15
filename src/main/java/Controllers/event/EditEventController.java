package Controllers.event;

import Controllers.Marketplace.PageHost;
import Entities.event.Category;
import Entities.event.Event;
import Services.event.CategoryService;
import Services.event.EventService;
import Utils.EventNavigationState;
import Utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

public class EditEventController {

    @FXML
    private TextField tfTitle;
    @FXML
    private TextArea taDescription;
    @FXML
    private TextField tfLocation;
    @FXML
    private DatePicker dpStartDate;
    @FXML
    private DatePicker dpEndDate;
    @FXML
    private TextField tfImage;
    @FXML
    private TextField tfCapacity;
    @FXML
    private ComboBox<Category> cbCategory;
    @FXML
    private ComboBox<String> cbStatus;
    @FXML
    private ComboBox<String> cbLocationType;
    @FXML
    private Label lblTitleError;
    @FXML
    private Label lblDescriptionError;
    @FXML
    private Label lblLocationError;
    @FXML
    private Label lblStartDateError;
    @FXML
    private Label lblEndDateError;
    @FXML
    private Label lblImageError;
    @FXML
    private Label lblCapacityError;
    @FXML
    private Label lblCategoryError;
    @FXML
    private Label lblStatusError;
    @FXML
    private Label lblLocationTypeError;
    @FXML
    private Label lblFormError;

    private final EventService eventService = new EventService();
    private final CategoryService categoryService = new CategoryService();
    private Event event;
    private PageHost dashboardContext;

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @FXML
    public void initialize() {
        try {
            cbCategory.setItems(FXCollections.observableArrayList(categoryService.getAll()));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les categories: " + e.getMessage());
        }

        cbStatus.setItems(FXCollections.observableArrayList("ACTIVE", "INACTIVE", "DRAFT"));
        cbLocationType.setItems(FXCollections.observableArrayList("indoor", "outdoor"));

        tfTitle.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblTitleError, ""));
        taDescription.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblDescriptionError, ""));
        tfLocation.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblLocationError, ""));
        dpStartDate.valueProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblStartDateError, ""));
        dpEndDate.valueProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblEndDateError, ""));
        tfImage.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblImageError, ""));
        tfCapacity.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblCapacityError, ""));
        cbCategory.valueProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblCategoryError, ""));
        cbStatus.valueProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblStatusError, ""));
        cbLocationType.valueProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblLocationTypeError, ""));

        Event editingEvent = EventNavigationState.getEditingEvent();
        if (editingEvent != null) {
            setEvent(editingEvent);
        }
    }

    public void setEvent(Event event) {
        this.event = event;
        tfTitle.setText(event.getTitle());
        taDescription.setText(event.getDescription());
        tfLocation.setText(event.getLocation());
        tfImage.setText(event.getImage());
        tfCapacity.setText(String.valueOf(event.getCapacity()));
        cbStatus.setValue(event.getStatus());
        cbLocationType.setValue(event.getLocationType());

        if (event.getStartDate() != null) {
            dpStartDate.setValue(event.getStartDate().toLocalDateTime().toLocalDate());
        }
        if (event.getEndDate() != null) {
            dpEndDate.setValue(event.getEndDate().toLocalDateTime().toLocalDate());
        }

        for (Category category : cbCategory.getItems()) {
            if (event.getCategory() != null && category.getId() == event.getCategory().getId()) {
                cbCategory.getSelectionModel().select(category);
                break;
            }
        }
    }

    @FXML
    private void sauvegarder() {
        if (event == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun evenement a modifier.");
            return;
        }

        clearInlineErrors();
        if (!updateAndValidateEvent()) {
            return;
        }

        try {
            eventService.update(event);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Evenement mis a jour avec succes.");
            EventNavigationState.clearEditingEvent();
            navigateBackToEventList();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void annuler() {
        EventNavigationState.clearEditingEvent();
        navigateBackToEventList();
    }

    private boolean updateAndValidateEvent() {
        EventFormValidator.Result validation = EventFormValidator.validate(
                tfTitle.getText(),
                taDescription.getText(),
                tfLocation.getText(),
                dpStartDate.getValue(),
                dpEndDate.getValue(),
                tfImage.getText(),
                tfCapacity.getText(),
                cbCategory.getValue(),
                cbStatus.getValue(),
                cbLocationType.getValue(),
                SessionManager.getCurrentUser() != null
        );

        if (!validation.isValid()) {
            applyValidationErrors(validation.getErrors());
            return false;
        }

        event.setTitle(validation.getTitle());
        event.setDescription(validation.getDescription());
        event.setLocation(validation.getLocation());
        event.setStartDate(Timestamp.valueOf(LocalDateTime.of(validation.getStartDate(), LocalTime.of(0, 0))));
        event.setEndDate(Timestamp.valueOf(LocalDateTime.of(validation.getEndDate(), LocalTime.of(23, 59, 59))));
        event.setImage(validation.getImage());
        event.setCapacity(validation.getCapacity());
        event.setQrCodePath(null);
        event.setStatus(validation.getStatus());
        event.setCategory(validation.getCategory());
        event.setCreatedById(SessionManager.getCurrentUser().getId());
        event.setVisualVibe(null);
        event.setLocationType(validation.getLocationType());
        return true;
    }

    private void applyValidationErrors(Map<String, String> errors) {
        setInlineError(lblTitleError, errors.get(EventFormValidator.FIELD_TITLE));
        setInlineError(lblDescriptionError, errors.get(EventFormValidator.FIELD_DESCRIPTION));
        setInlineError(lblLocationError, errors.get(EventFormValidator.FIELD_LOCATION));
        setInlineError(lblStartDateError, errors.get(EventFormValidator.FIELD_START_DATE));
        setInlineError(lblEndDateError, errors.get(EventFormValidator.FIELD_END_DATE));
        setInlineError(lblImageError, errors.get(EventFormValidator.FIELD_IMAGE));
        setInlineError(lblCapacityError, errors.get(EventFormValidator.FIELD_CAPACITY));
        setInlineError(lblCategoryError, errors.get(EventFormValidator.FIELD_CATEGORY));
        setInlineError(lblStatusError, errors.get(EventFormValidator.FIELD_STATUS));
        setInlineError(lblLocationTypeError, errors.get(EventFormValidator.FIELD_LOCATION_TYPE));
        setInlineError(lblFormError, errors.get(EventFormValidator.FIELD_FORM));
    }

    private void closeWindow() {
        Stage stage = (Stage) tfTitle.getScene().getWindow();
        stage.close();
    }

    private void navigateBackToEventList() {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/EventView.fxml");
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
        setInlineError(lblTitleError, "");
        setInlineError(lblDescriptionError, "");
        setInlineError(lblLocationError, "");
        setInlineError(lblStartDateError, "");
        setInlineError(lblEndDateError, "");
        setInlineError(lblImageError, "");
        setInlineError(lblCapacityError, "");
        setInlineError(lblCategoryError, "");
        setInlineError(lblStatusError, "");
        setInlineError(lblLocationTypeError, "");
        setInlineError(lblFormError, "");
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
