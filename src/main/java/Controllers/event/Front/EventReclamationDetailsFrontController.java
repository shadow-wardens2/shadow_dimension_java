package Controllers.event.Front;

import Entities.event.EventReclamation;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.io.IOException;

public class EventReclamationDetailsFrontController {

    @FXML
    private Label lblStatus;
    @FXML
    private Label lblSubject;
    @FXML
    private Label lblDate;
    @FXML
    private Label lblEventTitle;
    @FXML
    private TextArea taMessage;
    @FXML
    private TextArea taAiResponse;
    @FXML
    private TextArea taAdminResponse;

    public void setReclamation(EventReclamation reclamation) {
        if (reclamation == null) return;

        lblStatus.setText(reclamation.getStatusLabel());
        lblSubject.setText(safe(reclamation.getSubject()));
        lblDate.setText(String.valueOf(reclamation.getCreatedAt()));
        lblEventTitle.setText("Event: " + safe(reclamation.getEventTitle()));

        taMessage.setText(safe(reclamation.getMessage()));
        taAiResponse.setText(safeOrPlaceholder(reclamation.getAiResponse(), "No AI analysis available yet."));
        taAdminResponse.setText(safeOrPlaceholder(reclamation.getAdminResponse(), "Pending admin review..."));
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/event/Front/EventReclamationFront.fxml"));
            lblStatus.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation", e.getMessage());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeOrPlaceholder(String value, String placeholder) {
        return (value == null || value.isBlank()) ? placeholder : value;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
