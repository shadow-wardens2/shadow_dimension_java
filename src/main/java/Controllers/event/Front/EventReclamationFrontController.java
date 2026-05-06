package Controllers.event.Front;

import Entities.User.User;
import Entities.event.EventReclamation;
import Entities.event.Reservation;
import Services.event.EventReclamationService;
import Services.event.ReservationService;
import Utils.SessionManager;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class EventReclamationFrontController implements Initializable {

    @FXML
    private ComboBox<String> cbEvent;
    @FXML
    private TextField tfSubject;
    @FXML
    private TextArea taClaim;
    @FXML
    private TableView<EventReclamation> reclamationTable;
    @FXML
    private TableColumn<EventReclamation, Integer> colId;
    @FXML
    private TableColumn<EventReclamation, String> colEvent;
    @FXML
    private TableColumn<EventReclamation, String> colStatus;
    @FXML
    private TableColumn<EventReclamation, String> colCreatedAt;
    @FXML
    private TableColumn<EventReclamation, String> colMessage;
    @FXML
    private TableColumn<EventReclamation, String> colAdminResponse;
    @FXML
    private TableColumn<EventReclamation, String> colActions;
    @FXML
    private Label lbHint;

    private final ReservationService reservationService = new ReservationService();
    private final EventReclamationService reclamationService = new EventReclamationService();
    private final Map<String, Integer> eventByLabel = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (SessionManager.getCurrentUser() == null) {
            showAlert(Alert.AlertType.WARNING, "Authentication", "Please login first.");
            return;
        }

        colId.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createObjectBinding(cell.getValue()::getId));
        colEvent.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getEventTitle()));
        colStatus.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getStatusLabel()));
        colCreatedAt.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue().getCreatedAt())));
        colMessage.setCellValueFactory(cell -> new ReadOnlyStringWrapper(valueOrDash(cell.getValue().getMessage())));
        colAdminResponse.setCellValueFactory(cell -> new ReadOnlyStringWrapper(valueOrDash(cell.getValue().getAdminResponse())));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button("Details");
            private final Button btnEscalate = new Button("Escalate");
            private final HBox box = new HBox(8, btnDetails, btnEscalate);

            {
                btnDetails.getStyleClass().add("secondary-button");
                btnEscalate.getStyleClass().add("secondary-button");
                
                btnDetails.setOnAction(event -> openDetails(getTableView().getItems().get(getIndex())));
                btnEscalate.setOnAction(event -> handleEscalate(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                EventReclamation row = getTableView().getItems().get(getIndex());
                boolean canEscalate = row.canEscalate();
                btnEscalate.setManaged(canEscalate);
                btnEscalate.setVisible(canEscalate);
                setGraphic(box);
            }
        });

        reclamationTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !reclamationTable.getSelectionModel().isEmpty()) {
                openDetails(reclamationTable.getSelectionModel().getSelectedItem());
            }
        });

        loadAcceptedEvents();
        loadMyRows();
    }

    @FXML
    private void handleSubmit() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Authentication", "Please login first.");
            return;
        }

        String selectedEvent = cbEvent.getValue();
        if (selectedEvent == null || selectedEvent.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Select an accepted event first.");
            return;
        }

        Integer eventId = eventByLabel.get(selectedEvent);
        if (eventId == null || eventId <= 0) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Invalid event selected.");
            return;
        }

        try {
            EventReclamation created = reclamationService.create(user.getId(), eventId, tfSubject.getText(), taClaim.getText());
            tfSubject.clear();
            taClaim.clear();
            loadMyRows();

            showAiResponseDialog(created);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Reclamation", e.getMessage());
        }
    }

    private void showAiResponseDialog(EventReclamation reclamation) {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("AI Response Generated");
        dialog.setHeaderText("Your Reclamation - AI Analysis");
        dialog.setContentText("AI Response:\n\n" + (reclamation.getAiResponse() != null ? reclamation.getAiResponse() : "No response available"));
        dialog.getDialogPane().setStyle("-fx-font-size: 12;");

        javafx.scene.control.ButtonType escalateBtn = new javafx.scene.control.ButtonType("Escalate to Admin", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType acceptBtn = new javafx.scene.control.ButtonType("Accept Response", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getButtonTypes().setAll(escalateBtn, acceptBtn);

        java.util.Optional<javafx.scene.control.ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == escalateBtn) {
            handleEscalateFromDialog(reclamation);
        }
    }

    private void handleEscalateFromDialog(EventReclamation reclamation) {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Authentication", "Please login first.");
            return;
        }
        try {
            reclamationService.escalate(reclamation.getId(), user);
            loadMyRows();
            showAlert(Alert.AlertType.INFORMATION, "Escalated", "Your reclamation has been escalated to admin. They will review and respond shortly.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Escalation", e.getMessage());
        }
    }

    @FXML
    private void handleBackHome() {
        loadPage("/HomeFront.fxml");
    }

    @FXML
    private void handleBackEvents() {
        loadPage("/event/Front/EventFront.fxml");
    }

    @FXML
    private void handleBackMyEvents() {
        loadPage("/event/Front/MyEvents.fxml");
    }

    private void loadAcceptedEvents() {
        User user = SessionManager.getCurrentUser();
        List<Reservation> reservations = reservationService.getAcceptedReservationsForUser(user.getId());

        eventByLabel.clear();
        cbEvent.getItems().clear();
        for (Reservation reservation : reservations) {
            String label = reservation.getEventTitle() + " (#" + reservation.getEventId() + ")";
            eventByLabel.put(label, reservation.getEventId());
            cbEvent.getItems().add(label);
        }

        if (!cbEvent.getItems().isEmpty()) {
            cbEvent.getSelectionModel().selectFirst();
            lbHint.setText("Only accepted reservations can be reclaimed.");
        } else {
            lbHint.setText("No accepted reservations available for reclamation.");
        }
    }

    private void loadMyRows() {
        User user = SessionManager.getCurrentUser();
        List<EventReclamation> rows = reclamationService.findMyReclamations(user.getId());
        reclamationTable.setItems(FXCollections.observableArrayList(rows));
    }

    private void handleEscalate(EventReclamation reclamation) {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Authentication", "Please login first.");
            return;
        }

        try {
            reclamationService.escalate(reclamation.getId(), user);
            loadMyRows();
            showAlert(Alert.AlertType.INFORMATION, "Escalation", "Reclamation escalated.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Escalation", e.getMessage());
        }
    }

    private void openDetails(EventReclamation reclamation) {
        if (reclamation == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/event/Front/EventReclamationDetailsFront.fxml"));
            Parent root = loader.load();
            EventReclamationDetailsFrontController controller = loader.getController();
            controller.setReclamation(reclamation);
            reclamationTable.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load details: " + e.getMessage());
        }
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void loadPage(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            reclamationTable.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation", e.getMessage());
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
