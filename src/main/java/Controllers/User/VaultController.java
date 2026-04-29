package Controllers.User;

import Controllers.Marketplace.Back.PageHost;
import Entities.User.User;
import Services.User.ServiceUser;
import Utils.SessionManager;
import Utils.FaceCaptureUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;

public class VaultController {

    // Vault profile display controls.

    @FXML
    private Label lbVaultTitle;

    @FXML
    private Label lbAuthState;

    @FXML
    private Label lbUsername;

    @FXML
    private Label lbEmail;

    @FXML
    private Label lbFullName;

    @FXML
    private Label lbPhone;

    @FXML
    private Label lbCountry;

    @FXML
    private Label lbCity;

    @FXML
    private Label lbBio;

    @FXML
    private Button btnEditProfile;

    @FXML
    private Label lbFaceIdStatus;

    @FXML
    private Label lbFaceIdHint;

    @FXML
    private Button btnFaceIdAction;

    @FXML
    private Button btnDisableFaceId;

    @FXML
    private javafx.scene.control.ScrollPane rootNode;

    private final ServiceUser serviceUser = new ServiceUser();

    // Dashboard page host to navigate to edit profile.
    private PageHost dashboardContext;

    // Renders guest or authenticated vault snapshot from session user.
    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();

        if (user == null) {
            lbVaultTitle.setText("Guest Vault");
            lbAuthState.setText("Connect Soul to unlock your personal vault.");
            lbUsername.setText("-");
            lbEmail.setText("-");
            lbFullName.setText("-");
            lbPhone.setText("-");
            lbCountry.setText("-");
            lbCity.setText("-");
            lbBio.setText("-");
            btnEditProfile.setDisable(true);
            applyGuestFaceIdState();
            return;
        }

        String username = safe(user.getUsername(), "Shadow Dweller");
        lbVaultTitle.setText(username + " Vault");
        lbAuthState.setText("Soul synchronized.");
        lbUsername.setText(username);
        lbEmail.setText(safe(user.getEmail(), "-"));
        lbFullName.setText(safe(user.getFullName(), "-"));
        lbPhone.setText(safe(user.getPhone(), "-"));
        lbCountry.setText(safe(user.getCountry(), "-"));
        lbCity.setText(safe(user.getCity(), "-"));
        lbBio.setText(safe(user.getBio(), "-"));
        btnEditProfile.setDisable(false);
        refreshFaceIdState(user);
    }

    // Injected by home shell to enable in-page navigation.
    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    // Opens profile editor only when user is logged in.
    @FXML
    private void handleEditProfile() {
        if (dashboardContext != null && SessionManager.isLoggedIn()) {
            dashboardContext.loadPage("/User/EditProfileContent.fxml");
            return;
        }

        if (SessionManager.isLoggedIn()) {
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
    }

    @FXML
    private void handleFaceIdAction() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Face ID", "Connectez-vous avant d'activer Face ID.");
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

    // Returns fallback text when profile fields are empty.
    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private void refreshFaceIdState(User user) {
        if (user == null) {
            applyGuestFaceIdState();
            return;
        }

        try {
            boolean enabled = serviceUser.isFaceIdEnabled(user.getId());
            lbFaceIdStatus.setText(enabled ? "Face ID enabled" : "Face ID disabled");
            lbFaceIdHint.setText(enabled
                    ? "Your current face signature is stored locally for quick vault login."
                    : "Enable Face ID to capture your face now and use it later on the login screen.");
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
        lbFaceIdHint.setText("Connect Soul first, then enable Face ID from your personal vault.");
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
