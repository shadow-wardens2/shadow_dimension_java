package Controllers.event;

import Entities.event.Category;
import Entities.event.Event;
import Services.event.CategoryService;
import Services.event.EventService;
import Utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
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

    private final EventService eventService = new EventService();
    private final CategoryService categoryService = new CategoryService();
    private Event event;

    @FXML
    public void initialize() {
        try {
            cbCategory.setItems(FXCollections.observableArrayList(categoryService.getAll()));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les categories: " + e.getMessage());
        }

        cbStatus.setItems(FXCollections.observableArrayList("ACTIVE", "INACTIVE", "DRAFT"));
        cbLocationType.setItems(FXCollections.observableArrayList("indoor", "outdoor"));
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

        try {
            updateAndValidateEvent();
            eventService.update(event);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Evenement mis a jour avec succes.");
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

    private void updateAndValidateEvent() {
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

        if (title.isEmpty()) throw new IllegalArgumentException("Le titre est obligatoire.");
        if (description.isEmpty()) throw new IllegalArgumentException("La description est obligatoire.");
        if (location.isEmpty()) throw new IllegalArgumentException("La localisation est obligatoire.");
        if (startDate == null) throw new IllegalArgumentException("La date de debut est obligatoire.");
        if (endDate == null) throw new IllegalArgumentException("La date de fin est obligatoire.");
        if (!startDate.isBefore(endDate)) throw new IllegalArgumentException("La date de debut doit etre avant la date de fin.");
        if (image.isEmpty()) throw new IllegalArgumentException("L'image est obligatoire.");
        if (capacityText.isEmpty()) throw new IllegalArgumentException("La capacite est obligatoire.");
        if (category == null) throw new IllegalArgumentException("La categorie est obligatoire.");
        if (status == null || status.isBlank()) throw new IllegalArgumentException("Le status est obligatoire.");
        if (locationType == null || locationType.isBlank()) throw new IllegalArgumentException("Le type de lieu est obligatoire.");
        if (SessionManager.getCurrentUser() == null) throw new IllegalArgumentException("Aucun utilisateur connecte.");

        int capacity;
        try {
            capacity = Integer.parseInt(capacityText);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("La capacite doit etre un nombre entier.");
        }

        if (capacity <= 0) throw new IllegalArgumentException("La capacite doit etre superieure a 0.");

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
    }

    private void closeWindow() {
        Stage stage = (Stage) tfTitle.getScene().getWindow();
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
