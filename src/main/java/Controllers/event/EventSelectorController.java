package Controllers.event;

// Host interface used to swap center content pages in dashboard.
import Controllers.Marketplace.PageHost;
// JavaFX action event type for button handlers.
import javafx.event.ActionEvent;
// Annotation that links methods/fields to FXML.
import javafx.fxml.FXML;

// Simple selector controller used to route to Event or Category management pages.
public class EventSelectorController {

    // Navigation context injected by HomePageController.
    private PageHost dashboardContext;

    // Stores host context for later navigation calls.
    public void setDashboardContext(PageHost dashboardContext) {
        // Assigns injected page host.
        this.dashboardContext = dashboardContext;
    }

    // Handles click on "Manage Events" button.
    @FXML
    void handleEvents(ActionEvent event) {
        // Loads event list page into main content area.
        dashboardContext.loadPage("/event/EventView.fxml");
    }

    // Handles click on "Manage Categories" button.
    @FXML
    void handleEventCategories(ActionEvent event) {
        // Loads category list page into main content area.
        dashboardContext.loadPage("/event/CategoryView.fxml");
    }
}
