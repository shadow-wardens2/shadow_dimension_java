package Controllers.event;

// Host contract used to swap center pages in the dashboard layout.
import Controllers.Marketplace.PageHost;
// Event entity displayed and manipulated by this controller.
import Entities.event.Event;
// Service layer used for event CRUD and retrieval.
import Services.event.EventService;
// Shared navigation state used to pass edited entity between pages.
import Utils.EventNavigationState;
// Utility used to export current list to styled PDF.
import Utils.PdfExportUtil;
// Utility used for voice-to-text search input.
import Utils.VoiceToTextUtil;
// Factory helpers for observable collection creation.
import javafx.collections.FXCollections;
// Observable list type bound to JavaFX UI collections.
import javafx.collections.ObservableList;
// Action event type for button handlers.
import javafx.event.ActionEvent;
// Injects nodes defined with fx:id in FXML.
import javafx.fxml.FXML;
// Loads additional FXML views when needed.
import javafx.fxml.FXMLLoader;
// Lifecycle interface allowing initialize callback.
import javafx.fxml.Initializable;
// Alignment helpers for row/action layout.
import javafx.geometry.Pos;
// Base JavaFX node type used as loaded root.
import javafx.scene.Parent;
// Scene container for popup windows.
import javafx.scene.Scene;
// Standard JavaFX alert dialog.
import javafx.scene.control.Alert;
// JavaFX button control.
import javafx.scene.control.Button;
// JavaFX combo box control.
import javafx.scene.control.ComboBox;
// JavaFX label control.
import javafx.scene.control.Label;
// Controls ellipsis behavior when text is too long.
import javafx.scene.control.OverrunStyle;
// JavaFX text field control.
import javafx.scene.control.TextField;
// Horizontal layout container.
import javafx.scene.layout.HBox;
// Layout grow-priority utility.
import javafx.scene.layout.Priority;
// Flexible empty node used as spacer.
import javafx.scene.layout.Region;
// Vertical layout container.
import javafx.scene.layout.VBox;
// Stage window class (used for chatbot popup).
import javafx.stage.Stage;

// IO exception for FXML load operations.
import java.io.IOException;
// URL type from Initializable signature.
import java.net.URL;
// SQL checked exception from service calls.
import java.sql.SQLException;
// Timestamp type for date formatting helper.
import java.sql.Timestamp;
// Date formatter used to display start/end dates in cards.
import java.text.SimpleDateFormat;
// Dynamic list implementation used for export snapshot.
import java.util.ArrayList;
// Comparator used for sorting options.
import java.util.Comparator;
// List interface.
import java.util.List;
// Locale used for safe lower-casing.
import java.util.Locale;
// Resource bundle from Initializable signature.
import java.util.ResourceBundle;
// Async utility for non-blocking voice recognition.
import java.util.concurrent.CompletableFuture;
// File chooser used by PDF export flow.
import javafx.stage.FileChooser;

// Controller for the Event management page.
public class EventController implements Initializable {

    // Reference to dashboard host used for in-page navigation.
    private PageHost dashboardContext;

    // Container where event cards are dynamically rendered.
    @FXML
    private VBox eventsContainer;
    // Search text field used for live filtering.
    @FXML
    private TextField tfSearch;
    // Sort selector used to choose ordering strategy.
    @FXML
    private ComboBox<String> cbSort;
    // Voice button used to trigger speech recognition.
    @FXML
    private Button btnMic;

    // Service instance that communicates with database layer.
    private final EventService eventService = new EventService();
    // Complete event dataset loaded from backend.
    private final ObservableList<Event> masterEvents = FXCollections.observableArrayList();
    // Filtered/sorted subset currently displayed in UI.
    private final ObservableList<Event> displayedEvents = FXCollections.observableArrayList();

    // Called by host to inject page navigation dependency.
    public void setDashboardContext(PageHost dashboardContext) {
        // Stores host reference for later page transitions.
        this.dashboardContext = dashboardContext;
    }

