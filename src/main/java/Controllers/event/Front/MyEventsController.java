package Controllers.event.Front;

import Entities.User.User;
import Entities.event.Reservation;
import Services.event.ReservationService;
import Utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class MyEventsController implements Initializable {

    @FXML
    private TableView<Reservation> reservationTable;
    @FXML
    private TableColumn<Reservation, Integer> colId;
    @FXML
    private TableColumn<Reservation, String> colEvent;
    @FXML
    private TableColumn<Reservation, String> colReservedAt;
    @FXML
    private TableColumn<Reservation, String> colStatus;
    @FXML
    private TableColumn<Reservation, String> colActions;

    private final ReservationService reservationService = new ReservationService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEvent.setCellValueFactory(new PropertyValueFactory<>("eventTitle"));
        colReservedAt.setCellValueFactory(new PropertyValueFactory<>("reservedAt"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusLabel"));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnTicket = new Button("Ticket PDF");
            private final Button btnIcs = new Button("ICS");
            private final HBox box = new HBox(8, btnTicket, btnIcs);

            {
                btnTicket.getStyleClass().add("secondary-button");
                btnIcs.getStyleClass().add("glow-button");

                btnTicket.setOnAction(event -> downloadTicket(getTableView().getItems().get(getIndex())));
                btnIcs.setOnAction(event -> downloadIcs(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        loadRows();
    }

    @FXML
    private void handleBackHome() {
        loadPage("/HomeFront.fxml");
    }

    @FXML
    private void handleBackToEvents() {
        loadPage("/event/Front/EventFront.fxml");
    }

    @FXML
    private void handleOpenReclamations() {
        loadPage("/event/Front/EventReclamationFront.fxml");
    }

    private void loadRows() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Authentication", "Please login to access this page.");
            return;
        }

        reservationTable.setItems(FXCollections.observableArrayList(
                reservationService.getAcceptedReservationsForUser(user.getId())
        ));
    }

    private void downloadTicket(Reservation reservation) {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Authentication", "Please login.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Ticket PDF");
        chooser.setInitialFileName("ticket-" + reservation.getId() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        java.io.File file = chooser.showSaveDialog(reservationTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            reservationService.generateTicketPdf(reservation.getId(), user, Path.of(file.getAbsolutePath()));
            showAlert(Alert.AlertType.INFORMATION, "Success", "Ticket downloaded.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    private void downloadIcs(Reservation reservation) {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Authentication", "Please login.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Calendar File");
        chooser.setInitialFileName("event-" + reservation.getId() + ".ics");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ICS files", "*.ics"));
        java.io.File file = chooser.showSaveDialog(reservationTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            reservationService.generateIcs(reservation.getId(), user, Path.of(file.getAbsolutePath()));
            showAlert(Alert.AlertType.INFORMATION, "Success", "ICS downloaded.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
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
            reservationTable.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation", e.getMessage());
        }
    }
}
