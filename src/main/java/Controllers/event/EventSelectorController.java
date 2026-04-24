package Controllers.event;

// Host interface used to swap center content pages in dashboard.
import Controllers.Marketplace.PageHost;
import Entities.User.User;
import Utils.SessionManager;
// JavaFX action event type for button handlers.
import javafx.event.ActionEvent;
// Annotation that links methods/fields to FXML.
import javafx.fxml.FXML;
import javafx.scene.control.Button;

// Simple selector controller used to route to Event or Category management pages.
public class EventSelectorController {

    // Navigation context injected by HomePageController.
    private PageHost dashboardContext;

    @FXML
    private Button btnEvents;
    @FXML
    private Button btnCategories;
    @FXML
    private Button btnReservations;

    @FXML
    private void initialize() {
        User user = SessionManager.getCurrentUser();
        boolean isAdmin = user != null && user.isAdmin();
        btnEvents.setText(isAdmin ? "Manage Events" : "View Events");
        if (btnCategories != null) {
            btnCategories.setVisible(isAdmin);
            btnCategories.setManaged(isAdmin);
        }
        if (btnReservations != null) {
            btnReservations.setVisible(isAdmin);
            btnReservations.setManaged(isAdmin);
        }
    }

    // Stores host context for later navigation calls.
    public void setDashboardContext(PageHost dashboardContext) {
        // Assigns injected page host.
        this.dashboardContext = dashboardContext;
    }

    // Handles click on "Manage Events" button.
    @FXML
    void handleEvents(ActionEvent event) {
        // Loads event list page into main content area.
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/EventView.fxml");
        }
    }

    // Handles click on "Manage Categories" button.
    @FXML
    void handleEventCategories(ActionEvent event) {
        // Loads category list page into main content area.
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/CategoryView.fxml");
        }
    }

    @FXML
    void handleReservations(ActionEvent event) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/ReservationView.fxml");
        }
    }
}
