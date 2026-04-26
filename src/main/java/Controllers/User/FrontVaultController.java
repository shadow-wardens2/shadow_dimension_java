package Controllers.User;

import Entities.User.User;
import Services.User.ServiceUser;
import Utils.FaceCaptureUtil;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;

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
    private Label lbFaceIdStatus;

    @FXML
    private Label lbFaceIdHint;

    @FXML
    private Button btnFaceIdAction;

    @FXML
    private Button btnDisableFaceId;

    private final ServiceUser serviceUser = new ServiceUser();

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
            applyGuestFaceIdState();
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
        refreshFaceIdState(user);
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

    @FXML
    private void handleFaceIdAction() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            loadPage("/User/ConnectSoul.fxml");
            return;
        }

        try {
            Stage owner = (Stage) rootNode.getScene().getWindow();
            BufferedImage capture = FaceCaptureUtil.captureFace(
                    owner,
                    "Enable Face ID",
                    "Capture a clear frontal face image to bind Face ID to your vault."
            );
            if (capture == null) {
                return;
            }

            String signature = serviceUser.buildFaceSignature(capture);
            serviceUser.enrollFaceId(user.getId(), signature);
            refreshFaceIdState(serviceUser.getById(user.getId()));
            showAlert(Alert.AlertType.INFORMATION, "Face ID", "Face ID activee pour ce compte.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Face ID", e.getMessage());
        }
    }

    @FXML
    private void handleDisableFaceId() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }

        try {
            serviceUser.disableFaceId(user.getId());
            refreshFaceIdState(serviceUser.getById(user.getId()));
            showAlert(Alert.AlertType.INFORMATION, "Face ID", "Face ID desactivee.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
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

    private void refreshFaceIdState(User user) {
        try {
            boolean enabled = serviceUser.isFaceIdEnabled(user.getId());
            lbFaceIdStatus.setText(enabled ? "Face ID enabled" : "Face ID disabled");
            lbFaceIdHint.setText(enabled
                    ? "Your face signature is ready for quick access from the login screen."
                    : "Enable Face ID to capture your face from this vault and unlock biometric login.");
            btnFaceIdAction.setText(enabled ? "Re-enroll Face ID" : "Enable Face ID");
            btnFaceIdAction.setDisable(false);
            btnDisableFaceId.setDisable(!enabled);
        } catch (SQLException e) {
            lbFaceIdStatus.setText("Face ID unavailable");
            lbFaceIdHint.setText("The biometric profile could not be loaded right now.");
            btnFaceIdAction.setDisable(true);
            btnDisableFaceId.setDisable(true);
        }
    }

    private void applyGuestFaceIdState() {
        lbFaceIdStatus.setText("Face ID locked");
        lbFaceIdHint.setText("Connect Soul first, then enable Face ID from your vault.");
        btnFaceIdAction.setText("Enable Face ID");
        btnFaceIdAction.setDisable(true);
        btnDisableFaceId.setDisable(true);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
