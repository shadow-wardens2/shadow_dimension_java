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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
        String title = tfTitle.getText() != null ? tfTitle.getText().trim() : "";
        String description = taDescription.getText() != null ? taDescription.getText().trim() : "";
        String location = tfLocation.getText() != null ? tfLocation.getText().trim() : "";
        String image = tfImage.getText() != null ? tfImage.getText().trim() : "";
        String capacityText = tfCapacity.getText() != null ? tfCapacity.getText().trim() : "";
        LocalDate startDate = dpStartDate.getValue();
        LocalDate endDate = dpEndDate.getValue();
        Category category = cbCategory.getValue();
        String status = cbStatus.getValue();
        String locationType = cbLocationType.getValue();
        boolean hasInputError = false;

        if (title.isEmpty()) {
            setInlineError(lblTitleError, "Le titre est obligatoire.");
            hasInputError = true;
        }
        if (description.isEmpty()) {
            setInlineError(lblDescriptionError, "La description est obligatoire.");
            hasInputError = true;
        }
        if (location.isEmpty()) {
            setInlineError(lblLocationError, "La localisation est obligatoire.");
            hasInputError = true;
        }
        if (startDate == null) {
            setInlineError(lblStartDateError, "La date de debut est obligatoire.");
            hasInputError = true;
        }
        if (endDate == null) {
            setInlineError(lblEndDateError, "La date de fin est obligatoire.");
            hasInputError = true;
        }
        if (startDate != null && endDate != null && !startDate.isBefore(endDate)) {
            setInlineError(lblEndDateError, "La date de fin doit etre apres la date de debut.");
            hasInputError = true;
        }
        if (image.isEmpty()) {
            setInlineError(lblImageError, "L'image est obligatoire.");
            hasInputError = true;
        }
        if (capacityText.isEmpty()) {
            setInlineError(lblCapacityError, "La capacite est obligatoire.");
            hasInputError = true;
        }
        if (category == null) {
            setInlineError(lblCategoryError, "La categorie est obligatoire.");
            hasInputError = true;
        }
        if (status == null || status.isBlank()) {
            setInlineError(lblStatusError, "Le status est obligatoire.");
            hasInputError = true;
        }
        if (locationType == null || locationType.isBlank()) {
            setInlineError(lblLocationTypeError, "Le type de lieu est obligatoire.");
            hasInputError = true;
        }
        if (SessionManager.getCurrentUser() == null) {
            setInlineError(lblFormError, "Aucun utilisateur connecte.");
            hasInputError = true;
        }

        int capacity;
        try {
            capacity = Integer.parseInt(capacityText);
        } catch (NumberFormatException ex) {
            setInlineError(lblCapacityError, "La capacite doit etre un nombre entier.");
            hasInputError = true;
            capacity = -1;
        }

        if (!capacityText.isEmpty() && capacity <= 0) {
            setInlineError(lblCapacityError, "La capacite doit etre superieure a 0.");
            hasInputError = true;
        }

        if (hasInputError) {
            return false;
        }

        event.setTitle(title);
        event.setDescription(description);
        event.setLocation(location);
        event.setStartDate(Timestamp.valueOf(LocalDateTime.of(startDate, LocalTime.of(0, 0))));
        event.setEndDate(Timestamp.valueOf(LocalDateTime.of(endDate, LocalTime.of(23, 59, 59))));
        event.setImage(image);
        event.setCapacity(capacity);
        event.setQrCodePath(null);
        event.setStatus(status);
        event.setCategory(category);
        event.setCreatedById(SessionManager.getCurrentUser().getId());
        event.setVisualVibe(null);
        event.setLocationType(locationType);
        return true;
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
