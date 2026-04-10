package Controllers.User;

import Entities.User.User;
import Services.User.ServiceUser;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class ConnectSoulController {

    @FXML
    private TextField tfLoginIdentity;

    @FXML
    private PasswordField pfLoginPassword;

    @FXML
    private TextField tfSignupEmail;

    @FXML
    private TextField tfSignupUsername;

    @FXML
    private PasswordField pfSignupPassword;

    @FXML
    private VBox loginBox;

    @FXML
    private VBox signupBox;

    private final ServiceUser serviceUser = new ServiceUser();

    @FXML
    private void handleLogin() {
        try {
            User user = serviceUser.login(tfLoginIdentity.getText(), pfLoginPassword.getText());
            if (user == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Identifiants invalides.");
                return;
            }

            SessionManager.setCurrentUser(user);
            openHomePage();
        } catch (SQLException | IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Attention", e.getMessage());
        }
    }

    @FXML
    private void handleSignup() {
        try {
            User user = serviceUser.signup(tfSignupEmail.getText(), tfSignupUsername.getText(), pfSignupPassword.getText());
            SessionManager.setCurrentUser(user);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Compte cree avec succes.");
            openHomePage();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        } catch (IllegalArgumentException | IOException e) {
            showAlert(Alert.AlertType.WARNING, "Attention", e.getMessage());
        }
    }

    @FXML
    private void showSignup() {
        loginBox.setVisible(false);
        loginBox.setManaged(false);
        signupBox.setVisible(true);
        signupBox.setManaged(true);
    }

    @FXML
    private void showLogin() {
        signupBox.setVisible(false);
        signupBox.setManaged(false);
        loginBox.setVisible(true);
        loginBox.setManaged(true);
    }

    private void openHomePage() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/HomePage.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) tfLoginIdentity.getScene().getWindow();
        stage.setTitle("Home");
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
