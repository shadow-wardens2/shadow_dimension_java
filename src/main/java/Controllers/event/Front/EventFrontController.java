package Controllers.event.Front;

import Entities.event.Event;
import Entities.event.EventRatingSummary;
import Services.event.EventService;
import Services.event.ReviewService;
import Utils.SessionManager;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class EventFrontController implements Initializable {

    @FXML
    private TableView<Event> eventTable;
    @FXML
    private TableColumn<Event, String> colTitle;
    @FXML
    private TableColumn<Event, String> colLocation;
    @FXML
    private TableColumn<Event, String> colDate;
    @FXML
    private TableColumn<Event, String> colRating;
    @FXML
    private TableColumn<Event, String> colActions;
    @FXML
    private TextField tfSearch;
    @FXML
    private ComboBox<String> cbSort;
    @FXML
    private Button btnMyEvents;

    private final EventService eventService = new EventService();
    private final ReviewService reviewService = new ReviewService();
    private final ObservableList<Event> events = FXCollections.observableArrayList();
    private FilteredList<Event> filtered;
    private final Map<Integer, EventRatingSummary> ratingCache = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colDate.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue().getStartDate())));
        colRating.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatRating(cell.getValue().getId())));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button("Details");
            private final HBox box = new HBox(8, btnDetails);

            {
                btnDetails.getStyleClass().add("secondary-button");

                btnDetails.setOnAction(event -> openDetails(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        filtered = new FilteredList<>(events, e -> true);
        eventTable.setItems(filtered);

        cbSort.setItems(FXCollections.observableArrayList("Start Date", "Title A-Z", "Title Z-A"));
        cbSort.getSelectionModel().selectFirst();

        tfSearch.textProperty().addListener((obs, o, n) -> applyFilter());
        cbSort.valueProperty().addListener((obs, o, n) -> applySort());

        boolean loggedIn = SessionManager.isLoggedIn();
        btnMyEvents.setDisable(!loggedIn);

        loadData();
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

    private void loadData() {
        events.clear();
        ratingCache.clear();
        try {
            List<Event> all = eventService.getAll();
            for (Event event : all) {
                if (event.getStatus() != null && "ACTIVE".equalsIgnoreCase(event.getStatus())) {
                    events.add(event);
                    ratingCache.put(event.getId(), reviewService.getRatingSummary(event.getId()));
                }
            }
            applyFilter();
            applySort();
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
            eventTable.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    private void applyFilter() {
        String search = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase(Locale.ROOT);
        filtered.setPredicate(event -> {
            if (search.isBlank()) {
                return true;
            }
            return safe(event.getTitle()).contains(search)
                    || safe(event.getLocation()).contains(search)
                    || safe(event.getDescription()).contains(search);
        });
    }

    private void applySort() {
        String sort = cbSort.getValue();
        if ("Title A-Z".equals(sort)) {
            filtered.getSource().sort((a, b) -> safe(a.getTitle()).compareTo(safe(b.getTitle())));
        } else if ("Title Z-A".equals(sort)) {
            filtered.getSource().sort((a, b) -> safe(b.getTitle()).compareTo(safe(a.getTitle())));
        } else {
            filtered.getSource().sort((a, b) -> a.getStartDate().compareTo(b.getStartDate()));
        }
    }

    private String formatRating(int eventId) {
        EventRatingSummary stats = ratingCache.get(eventId);
        if (stats == null || stats.getTotalReviews() == 0) {
            return "No reviews";
        }
        return String.format(Locale.ROOT, "%.1f (%d)", stats.getAverageRating(), stats.getTotalReviews());
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

    private void loadPage(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            eventTable.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }
}
