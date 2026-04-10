package Controllers.Marketplace;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class MarketplaceSelectorController {

    private PageHost dashboardContext;

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @FXML
    void handleCategories(ActionEvent event) {
        dashboardContext.loadPage("/Marketplace/ManagementCategorie.fxml");
    }

    @FXML
    void handleProducts(ActionEvent event) {
        dashboardContext.loadPage("/Marketplace/ManagementProduit.fxml");
    }

    @FXML
    void handleTypes(ActionEvent event) {
        dashboardContext.loadPage("/Marketplace/ManagementType.fxml");
    }
}
