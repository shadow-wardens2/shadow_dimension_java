package Controllers;

import Utils.SessionManager;
import Entities.User.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import java.io.IOException;

public class HomeController {

    @FXML
    private javafx.scene.layout.AnchorPane rootNode;
    @FXML
    private Button btnDashboard;

    @FXML
    public void initialize() {
        if (SessionManager.isLoggedIn()) {
            User user = SessionManager.getCurrentUser();
            if (user.isAdmin()) {
                btnDashboard.setVisible(true);
                btnDashboard.setManaged(true);
            }
        }
    }

    @FXML
    void navigateToMarketplace() {
        if (SessionManager.isLoggedIn()) {
            loadPage("/Marketplace/Front/MarketplaceFront.fxml");
        } else {
            loadPage("/User/ConnectSoul.fxml");
        }
    }

    @FXML
    void navigateToTutorials() {
        if (SessionManager.isLoggedIn()) {
            loadPage("/Tutorials/TutorialsFront.fxml");
        } else {
            loadPage("/User/ConnectSoul.fxml");
        }
    }

    @FXML
    void navigateToManagement() {
        if (SessionManager.isLoggedIn()) {
            loadPage("/Marketplace/Back/MarketplaceManagement.fxml");
        } else {
            loadPage("/User/ConnectSoul.fxml");
        }
    }

    @FXML
    void handleOpenDashboard() {
        loadPage("/HomePage.fxml");
    }

    private void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            rootNode.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEnterNight() {
        navigateToMarketplace();
    }
}