    // JavaFX lifecycle callback executed after FXML injection.
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initializes filter/sort controls and listeners.
        setupSearchAndSort();
        // Loads latest events from service and renders view.
        loadEvents();
    }

    // Configures sorting options and reactive filtering listeners.
    private void setupSearchAndSort() {
        // Populates sort combobox with available sorting modes.
        cbSort.setItems(FXCollections.observableArrayList(
                "Newest first",
                "Oldest first",
                "Title A-Z",
                "Title Z-A",
                "Capacity high-low",
                "Capacity low-high"
        ));
        // Selects default sorting mode on page load.
        cbSort.getSelectionModel().select("Newest first");

        // Re-applies filtering and sorting whenever search text changes.
        tfSearch.textProperty().addListener((obs, oldValue, newValue) -> applyDynamicFilterAndSort());
        // Re-applies filtering and sorting whenever sort mode changes.
        cbSort.valueProperty().addListener((obs, oldValue, newValue) -> applyDynamicFilterAndSort());

        // Runs initial render pass using defaults.
        applyDynamicFilterAndSort();
    }

    // Applies current search + sort criteria then refreshes card rendering.
    private void applyDynamicFilterAndSort() {
        // Normalizes search input to lowercase for case-insensitive matching.
        String search = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase(Locale.ROOT);

        // Clears old rendered dataset before rebuilding it.
        displayedEvents.clear();
        // Iterates over all loaded events.
        for (Event event : masterEvents) {
            // Fast path: when search is empty, keep all events.
            if (search.isEmpty()) {
                // Adds event without additional checks.
                displayedEvents.add(event);
                // Skips remaining checks for this row.
                continue;
            }

            // Builds searchable lowercase fields from event data.
            String title = safeLower(event.getTitle());
            String description = safeLower(event.getDescription());
            String location = safeLower(event.getLocation());
            String category = safeLower(event.getCategoryName());
            String status = safeLower(event.getStatus());
            String locationType = safeLower(event.getLocationType());
            String capacity = String.valueOf(event.getCapacity());

                // True when any field contains search token.
            boolean match = title.contains(search)
                    || description.contains(search)
                    || location.contains(search)
                    || category.contains(search)
                    || status.contains(search)
                    || locationType.contains(search)
                    || capacity.contains(search);

            // Keeps matching event in displayed dataset.
            if (match) {
                displayedEvents.add(event);
            }
        }

        // Reads selected sort strategy.
        String selectedSort = cbSort.getValue();
        // Comparator variable selected by strategy.
        Comparator<Event> comparator;

        // Oldest first by ascending id.
        if ("Oldest first".equals(selectedSort)) {
            comparator = Comparator.comparingInt(Event::getId);
        // Alphabetical title ascending.
        } else if ("Title A-Z".equals(selectedSort)) {
            comparator = Comparator.comparing(e -> safeLower(e.getTitle()));
        // Alphabetical title descending.
        } else if ("Title Z-A".equals(selectedSort)) {
            comparator = Comparator.comparing((Event e) -> safeLower(e.getTitle())).reversed();
        // Capacity descending.
        } else if ("Capacity high-low".equals(selectedSort)) {
            comparator = Comparator.comparingInt(Event::getCapacity).reversed();
        // Capacity ascending.
        } else if ("Capacity low-high".equals(selectedSort)) {
            comparator = Comparator.comparingInt(Event::getCapacity);
        // Default: newest first by descending id.
        } else {
            comparator = Comparator.comparingInt(Event::getId).reversed();
        }

        // Applies sort to currently displayed subset.
        displayedEvents.sort(comparator);
        // Rebuilds visible cards after filtering/sorting.
        renderEvents();
    }

    // Handles voice button click to fill search field from microphone.
    @FXML
    private void handleVoiceSearch() {
        // Prevents repeated clicks while recognition is running.
        btnMic.setDisable(true);

        // Runs speech recognition off UI thread.
        CompletableFuture.supplyAsync(() -> {
            try {
                // Captures one speech phrase with timeout.
                return VoiceToTextUtil.recognizeOnce(6);
            } catch (Exception e) {
                // Returns empty string on any recognition failure.
                return "";
            }
        // Switches back to UI thread to update controls safely.
        }).thenAccept(text -> javafx.application.Platform.runLater(() -> {
            // Re-enables button once recognition completed.
            btnMic.setDisable(false);
            // Writes recognized text into search field when available.
            if (text != null && !text.isBlank()) {
                tfSearch.setText(text);
            } else {
                // Informs user that nothing was recognized.
                showAlert(Alert.AlertType.INFORMATION, "Microphone", "Aucune voix detectee. Reessayez.");
            }
        }));
    }

    // Opens chatbot window dedicated to event assistance.
    @FXML
    private void handleOpenChatbot() {
        try {
            // Loads chatbot FXML view.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/event/EventChatbot.fxml"));
            // Creates root node from FXML.
            Parent root = loader.load();

            // Creates separate stage for chatbot utility.
            Stage stage = new Stage();
            // Sets window title.
            stage.setTitle("Event Oracle");
            // Uses compact horizontal scene size.
            stage.setScene(new Scene(root, 760, 300));
            // Allows user to resize chatbot window.
            stage.setResizable(true);
            // Displays chatbot stage.
            stage.show();
        } catch (IOException e) {
            // Displays any loading/opening error.
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le chatbot: " + e.getMessage());
        }
    }

    // Navigates back to event selector page.
    @FXML
    private void handleGoBack() {
        // Only navigates when host context is available.
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/EventSelector.fxml");
        }
    }

    // Opens add-event form inside dashboard content area.
    @FXML
    void handleAddEvent(ActionEvent event) {
        // Clears previous edit state to ensure add mode.
        EventNavigationState.clearEditingEvent();
        // Routes to AddEvent page through host.
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/AddEvent.fxml");
        }
    }

    // Exports currently displayed rows (post filter/sort) to PDF.
    @FXML
    void handleExportPdf(ActionEvent event) {
        // Creates standard save dialog.
        FileChooser fileChooser = new FileChooser();
        // Sets dialog title.
        fileChooser.setTitle("Export Events PDF");
        // Restricts output extension to PDF.
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        // Suggests a default output filename.
        fileChooser.setInitialFileName("events-report.pdf");

        // Opens save dialog attached to current page window.
        java.io.File file = fileChooser.showSaveDialog(tfSearch.getScene().getWindow());
        // Stops when user cancels dialog.
        if (file == null) {
            return;
        }

        try {
            // Copies current displayed list into plain list for export.
            List<Event> rows = new ArrayList<>(displayedEvents);
            // Delegates PDF creation to utility class.
            PdfExportUtil.exportEvents(file.getAbsolutePath(), rows);
            // Confirms successful export.
            showAlert(Alert.AlertType.INFORMATION, "Succes", "PDF exporte avec succes.");
        } catch (Exception e) {
            // Reports export failure details.
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'exporter le PDF: " + e.getMessage());
        }
    }

    // Reloads full event list from service and refreshes display.
    private void loadEvents() {
        // Clears existing cached rows.
        masterEvents.clear();
        try {
            // Fetches events from backend service.
            masterEvents.addAll(eventService.getAll());
            // Re-applies active search/sort and rerenders.
            applyDynamicFilterAndSort();
        } catch (SQLException e) {
            // Shows backend error to user.
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // Regenerates visual event cards based on displayedEvents list.
    private void renderEvents() {
        // Removes previously rendered cards.
        eventsContainer.getChildren().clear();
        // Handles empty state.
        if (displayedEvents.isEmpty()) {
            // Empty-state message node.
            Label empty = new Label("No events found.");
            // Inline style for subdued empty-state appearance.
            empty.setStyle("-fx-text-fill: #9ea3b0; -fx-font-size: 14px;");
            // Renders empty-state label.
            eventsContainer.getChildren().add(empty);
            // Stops rendering normal cards.
            return;
        }

        // Creates one card row per displayed event.
        for (Event event : displayedEvents) {
            eventsContainer.getChildren().add(createEventCard(event));
        }
    }

    // Builds one styled row card with event data and actions.
    private HBox createEventCard(Event event) {
        // ID badge at row start.
        Label idLabel = new Label("#" + event.getId());
        idLabel.setStyle("-fx-text-fill: #d6b2fc; -fx-font-weight: 700;");
        idLabel.setMinWidth(56);
        idLabel.setPrefWidth(56);

        // Title label with truncation.
        Label titleLabel = new Label(truncate(safeDisplay(event.getTitle()), 36));
        titleLabel.setStyle("-fx-text-fill: #f3eefc; -fx-font-size: 14px; -fx-font-weight: 700;");
        titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        titleLabel.setMaxWidth(260);

        // Secondary description text.
        Label descriptionLabel = new Label(truncate(safeDisplay(event.getDescription()), 52));
        descriptionLabel.setStyle("-fx-text-fill: #9ea3b0; -fx-font-size: 12px;");
        descriptionLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        descriptionLabel.setMaxWidth(300);
        // Groups title + description vertically.
        VBox identityBox = new VBox(2, titleLabel, descriptionLabel);
        identityBox.setMinWidth(220);
        identityBox.setPrefWidth(260);
        identityBox.setMaxWidth(300);

        // Displays start/end schedule range.
        Label scheduleLabel = new Label(formatTimestamp(event.getStartDate()) + " -> " + formatTimestamp(event.getEndDate()));
        scheduleLabel.setStyle("-fx-text-fill: #9ea3b0;");
        scheduleLabel.setMinWidth(190);
        scheduleLabel.setPrefWidth(210);
        scheduleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        // Displays location with location type.
        Label locationLabel = new Label(truncate(safeDisplay(event.getLocation()), 24) + " (" + truncate(safeDisplay(event.getLocationType()), 12) + ")");
        locationLabel.setStyle("-fx-text-fill: #9ea3b0;");
        locationLabel.setMinWidth(150);
        locationLabel.setPrefWidth(180);
        locationLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        // Category badge text.
        Label categoryLabel = new Label(truncate(safeDisplay(event.getCategoryName()), 16));
        categoryLabel.setStyle("-fx-text-fill: #c8b3ff; -fx-font-weight: 700;");
        categoryLabel.setMinWidth(110);
        categoryLabel.setPrefWidth(120);
        categoryLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        // Status badge text.
        Label statusLabel = new Label(truncate(safeDisplay(event.getStatus()), 12));
        statusLabel.setStyle("-fx-text-fill: #c8b3ff; -fx-font-weight: 700;");
        statusLabel.setMinWidth(70);
        statusLabel.setPrefWidth(80);
        statusLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        // Edit action button.
        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("edit-button");
        // Opens edit page for selected event.
        btnEdit.setOnAction(actionEvent -> openEditEvent(event));

        // Delete action button.
        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("delete-button");
        // Deletes selected event and refreshes list.
        btnDelete.setOnAction(actionEvent -> deleteEvent(event));

        // Groups row action buttons.
        HBox actionsBox = new HBox(8, btnEdit, btnDelete);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);
        actionsBox.setMinWidth(170);
        actionsBox.setPrefWidth(170);
        actionsBox.setMaxWidth(170);

        // Flexible spacer to push actions to right edge.
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Final assembled row with all visual segments.
        HBox row = new HBox(14, idLabel, identityBox, scheduleLabel, locationLabel, categoryLabel, statusLabel, spacer, actionsBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("user-row-card");
        // Returns fully configured row card.
        return row;
    }

    // Saves selected event into navigation state then opens edit form.
    private void openEditEvent(Event event) {
        // Stores current event for next controller consumption.
        EventNavigationState.setEditingEvent(event);
        // Navigates to edit page inside dashboard.
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/EditEvent.fxml");
        }
    }

    // Deletes one event from persistence and refreshes UI list.
    private void deleteEvent(Event event) {
        try {
            // Executes deletion via service layer.
            eventService.delete(event);
            // Reloads data and reapplies filters/sort.
            loadEvents();
            // Confirms successful deletion.
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Evenement supprime avec succes.");
        } catch (SQLException ex) {
            // Displays deletion error from backend.
            showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
    }

    // Formats timestamp for compact display in row card.
    private String formatTimestamp(Timestamp timestamp) {
        // Returns placeholder when timestamp missing.
        if (timestamp == null) {
            return "-";
        }
        // Formats as yyyy-MM-dd HH:mm.
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(timestamp);
    }

    // Returns safe non-empty text for display cells.
    private String safeDisplay(String value) {
        // Uses placeholder when null/blank.
        return value == null || value.isBlank() ? "-" : value;
    }

    // Truncates long strings and appends ellipsis for compact cards.
    private String truncate(String value, int maxLength) {
        // Uses placeholder when value is null.
        if (value == null) {
            return "-";
        }
        // Returns original text when within limit.
        if (value.length() <= maxLength) {
            return value;
        }
        // Returns shortened text with trailing ellipsis.
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    // Lowercases text safely for case-insensitive comparisons.
    private String safeLower(String value) {
        // Returns empty string when source value is null.
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    // Small helper to show unified alerts in this page.
    private void showAlert(Alert.AlertType type, String title, String message) {
        // Builds alert instance using provided type.
        Alert alert = new Alert(type);
        // Sets popup title.
        alert.setTitle(title);
        // Hides header for cleaner UI.
        alert.setHeaderText(null);
        // Sets alert body message.
        alert.setContentText(message);
        // Displays modal popup and waits for close.
        alert.showAndWait();
    }
}
