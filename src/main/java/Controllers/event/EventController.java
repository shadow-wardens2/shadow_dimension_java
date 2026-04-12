package Controllers.event;

import Entities.event.Event;
import Services.event.EventService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ResourceBundle;

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

    private final EventService eventService = new EventService();
    private final ObservableList<Event> observableEvents = FXCollections.observableArrayList();

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

    private void loadEvents() {
        observableEvents.clear();
        try {
            observableEvents.addAll(eventService.getAll());
            eventTable.setItems(observableEvents);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
