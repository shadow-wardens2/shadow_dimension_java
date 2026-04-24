package Controllers.event;

import Controllers.Marketplace.PageHost;
import Entities.event.EventReservation;
import Services.event.EventReservationService;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class ReservationController implements Initializable {

    @FXML
    private VBox reservationsContainer;

    private PageHost dashboardContext;
    private final EventReservationService reservationService = new EventReservationService();

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (SessionManager.getCurrentUser() == null || !SessionManager.getCurrentUser().isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Acces restreint", "Seuls les admins peuvent gerer les reservations.");
            if (dashboardContext != null) {
                dashboardContext.loadPage("/event/EventSelector.fxml");
            }
            return;
        }
        loadReservations();
    }

    @FXML
    private void handleGoBack() {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/EventSelector.fxml");
        }
    }

    private void loadReservations() {
        reservationsContainer.getChildren().clear();
        try {
            List<EventReservation> reservations = reservationService.getAllWithDetails();
            if (reservations.isEmpty()) {
                Label empty = new Label("No reservations found.");
                empty.setStyle("-fx-text-fill: #9ea3b0; -fx-font-size: 14px;");
                reservationsContainer.getChildren().add(empty);
                return;
            }

            for (EventReservation reservation : reservations) {
                reservationsContainer.getChildren().add(createReservationCard(reservation));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private HBox createReservationCard(EventReservation reservation) {
        Label idLabel = new Label("#" + reservation.getId());
        idLabel.setStyle("-fx-text-fill: #d6b2fc; -fx-font-weight: 700;");
        idLabel.setMinWidth(56);
        idLabel.setPrefWidth(56);

        Label eventLabel = new Label(truncate(safe(reservation.getEventTitle()), 40));
        eventLabel.setStyle("-fx-text-fill: #f3eefc; -fx-font-size: 14px; -fx-font-weight: 700;");
        eventLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        eventLabel.setMinWidth(220);
        eventLabel.setPrefWidth(260);

        String userIdentity = safe(reservation.getUsername()) + " | " + safe(reservation.getUserEmail());
        Label userLabel = new Label(truncate(userIdentity, 38));
        userLabel.setStyle("-fx-text-fill: #9ea3b0; -fx-font-size: 12px;");
        userLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        userLabel.setMinWidth(230);
        userLabel.setPrefWidth(260);

        Label dateLabel = new Label("Reserved: " + formatTimestamp(reservation.getReservedAt()));
        dateLabel.setStyle("-fx-text-fill: #9ea3b0;");
        dateLabel.setMinWidth(180);
        dateLabel.setPrefWidth(180);

        Label statusLabel = new Label(safe(reservation.getStatus()));
        statusLabel.setStyle("-fx-text-fill: #c8b3ff; -fx-font-weight: 700;");
        statusLabel.setMinWidth(90);
        statusLabel.setPrefWidth(90);

        Button btnAccept = new Button("Accept");
        btnAccept.getStyleClass().add("edit-button");
        btnAccept.setDisable("ACCEPTED".equalsIgnoreCase(reservation.getStatus()));
        btnAccept.setOnAction(event -> updateReservationStatus(reservation, "ACCEPTED"));

        Button btnRefuse = new Button("Refuse");
        btnRefuse.getStyleClass().add("delete-button");
        btnRefuse.setDisable("REFUSED".equalsIgnoreCase(reservation.getStatus()));
        btnRefuse.setOnAction(event -> updateReservationStatus(reservation, "REFUSED"));

        HBox actions = new HBox(8, btnAccept, btnRefuse);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox row = new HBox(14, idLabel, eventLabel, userLabel, dateLabel, statusLabel, spacer, actions);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("user-row-card");
        return row;
    }

    private void updateReservationStatus(EventReservation reservation, String status) {
        try {
            reservationService.updateStatus(reservation.getId(), status);
            loadReservations();
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Reservation mise a jour avec succes.");
        } catch (SQLException | IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private String safe(String value) {
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

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(timestamp);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
