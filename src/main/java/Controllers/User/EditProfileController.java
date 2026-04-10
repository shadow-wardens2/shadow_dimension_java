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
    private TextField tfCountry;

    @FXML
    private TextField tfCity;

    @FXML
    private TextArea taBio;

    private final ServiceUser serviceUser = new ServiceUser();

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
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Profil mis a jour.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        }
    }

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

    @FXML
    private void handleOpenMarketplace() {
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

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
