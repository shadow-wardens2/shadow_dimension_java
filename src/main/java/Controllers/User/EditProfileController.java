package Controllers.User;

import Entities.User.User;
import Services.User.ServiceUser;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class EditProfileController {

    // Header and profile form controls.

    @FXML
    private Label lbWelcome;

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

    // Service dependency for profile persistence.
    private final ServiceUser serviceUser = new ServiceUser();

    // Initializes profile form from current session user.
    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }

        lbWelcome.setText("Connected as: " + user.getUsername());
        tfEmail.setText(user.getEmail());
        tfUsername.setText(user.getUsername());
        tfFullName.setText(emptyIfNull(user.getFullName()));
        tfPhone.setText(emptyIfNull(user.getPhone()));
        tfCountry.setText(emptyIfNull(user.getCountry()));
        tfCity.setText(emptyIfNull(user.getCity()));
        taBio.setText(emptyIfNull(user.getBio()));

        tfPhone.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblPhoneError, ""));
    }

    // Saves edited profile values.
    @FXML
    private void handleSaveProfile() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun utilisateur connecte.");
            return;
        }

        try {
            setInlineError(lblPhoneError, "");

            String phone = tfPhone.getText() == null ? "" : tfPhone.getText().trim();
            if (!phone.isBlank() && !isValidTunisiaPhone(phone)) {
                setInlineError(lblPhoneError, "Format: +216 suivi de 8 chiffres");
                return;
            }

            user.setEmail(tfEmail.getText().trim());
            user.setUsername(tfUsername.getText().trim());
            user.setFullName(tfFullName.getText().trim());
            user.setPhone(phone);
            user.setCountry(tfCountry.getText().trim());
            user.setCity(tfCity.getText().trim());
            user.setBio(taBio.getText().trim());

            serviceUser.updateProfile(user);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Profil mis a jour.");
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

    // Logs user out and returns to Connect Soul screen.
    @FXML
    private void handleLogout() {
        SessionManager.clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/ConnectSoul.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setTitle("Connect Soul");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // Opens marketplace management when user is authenticated.
    @FXML
    private void handleOpenMarketplace() {
        if (!SessionManager.isLoggedIn()) {
            showAlert(Alert.AlertType.ERROR, "Accès Refusé", "Vous devez être connecté pour accéder au Marketplace.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/MarketplaceManagement.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setTitle("Gestion Produits");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // Generic blocking alert helper.
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // Replaces null DB values by empty text for UI binding.
    private String emptyIfNull(String value) {
        return value == null ? "" : value;
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
