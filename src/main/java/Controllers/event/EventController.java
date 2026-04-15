package Controllers.event;

import Controllers.Marketplace.PageHost;
import Entities.event.Event;
import Services.event.EventService;
import Utils.EventNavigationState;
import Utils.PdfExportUtil;
import Utils.VoiceToTextUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import javafx.stage.FileChooser;

public class EventController implements Initializable {

    private PageHost dashboardContext;

    @FXML
    private VBox eventsContainer;
    @FXML
    private TextField tfSearch;
    @FXML
    private ComboBox<String> cbSort;
    @FXML
    private Button btnMic;

    private final EventService eventService = new EventService();
    private final ObservableList<Event> masterEvents = FXCollections.observableArrayList();
    private final ObservableList<Event> displayedEvents = FXCollections.observableArrayList();

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSearchAndSort();
        loadEvents();
    }

    private void setupSearchAndSort() {
        cbSort.setItems(FXCollections.observableArrayList(
                "Newest first",
                "Oldest first",
                "Title A-Z",
                "Title Z-A",
                "Capacity high-low",
                "Capacity low-high"
        ));
        cbSort.getSelectionModel().select("Newest first");

        tfSearch.textProperty().addListener((obs, oldValue, newValue) -> applyDynamicFilterAndSort());
        cbSort.valueProperty().addListener((obs, oldValue, newValue) -> applyDynamicFilterAndSort());

        applyDynamicFilterAndSort();
    }

    private void applyDynamicFilterAndSort() {
        String search = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase(Locale.ROOT);

        displayedEvents.clear();
        for (Event event : masterEvents) {
            if (search.isEmpty()) {
                displayedEvents.add(event);
                continue;
            }

            String title = safeLower(event.getTitle());
            String description = safeLower(event.getDescription());
            String location = safeLower(event.getLocation());
            String category = safeLower(event.getCategoryName());
            String status = safeLower(event.getStatus());
            String locationType = safeLower(event.getLocationType());
            String capacity = String.valueOf(event.getCapacity());

            boolean match = title.contains(search)
                    || description.contains(search)
                    || location.contains(search)
                    || category.contains(search)
                    || status.contains(search)
                    || locationType.contains(search)
                    || capacity.contains(search);

            if (match) {
                displayedEvents.add(event);
            }
        }

        String selectedSort = cbSort.getValue();
        Comparator<Event> comparator;

        if ("Oldest first".equals(selectedSort)) {
            comparator = Comparator.comparingInt(Event::getId);
        } else if ("Title A-Z".equals(selectedSort)) {
            comparator = Comparator.comparing(e -> safeLower(e.getTitle()));
        } else if ("Title Z-A".equals(selectedSort)) {
            comparator = Comparator.comparing((Event e) -> safeLower(e.getTitle())).reversed();
        } else if ("Capacity high-low".equals(selectedSort)) {
            comparator = Comparator.comparingInt(Event::getCapacity).reversed();
        } else if ("Capacity low-high".equals(selectedSort)) {
            comparator = Comparator.comparingInt(Event::getCapacity);
        } else {
            comparator = Comparator.comparingInt(Event::getId).reversed();
        }

        displayedEvents.sort(comparator);
        renderEvents();
    }

    @FXML
    private void handleVoiceSearch() {
        btnMic.setDisable(true);

        CompletableFuture.supplyAsync(() -> {
            try {
                return VoiceToTextUtil.recognizeOnce(6);
            } catch (Exception e) {
                return "";
            }
        }).thenAccept(text -> javafx.application.Platform.runLater(() -> {
            btnMic.setDisable(false);
            if (text != null && !text.isBlank()) {
                tfSearch.setText(text);
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Microphone", "Aucune voix detectee. Reessayez.");
            }
        }));
    }

    @FXML
    private void handleOpenChatbot() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/event/EventChatbot.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Event Oracle");
            stage.setScene(new Scene(root, 760, 300));
            stage.setResizable(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le chatbot: " + e.getMessage());
        }
    }

    @FXML
    private void handleGoBack() {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/EventSelector.fxml");
        }
    }

    @FXML
    void handleAddEvent(ActionEvent event) {
        EventNavigationState.clearEditingEvent();
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/AddEvent.fxml");
        }
    }

    @FXML
    void handleExportPdf(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Events PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("events-report.pdf");

        java.io.File file = fileChooser.showSaveDialog(tfSearch.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            List<Event> rows = new ArrayList<>(displayedEvents);
            PdfExportUtil.exportEvents(file.getAbsolutePath(), rows);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "PDF exporte avec succes.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'exporter le PDF: " + e.getMessage());
        }
    }

    private void loadEvents() {
        masterEvents.clear();
        try {
            masterEvents.addAll(eventService.getAll());
            applyDynamicFilterAndSort();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void renderEvents() {
        eventsContainer.getChildren().clear();
        if (displayedEvents.isEmpty()) {
            Label empty = new Label("No events found.");
            empty.setStyle("-fx-text-fill: #9ea3b0; -fx-font-size: 14px;");
            eventsContainer.getChildren().add(empty);
            return;
        }

        for (Event event : displayedEvents) {
            eventsContainer.getChildren().add(createEventCard(event));
        }
    }

    private HBox createEventCard(Event event) {
        Label idLabel = new Label("#" + event.getId());
        idLabel.setStyle("-fx-text-fill: #d6b2fc; -fx-font-weight: 700;");
        idLabel.setMinWidth(56);
        idLabel.setPrefWidth(56);

        Label titleLabel = new Label(truncate(safeDisplay(event.getTitle()), 36));
        titleLabel.setStyle("-fx-text-fill: #f3eefc; -fx-font-size: 14px; -fx-font-weight: 700;");
        titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        titleLabel.setMaxWidth(260);

        Label descriptionLabel = new Label(truncate(safeDisplay(event.getDescription()), 52));
        descriptionLabel.setStyle("-fx-text-fill: #9ea3b0; -fx-font-size: 12px;");
        descriptionLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        descriptionLabel.setMaxWidth(300);
        VBox identityBox = new VBox(2, titleLabel, descriptionLabel);
        identityBox.setMinWidth(220);
        identityBox.setPrefWidth(260);
        identityBox.setMaxWidth(300);

        Label scheduleLabel = new Label(formatTimestamp(event.getStartDate()) + " -> " + formatTimestamp(event.getEndDate()));
        scheduleLabel.setStyle("-fx-text-fill: #9ea3b0;");
        scheduleLabel.setMinWidth(190);
        scheduleLabel.setPrefWidth(210);
        scheduleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        Label locationLabel = new Label(truncate(safeDisplay(event.getLocation()), 24) + " (" + truncate(safeDisplay(event.getLocationType()), 12) + ")");
        locationLabel.setStyle("-fx-text-fill: #9ea3b0;");
        locationLabel.setMinWidth(150);
        locationLabel.setPrefWidth(180);
        locationLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        Label categoryLabel = new Label(truncate(safeDisplay(event.getCategoryName()), 16));
        categoryLabel.setStyle("-fx-text-fill: #c8b3ff; -fx-font-weight: 700;");
        categoryLabel.setMinWidth(110);
        categoryLabel.setPrefWidth(120);
        categoryLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        Label statusLabel = new Label(truncate(safeDisplay(event.getStatus()), 12));
        statusLabel.setStyle("-fx-text-fill: #c8b3ff; -fx-font-weight: 700;");
        statusLabel.setMinWidth(70);
        statusLabel.setPrefWidth(80);
        statusLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("edit-button");
        btnEdit.setOnAction(actionEvent -> openEditEvent(event));

        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("delete-button");
        btnDelete.setOnAction(actionEvent -> deleteEvent(event));

        HBox actionsBox = new HBox(8, btnEdit, btnDelete);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);
        actionsBox.setMinWidth(170);
        actionsBox.setPrefWidth(170);
        actionsBox.setMaxWidth(170);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(14, idLabel, identityBox, scheduleLabel, locationLabel, categoryLabel, statusLabel, spacer, actionsBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("user-row-card");
        return row;
    }

    private void openEditEvent(Event event) {
        EventNavigationState.setEditingEvent(event);
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/EditEvent.fxml");
        }
    }

    private void deleteEvent(Event event) {
        try {
            eventService.delete(event);
            loadEvents();
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Evenement supprime avec succes.");
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(timestamp);
    }

    private String safeDisplay(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "-";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
