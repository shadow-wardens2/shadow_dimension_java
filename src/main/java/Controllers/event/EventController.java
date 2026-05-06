package Controllers.event;

import Controllers.event.Back.EventWeatherController;
import Entities.event.Event;
import Services.event.EventService;
import Utils.PdfExportUtil;
import Utils.VoiceToTextUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import javafx.stage.FileChooser;

public class EventController implements Initializable {

    @FXML
    private TilePane eventTilePane;
    @FXML
    private TextField tfSearch;
    @FXML
    private ComboBox<String> cbSort;
    @FXML
    private Button btnMic;

    private final EventService eventService = new EventService();
    private final ObservableList<Event> observableEvents = FXCollections.observableArrayList();
    private FilteredList<Event> filteredEvents;
    private SortedList<Event> sortedEvents;
    private final Map<Event, Parent> cardCache = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSearchAndSort();
        loadEvents();
        sortedEvents.addListener((ListChangeListener<Event>) c -> refreshGrid());
        refreshGrid();
    }

    private void setupSearchAndSort() {
        filteredEvents = new FilteredList<>(observableEvents, e -> true);
        sortedEvents = new SortedList<>(filteredEvents);

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

        filteredEvents.setPredicate(event -> {
            if (search.isEmpty()) {
                return true;
            }

            String title = safe(event.getTitle());
            String description = safe(event.getDescription());
            String location = safe(event.getLocation());
            String category = safe(event.getCategoryName());
            String status = safe(event.getStatus());
            String locationType = safe(event.getLocationType());
            String capacity = String.valueOf(event.getCapacity());

            return title.contains(search)
                    || description.contains(search)
                    || location.contains(search)
                    || category.contains(search)
                    || status.contains(search)
                    || locationType.contains(search)
                    || capacity.contains(search);
        });

        String selectedSort = cbSort.getValue();
        Comparator<Event> comparator;

        if ("Oldest first".equals(selectedSort)) {
            comparator = Comparator.comparingInt(Event::getId);
        } else if ("Title A-Z".equals(selectedSort)) {
            comparator = Comparator.comparing(e -> safe(e.getTitle()));
        } else if ("Title Z-A".equals(selectedSort)) {
            comparator = Comparator.comparing((Event e) -> safe(e.getTitle())).reversed();
        } else if ("Capacity high-low".equals(selectedSort)) {
            comparator = Comparator.comparingInt(Event::getCapacity).reversed();
        } else if ("Capacity low-high".equals(selectedSort)) {
            comparator = Comparator.comparingInt(Event::getCapacity);
        } else {
            comparator = Comparator.comparingInt(Event::getId).reversed();
        }

        sortedEvents.setComparator(comparator);
    }

    private void refreshGrid() {
        if (eventTilePane == null) {
            return;
        }

        eventTilePane.getChildren().clear();
        for (Event event : sortedEvents) {
            Parent card = cardCache.get(event);
            if (card == null) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/event/EventCard.fxml"));
                    card = loader.load();
                    EventCardController controller = loader.getController();
                    controller.setEventData(event, this::editEvent, this::deleteEvent, this::openWeather);
                    cardCache.put(event, card);
                } catch (IOException e) {
                    continue;
                }
            }
            eventTilePane.getChildren().add(card);
        }
    }

    private void editEvent(Event event) {
        if (event == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/event/EditEvent.fxml"));
            Parent root = loader.load();
            EditEventController controller = loader.getController();
            controller.setEvent(event);
            Stage stage = new Stage();
            stage.setTitle("Edit Event");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadEvents();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
    }

    private void deleteEvent(Event event) {
        if (event == null) {
            return;
        }

        try {
            eventService.delete(event);
            loadEvents();
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Evenement supprime avec succes.");
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
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

    private void openWeather(Event event) {
        if (event == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/event/Back/EventWeather.fxml"));
            Parent root = loader.load();
            EventWeatherController controller = loader.getController();
            controller.setEvent(event);

            Stage stage = new Stage();
            stage.setTitle("Event Weather");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la meteo: " + e.getMessage());
        }
    }

    @FXML
    void handleAddEvent(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/event/AddEvent.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add New Event");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadEvents();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
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
            List<Event> rows = new ArrayList<>(sortedEvents);
            PdfExportUtil.exportEvents(file.getAbsolutePath(), rows);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "PDF exporte avec succes.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'exporter le PDF: " + e.getMessage());
        }
    }

    private void loadEvents() {
        observableEvents.clear();
        cardCache.clear();
        try {
            observableEvents.addAll(eventService.getAll());
            applyDynamicFilterAndSort();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private String safe(String value) {
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
