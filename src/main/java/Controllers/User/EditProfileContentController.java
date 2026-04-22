package Controllers.User;

import Controllers.Marketplace.PageHost;
import Entities.User.User;
import Services.User.ServiceUser;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class EditProfileContentController {

    // Profile form controls.

    @FXML
    private TextField tfEmail;

    @FXML
    private TextField tfUsername;

    @FXML
    private TextField tfFullName;

    @FXML
    private TextField tfPhone;

    @FXML
    private Label lblPhoneError;

    @FXML
    private TextField tfCountry;

    @FXML
    private TextField tfCity;

    @FXML
    private TextArea taBio;

    // Dependencies and parent host for in-dashboard navigation.
    private final ServiceUser serviceUser = new ServiceUser();
    private PageHost dashboardContext;

    // Loads current user values into form fields.
    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }

        tfEmail.setText(valueOrEmpty(user.getEmail()));
        tfUsername.setText(valueOrEmpty(user.getUsername()));
        tfFullName.setText(valueOrEmpty(user.getFullName()));
        tfPhone.setText(valueOrEmpty(user.getPhone()));
        tfCountry.setText(valueOrEmpty(user.getCountry()));
        tfCity.setText(valueOrEmpty(user.getCity()));
        taBio.setText(valueOrEmpty(user.getBio()));

        tfPhone.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblPhoneError, ""));
    }

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    // Saves profile while preserving existing values for blank inputs.
    @FXML
    private void handleSaveProfile() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun utilisateur connecte.");
            return;
        }

        try {
            String phoneInput = tfPhone.getText();
            if (phoneInput != null && !phoneInput.isBlank() && !isValidTunisiaPhone(phoneInput.trim())) {
                setInlineError(lblPhoneError, "Format: +216 suivi de 8 chiffres");
                return;
            }

            user.setEmail(keepExistingIfBlank(user.getEmail(), tfEmail.getText()));
            user.setUsername(keepExistingIfBlank(user.getUsername(), tfUsername.getText()));
            user.setFullName(keepExistingIfBlank(user.getFullName(), tfFullName.getText()));
            user.setPhone(keepExistingIfBlank(user.getPhone(), tfPhone.getText()));
            user.setCountry(keepExistingIfBlank(user.getCountry(), tfCountry.getText()));
            user.setCity(keepExistingIfBlank(user.getCity(), tfCity.getText()));
            user.setBio(keepExistingIfBlank(user.getBio(), taBio.getText()));

            serviceUser.updateProfile(user);
            SessionManager.setCurrentUser(user);

            if (dashboardContext != null) {
                dashboardContext.loadPage("/User/VaultContent.fxml");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Profil mis a jour.");
            }
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() == null ? "Erreur de validation." : e.getMessage();
            if (msg.toLowerCase().contains("telephone")) {
                setInlineError(lblPhoneError, "Format: +216 suivi de 8 chiffres");
            } else {
                showAlert(Alert.AlertType.WARNING, "Attention", msg);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        }
    }

    // Returns to vault page inside the dashboard.
    @FXML
    private void handleBackToVault() {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/User/VaultContent.fxml");
        }
    }

    // Generic non-blocking alert helper for this screen.
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }

    // Converts null DB values into empty strings for text fields.
    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    // Keeps existing profile data when the new input is blank.
    private String keepExistingIfBlank(String existingValue, String inputValue) {
        if (inputValue == null || inputValue.isBlank()) {
            return existingValue;
        }
        return inputValue.trim();
    }

    private boolean isValidTunisiaPhone(String phone) {
        return phone.matches("^\\+216\\d{8}$");
    }

    private void setInlineError(Label label, String message) {
        if (label == null) {
            return;
        }
        boolean show = message != null && !message.isBlank();
        label.setText(show ? message : "");
        label.setVisible(show);
        label.setManaged(show);
    }
}
