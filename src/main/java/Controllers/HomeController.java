package Controllers;

import Utils.SessionManager;
import Entities.User.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import java.io.IOException;

public class HomeController {

    @FXML private javafx.scene.layout.AnchorPane rootNode;
    @FXML private Button btnDashboard;
    @FXML private Button btnAuth;

    @FXML
    public void initialize() {
        if (SessionManager.isLoggedIn()) {
            User user = SessionManager.getCurrentUser();
            if (user.isAdmin()) {
                btnDashboard.setVisible(true);
                btnDashboard.setManaged(true);
            }
            if (btnAuth != null) {
                btnAuth.setText("My Soul");
            }
        }
    }

    @FXML
    void navigateToMarketplace() {
        loadPage("/Marketplace/Front/MarketplaceFront.fxml");
    }

    @FXML
    void navigateToVault() {
        loadPage("/User/VaultFront.fxml");
    }

    @FXML
    void navigateToManagement() {
        loadPage("/Tutorials/TutorialsSelector.fxml");
    }

    @FXML
    void navigateToEvents() {
        loadPage("/event/Front/EventFront.fxml");
    }

    @FXML
    void handleOpenDashboard() {
        loadPage("/HomePage.fxml");
    }

    @FXML
    void handleAuthAction() {
        if (SessionManager.isLoggedIn()) {
            navigateToVault();
        } else {
            loadPage("/User/ConnectSoul.fxml");
        }
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
