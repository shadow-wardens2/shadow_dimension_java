package Controllers.User;

import Entities.User.User;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class FrontVaultController {

    @FXML
    private AnchorPane rootNode;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnAuth;

    @FXML
    private Label lbVaultTitle;

    @FXML
    private Label lbAuthState;

    @FXML
    private Label lbUsername;

    @FXML
    private Label lbSummaryUsername;

    @FXML
    private Label lbEmail;

    @FXML
    private Label lbFullName;

    @FXML
    private Label lbPhone;

    @FXML
    private Label lbCountry;

    @FXML
    private Label lbSummaryCountry;

    @FXML
    private Label lbCity;

    @FXML
    private Label lbBio;

    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn()) {
            if (btnAuth != null) {
                btnAuth.setText("Connect Soul");
            }
            lbVaultTitle.setText("Guest Vault");
            lbAuthState.setText("Enter the void freely. Connect your soul whenever you want to bind your identity.");
            lbUsername.setText("Shadow Dweller");
            lbSummaryUsername.setText("Shadow Dweller");
            lbEmail.setText("-");
            lbFullName.setText("-");
            lbPhone.setText("-");
            lbCountry.setText("-");
            lbSummaryCountry.setText("-");
            lbCity.setText("-");
            lbBio.setText("No soul record detected.");
            return;
        }

        User user = SessionManager.getCurrentUser();
        if (user != null && user.isAdmin()) {
            btnDashboard.setVisible(true);
            btnDashboard.setManaged(true);
        }

        if (btnAuth != null) {
            btnAuth.setText("Edit Profile");
        }

        String username = safe(user.getUsername(), "Shadow Dweller");
        lbVaultTitle.setText(username + " Vault");
        lbAuthState.setText("Soul synchronized. Your identity and relic trail are bound to the void.");
        lbUsername.setText(username);
        lbSummaryUsername.setText(username);
        lbEmail.setText(safe(user.getEmail(), "-"));
        lbFullName.setText(safe(user.getFullName(), "-"));
        lbPhone.setText(safe(user.getPhone(), "-"));
        lbCountry.setText(safe(user.getCountry(), "-"));
        lbSummaryCountry.setText(safe(user.getCountry(), "-"));
        lbCity.setText(safe(user.getCity(), "-"));
        lbBio.setText(safe(user.getBio(), "No bio engraved yet."));
    }

    @FXML
    private void navigateToHome() {
        loadPage("/HomeFront.fxml");
    }

    @FXML
    private void navigateToMarketplace() {
        loadPage("/Marketplace/Front/MarketplaceFront.fxml");
    }

    @FXML
    private void navigateToManagement() {
        loadPage("/Tutorials/TutorialsSelector.fxml");
    }

    @FXML
    private void handleOpenDashboard() {
        loadPage("/HomePage.fxml");
    }

    @FXML
    private void handleAuthAction() {
        if (SessionManager.isLoggedIn()) {
            openEditProfile();
        } else {
            loadPage("/User/ConnectSoul.fxml");
        }
    }

    @FXML
    private void handleEditProfile() {
        if (SessionManager.isLoggedIn()) {
            openEditProfile();
        } else {
            loadPage("/User/ConnectSoul.fxml");
        }
    }

    private void openEditProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/EditProfile.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) rootNode.getScene().getWindow();
            stage.setTitle("Edit Profile");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
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

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
