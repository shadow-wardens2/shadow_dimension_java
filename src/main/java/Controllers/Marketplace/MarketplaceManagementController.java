package Controllers.Marketplace;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MarketplaceManagementController implements PageHost {

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        // Load the Home page upon initialization
        loadPage("/HomeContent.fxml");
    }

    @FXML
    void openHome(ActionEvent event) {
        loadPage("/HomeContent.fxml");
    }

    @FXML
    void openMarketplaceSelector(ActionEvent event) {
        loadPage("/Marketplace/MarketplaceSelector.fxml");
    }

    // Helper method to swap out the center FXML content dynamically
    public void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // If the loaded page is the MarketplaceSelector, we want to inject this parent
            // controller
            // so from inside the selector they can swap the entire dashboard to
            // Prod/Cat/Type
            Object controller = loader.getController();
            if (controller instanceof MarketplaceSelectorController) {
                ((MarketplaceSelectorController) controller).setDashboardContext(this);
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
