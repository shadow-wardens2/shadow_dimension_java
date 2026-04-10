package Controllers.User;

import Controllers.Marketplace.PageHost;
import Entities.User.User;
import Services.User.ServiceUser;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class EditProfileContentController {

    @FXML
    private TextField tfEmail;

    @FXML
    private TextField tfUsername;

    @FXML
    private TextField tfFullName;

    @FXML
    private TextField tfPhone;

    @FXML
    private TextField tfCountry;

    @FXML
    private TextField tfCity;

    @FXML
    private TextArea taBio;

    private final ServiceUser serviceUser = new ServiceUser();
    private PageHost dashboardContext;

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
    }

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @FXML
    private void handleSaveProfile() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun utilisateur connecte.");
            return;
        }

        try {
            user.setEmail(tfEmail.getText().trim());
            user.setUsername(tfUsername.getText().trim());
            user.setFullName(tfFullName.getText().trim());
            user.setPhone(tfPhone.getText().trim());
            user.setCountry(tfCountry.getText().trim());
            user.setCity(tfCity.getText().trim());
            user.setBio(taBio.getText().trim());

            serviceUser.updateProfile(user);
            SessionManager.setCurrentUser(user);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Profil mis a jour.");

            if (dashboardContext != null) {
                dashboardContext.loadPage("/User/VaultContent.fxml");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        }
    }

    @FXML
    private void handleBackToVault() {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/User/VaultContent.fxml");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
