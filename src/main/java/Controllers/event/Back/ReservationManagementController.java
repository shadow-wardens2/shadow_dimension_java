package Controllers.event.Back;

import Entities.User.User;
import Entities.event.Reservation;
import Entities.event.ReservationStatus;
import Services.event.ReservationService;
import Utils.SessionManager;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ReservationManagementController implements Initializable {

    @FXML
    private TableView<Reservation> reservationTable;
    @FXML
    private TableColumn<Reservation, Integer> colId;
    @FXML
    private TableColumn<Reservation, String> colUser;
    @FXML
    private TableColumn<Reservation, String> colEvent;
    @FXML
    private TableColumn<Reservation, String> colStatus;
    @FXML
    private TableColumn<Reservation, String> colReservedAt;
    @FXML
    private TableColumn<Reservation, String> colActions;
    @FXML
    private TextField tfSearch;
    @FXML
    private ComboBox<String> cbSort;
    @FXML
    private Label lbPageInfo;

    private final ReservationService reservationService = new ReservationService();
    private int currentPage = 1;
    private final int pageSize = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!isAdmin()) {
            showAlert(Alert.AlertType.ERROR, "Security", "Only admins can moderate reservations.");
            return;
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUser.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUsername()));
        colEvent.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getEventTitle()));
        colStatus.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getStatusLabel()));
        colReservedAt.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue().getReservedAt())));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnApprove = new Button("Approve");
            private final Button btnReject = new Button("Reject");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(8, btnApprove, btnReject, btnDelete);

            {
                btnApprove.getStyleClass().add("edit-button");
                btnReject.getStyleClass().add("secondary-button");
                btnDelete.getStyleClass().add("delete-button");

                btnApprove.setOnAction(event -> moderate(getTableView().getItems().get(getIndex()), true));
                btnReject.setOnAction(event -> moderate(getTableView().getItems().get(getIndex()), false));
                btnDelete.setOnAction(event -> delete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                Reservation row = getTableView().getItems().get(getIndex());
                boolean isPending = row.getStatus() == ReservationStatus.PENDING;

                btnApprove.setVisible(isPending);
                btnApprove.setManaged(isPending);
                btnReject.setVisible(isPending);
                btnReject.setManaged(isPending);

                btnDelete.setVisible(!isPending);
                btnDelete.setManaged(!isPending);

                setGraphic(box);
            }
        });

        cbSort.setItems(FXCollections.observableArrayList("Newest", "Oldest", "User", "Event", "Status"));
        cbSort.getSelectionModel().selectFirst();

        tfSearch.textProperty().addListener((obs, oldValue, newValue) -> {
            currentPage = 1;
            loadRows();
        });

        cbSort.valueProperty().addListener((obs, oldValue, newValue) -> loadRows());

        loadRows();
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadRows();
        }
    }

    @FXML
    private void handleNextPage() {
        int total = reservationService.countBackOfficeReservations(tfSearch.getText());
        int maxPage = (int) Math.ceil((double) total / pageSize);
        if (currentPage < maxPage) {
            currentPage++;
            loadRows();
        }
    }

    private void loadRows() {
        if (!isAdmin()) {
            return;
        }

        String sortBy;
        boolean asc;
        String selected = cbSort.getValue();
        if ("Oldest".equals(selected)) {
            sortBy = "id";
            asc = true;
        } else if ("User".equals(selected)) {
            sortBy = "username";
            asc = true;
        } else if ("Event".equals(selected)) {
            sortBy = "eventTitle";
            asc = true;
        } else if ("Status".equals(selected)) {
            sortBy = "status";
            asc = true;
        } else {
            sortBy = "id";
            asc = false;
        }

        List<Reservation> rows = reservationService.findBackOfficeReservations(tfSearch.getText(), sortBy, asc, currentPage, pageSize);
        reservationTable.setItems(FXCollections.observableArrayList(rows));

        int total = reservationService.countBackOfficeReservations(tfSearch.getText());
        int maxPage = Math.max(1, (int) Math.ceil((double) total / pageSize));
        lbPageInfo.setText("Page " + currentPage + " / " + maxPage + " (" + total + " rows)");
    }

    private void moderate(Reservation reservation, boolean approve) {
        User actor = SessionManager.getCurrentUser();
        try {
            if (approve) {
                reservationService.approve(reservation.getId(), actor);
            } else {
                reservationService.reject(reservation.getId(), actor);
            }
            loadRows();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Moderation", ex.getMessage());
        }
    }

    private void delete(Reservation reservation) {
        User actor = SessionManager.getCurrentUser();
        try {
            reservationService.delete(reservation.getId(), actor);
            loadRows();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Delete", ex.getMessage());
        }
    }

    private boolean isAdmin() {
        User user = SessionManager.getCurrentUser();
        return user != null && user.isAdmin();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
