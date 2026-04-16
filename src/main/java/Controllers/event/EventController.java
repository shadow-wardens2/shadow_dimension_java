package Controllers.event;

import Entities.event.Event;
import Services.event.EventService;
import Utils.PdfExportUtil;
import Utils.VoiceToTextUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import javafx.stage.FileChooser;

public class EventController implements Initializable {

    @FXML
    private TableView<Event> eventTable;
    @FXML
    private TableColumn<Event, Integer> colId;
    @FXML
    private TableColumn<Event, String> colTitle;
    @FXML
    private TableColumn<Event, String> colDescription;
    @FXML
    private TableColumn<Event, String> colLocation;
    @FXML
    private TableColumn<Event, Timestamp> colStartDate;
    @FXML
    private TableColumn<Event, Timestamp> colEndDate;
    @FXML
    private TableColumn<Event, Integer> colCapacity;
    @FXML
    private TableColumn<Event, String> colCategory;
    @FXML
    private TableColumn<Event, String> colStatus;
    @FXML
    private TableColumn<Event, String> colLocationType;
    @FXML
    private TableColumn<Event, Integer> colActions;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colStartDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colEndDate.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colLocationType.setCellValueFactory(new PropertyValueFactory<>("locationType"));

        setupSearchAndSort();
        loadEvents();

        colActions.setCellFactory(param -> new TableCell<Event, Integer>() {
            private final Button btnUpdate = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(10, btnUpdate, btnDelete);

            {
                btnUpdate.getStyleClass().add("edit-button");
                btnDelete.getStyleClass().add("delete-button");

                btnDelete.setOnAction(event -> {
                    Event e = getTableView().getItems().get(getIndex());
                    try {
                        eventService.delete(e);
                        loadEvents();
                        showAlert(Alert.AlertType.INFORMATION, "Succes", "Evenement supprime avec succes.");
                    } catch (SQLException ex) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                    }
                });

                btnUpdate.setOnAction(event -> {
                    Event e = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/event/EditEvent.fxml"));
                        Parent root = loader.load();
                        EditEventController controller = loader.getController();
                        controller.setEvent(e);
                        Stage stage = new Stage();
                        stage.setTitle("Edit Event");
                        stage.setScene(new Scene(root));
                        stage.showAndWait();
                        loadEvents();
                    } catch (IOException ex) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                    }
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }

    private void setupSearchAndSort() {
        filteredEvents = new FilteredList<>(observableEvents, e -> true);
        sortedEvents = new SortedList<>(filteredEvents);
        eventTable.setItems(sortedEvents);

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

        java.io.File file = fileChooser.showSaveDialog(eventTable.getScene().getWindow());
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
