package Controllers.event;

import Entities.event.Category;
import Entities.event.Event;
import Services.event.CategoryService;
import Services.event.EventAiAssistantService;
import Services.event.EventService;
import Utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
import java.util.concurrent.CompletableFuture;

public class AddEventController {

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
    private Button btnGenerateDescription;
    @FXML
    private Button btnGenerateImage;

    private final EventService eventService = new EventService();
    private final CategoryService categoryService = new CategoryService();
    private final EventAiAssistantService aiAssistantService = new EventAiAssistantService();

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

    @FXML
    private void handleAjouter() {
        try {
            Event event = buildAndValidateEvent();
            event.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            eventService.add(event);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Evenement ajoute avec succes.");
            closeWindow();
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", e.getMessage());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleAnnuler() {
        closeWindow();
    }

    @FXML
    private void handleGenerateDescription() {
        String title = tfTitle.getText() != null ? tfTitle.getText().trim() : "";
        if (title.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Enter the title first to generate a description.");
            return;
        }

        btnGenerateDescription.setDisable(true);
        String originalText = btnGenerateDescription.getText();
        btnGenerateDescription.setText("Generating...");

        CompletableFuture
                .supplyAsync(() -> aiAssistantService.generateEventDescription(title))
                .thenAccept(generatedDescription -> Platform.runLater(() -> {
                    btnGenerateDescription.setDisable(false);
                    btnGenerateDescription.setText(originalText);

                    if (generatedDescription == null || generatedDescription.isBlank()) {
                        showAlert(Alert.AlertType.WARNING, "AI", "No description was generated.");
                        return;
                    }

                    if (generatedDescription.startsWith("AI key missing")
                            || generatedDescription.startsWith("AI service error")
                            || generatedDescription.startsWith("Failed to call AI service")
                            || generatedDescription.startsWith("Title is required")) {
                        showAlert(Alert.AlertType.WARNING, "AI", generatedDescription);
                        return;
                    }

                    taDescription.setText(generatedDescription);

                    String imageUrl = aiAssistantService.buildPollinationsImageUrl(title, generatedDescription);
                    tfImage.setText(imageUrl);
                }));
    }

    @FXML
    private void handleGenerateImageUrl() {
        String title = tfTitle.getText() != null ? tfTitle.getText().trim() : "";
        String description = taDescription.getText() != null ? taDescription.getText().trim() : "";

        if (title.isBlank() && description.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Enter a title or description first to generate an image URL.");
            return;
        }

        btnGenerateImage.setDisable(true);
        String previousText = btnGenerateImage.getText();
        btnGenerateImage.setText("Generating...");

        CompletableFuture
                .supplyAsync(() -> aiAssistantService.buildPollinationsImageUrl(title, description))
                .thenAccept(url -> Platform.runLater(() -> {
                    btnGenerateImage.setDisable(false);
                    btnGenerateImage.setText(previousText);
                    tfImage.setText(url);
                }));
    }

    private Event buildAndValidateEvent() {
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

        Event event = new Event();
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
        return event;
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
