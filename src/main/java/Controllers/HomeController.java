package Controllers;

import Utils.SessionManager;
import Utils.AvatarUtil;
import Entities.User.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import java.io.IOException;

public class HomeController {

    @FXML private javafx.scene.layout.AnchorPane rootNode;
    @FXML private Button btnDashboard;
    @FXML private Button btnAuth;
    @FXML private Button btnLogout;
    @FXML private ImageView imgUserAvatar;
    @FXML private Label lbUserAvatar;

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
            if (imgUserAvatar != null && lbUserAvatar != null) {
                AvatarUtil.applyDiceBearAvatar(imgUserAvatar, lbUserAvatar, user, 42);
                imgUserAvatar.setVisible(true);
                imgUserAvatar.setManaged(true);
                lbUserAvatar.setVisible(true);
                lbUserAvatar.setManaged(true);
            }
            if (btnLogout != null) {
                btnLogout.setVisible(true);
                btnLogout.setManaged(true);
            }
        } else if (lbUserAvatar != null && imgUserAvatar != null) {
            lbUserAvatar.setVisible(false);
            lbUserAvatar.setManaged(false);
            imgUserAvatar.setVisible(false);
            imgUserAvatar.setManaged(false);
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

    @FXML
    void handleLogout() {
        SessionManager.clear();
        loadPage("/HomeFront.fxml");
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
