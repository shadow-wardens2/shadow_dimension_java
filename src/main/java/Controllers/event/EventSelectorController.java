package Controllers.event;

import Controllers.Marketplace.PageHost;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class EventSelectorController {

    private PageHost dashboardContext;

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @FXML
    void handleEvents(ActionEvent event) {
        dashboardContext.loadPage("/event/EventView.fxml");
    }

    @FXML
    void handleEventCategories(ActionEvent event) {
        dashboardContext.loadPage("/event/CategoryView.fxml");
    }
}
