package Controllers.event.Front;

import Entities.event.Event;
import Services.event.EventService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import Utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class EventFrontController implements Initializable {

    @FXML
    private TilePane eventTilePane;
    @FXML
    private TextField tfSearch;
    @FXML
    private ComboBox<String> cbSort;
    @FXML
    private Button btnMyEvents;

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

        boolean loggedIn = SessionManager.isLoggedIn();
        btnMyEvents.setDisable(!loggedIn);
    }

    private void setupSearchAndSort() {
        filteredEvents = new FilteredList<>(observableEvents, e -> true);
        sortedEvents = new SortedList<>(filteredEvents);

        cbSort.setItems(FXCollections.observableArrayList(
                "Start Date",
                "Title A-Z",
                "Title Z-A"
        ));
        cbSort.getSelectionModel().selectFirst();

        tfSearch.textProperty().addListener((obs, oldValue, newValue) -> applyDynamicFilterAndSort());
        cbSort.valueProperty().addListener((obs, oldValue, newValue) -> applyDynamicFilterAndSort());

        applyDynamicFilterAndSort();
    }

    private void applyDynamicFilterAndSort() {
        String search = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase(Locale.ROOT);

        filteredEvents.setPredicate(event -> {
            if (search.isEmpty()) return true;
            return safe(event.getTitle()).contains(search)
                    || safe(event.getLocation()).contains(search)
                    || safe(event.getDescription()).contains(search);
        });

        String selectedSort = cbSort.getValue();
        Comparator<Event> comparator;

        if ("Title A-Z".equals(selectedSort)) {
            comparator = Comparator.comparing(e -> safe(e.getTitle()));
        } else if ("Title Z-A".equals(selectedSort)) {
            comparator = Comparator.comparing((Event e) -> safe(e.getTitle())).reversed();
        } else {
            // Default: Start Date
            comparator = Comparator.comparing(e -> e.getStartDate() == null ? "" : e.getStartDate().toString());
        }

        sortedEvents.setComparator(comparator);
    }

    private void refreshGrid() {
        if (eventTilePane == null) return;

        eventTilePane.getChildren().clear();
        for (Event event : sortedEvents) {
            Parent card = cardCache.get(event);
            if (card == null) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/event/Front/EventCardFront.fxml"));
                    card = loader.load();
                    EventCardFrontController controller = loader.getController();
                    controller.setEventData(event, this::openDetails);
                    cardCache.put(event, card);
                } catch (IOException e) {
                    continue;
                }
            }
            eventTilePane.getChildren().add(card);
        }
    }

    private void loadEvents() {
        observableEvents.clear();
        cardCache.clear();
        try {
            eventService.getAll().stream()
                    .filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus()))
                    .forEach(observableEvents::add);
            applyDynamicFilterAndSort();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    private void openDetails(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/event/Front/EventDetailsFront.fxml"));
            Parent root = loader.load();
            EventDetailsFrontController controller = loader.getController();
            controller.setEvent(event);
            eventTilePane.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    @FXML
    private void handleMyEvents() {
        if (!SessionManager.isLoggedIn()) {
            showAlert(Alert.AlertType.WARNING, "Authentication", "Please login to view your accepted reservations.");
            return;
        }
        loadPage("/event/Front/MyEvents.fxml");
    }

    @FXML
    private void handleBackHome() {
        loadPage("/HomeFront.fxml");
    }

    private void loadPage(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            eventTilePane.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
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
