package Controllers.event;

// Host interface for loading pages in dashboard content area.
import Controllers.Marketplace.PageHost;
// Category entity used in category selector.
import Entities.event.Category;
// Event entity currently edited.
import Entities.event.Event;
// Service to load category choices.
import Services.event.CategoryService;
// Service to persist event updates.
import Services.event.EventService;
// Shared edit-state transport between list and edit pages.
import Utils.EventNavigationState;
// Session helper for current user id validation.
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

// Controller for Edit Event page.
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

    // Event persistence service.
    private final EventService eventService = new EventService();
    // Category lookup service.
    private final CategoryService categoryService = new CategoryService();
    // Currently loaded event being edited.
    private Event event;
    // Host context for back navigation.
    private PageHost dashboardContext;

    // Injects dashboard host context.
    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    // JavaFX lifecycle initialization callback.
    @FXML
    public void initialize() {
        try {
            // Loads category options in combobox.
            cbCategory.setItems(FXCollections.observableArrayList(categoryService.getAll()));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les categories: " + e.getMessage());
        }

        // Populates status options.
        cbStatus.setItems(FXCollections.observableArrayList("ACTIVE", "INACTIVE", "DRAFT"));
        // Populates location type options.
        cbLocationType.setItems(FXCollections.observableArrayList("indoor", "outdoor"));

        // Clears validation labels when user edits values.
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

        // Recovers selected event from navigation state.
        Event editingEvent = EventNavigationState.getEditingEvent();
        if (editingEvent != null) {
            setEvent(editingEvent);
        }
    }

    // Pre-fills form fields from selected event.
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
                // Selects matching category item in combobox.
                cbCategory.getSelectionModel().select(category);
                break;
            }
        }
    }

    // Save button handler.
    @FXML
    private void sauvegarder() {
        // Guard when no event was provided for editing.
        if (event == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun evenement a modifier.");
            return;
        }

        // Clears old validation messages.
        clearInlineErrors();
        // Runs validation and model update.
        if (!updateAndValidateEvent()) {
            return;
        }

        try {
            // Persists update to database.
            eventService.update(event);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Evenement mis a jour avec succes.");
            // Clears shared edit state and returns to list.
            EventNavigationState.clearEditingEvent();
            navigateBackToEventList();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // Cancel handler.
    @FXML
    private void annuler() {
        // Clears edit state then returns without saving.
        EventNavigationState.clearEditingEvent();
        navigateBackToEventList();
    }

    // Validates form then copies validated values into current event object.
    private boolean updateAndValidateEvent() {
        // Delegates rules to centralized validator.
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

            // Writes validation messages into UI when invalid.
        if (!validation.isValid()) {
            applyValidationErrors(validation.getErrors());
            return false;
        }

            // Copies normalized validated values to mutable event entity.
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

    // Maps validator keys to inline label components.
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

    // Fallback close for standalone modal usage.
    private void closeWindow() {
        Stage stage = (Stage) tfTitle.getScene().getWindow();
        stage.close();
    }

    // Returns to event list through host or fallback stage close.
    private void navigateBackToEventList() {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/EventView.fxml");
            return;
        }
        closeWindow();
    }

    // Shared alert helper.
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Clears all inline validation labels.
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

    // Shows/hides one inline error label depending on message content.
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
