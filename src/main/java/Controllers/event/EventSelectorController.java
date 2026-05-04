package Controllers.event;

import Controllers.Marketplace.Back.PageHost;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

public class EventSelectorController {

    private PageHost dashboardContext;

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @FXML
    void handleEvents(ActionEvent event) {
        openPage("/event/EventView.fxml");
    }

    @FXML
    void handleEventCategories(ActionEvent event) {
        openPage("/event/CategoryView.fxml");
    }

    @FXML
    void handleReservations(ActionEvent event) {
        openPage("/event/Back/ReservationManagement.fxml");
    }

    @FXML
    void handleReclamations(ActionEvent event) {
        openPage("/event/Back/EventReclamationManagement.fxml");
    }

    @FXML
    void handleReviews(ActionEvent event) {
        openPage("/event/Back/ReviewManagement.fxml");
    }

    private void openPage(String fxmlPath) {
        if (dashboardContext == null) {
            showNavigationError("Dashboard context is not available.");
            return;
        }

        if (getClass().getResource(fxmlPath) == null) {
            showNavigationError("Page resource not found: " + fxmlPath);
            return;
        }

        dashboardContext.loadPage(fxmlPath);
    }

    private void showNavigationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Navigation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
