package Controllers.User;

import Controllers.Marketplace.PageHost;
import Entities.User.User;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class VaultController {

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

    private PageHost dashboardContext;

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
    }

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @FXML
    private void handleEditProfile() {
        if (dashboardContext != null && SessionManager.isLoggedIn()) {
            dashboardContext.loadPage("/User/EditProfileContent.fxml");
        }
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
