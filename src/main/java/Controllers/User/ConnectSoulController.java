package Controllers.User;

import Entities.User.User;
import Services.User.ServiceUser;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
    private TextField tfLoginPasswordVisible;

    @FXML
    private Button btnToggleLoginPassword;

    @FXML
    private TextField tfSignupEmail;

    @FXML
    private TextField tfSignupUsername;

    @FXML
    private PasswordField pfSignupPassword;

    @FXML
    private PasswordField pfSignupConfirmPassword;

    @FXML
    private TextField tfSignupPasswordVisible;

    @FXML
    private Button btnToggleSignupPassword;

    @FXML
    private VBox loginBox;

    @FXML
    private VBox signupBox;

    private final ServiceUser serviceUser = new ServiceUser();
    private boolean loginPasswordVisible;
    private boolean signupPasswordVisible;

    @FXML
    private void handleLogin() {
        try {
            User user = serviceUser.login(tfLoginIdentity.getText(), getLoginPassword());
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
            String password = getSignupPassword();
            String confirmPassword = pfSignupConfirmPassword.getText();

            if (!password.equals(confirmPassword)) {
                showAlert(Alert.AlertType.WARNING, "Attention", "Le mot de passe et sa confirmation ne correspondent pas.");
                return;
            }

            String policyError = passwordPolicyMessage(password);
            if (!policyError.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Mot de passe invalide", policyError);
                return;
            }

            User user = serviceUser.signup(tfSignupEmail.getText(), tfSignupUsername.getText(), password);
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

    @FXML
    private void toggleLoginPasswordVisibility() {
        loginPasswordVisible = !loginPasswordVisible;
        if (loginPasswordVisible) {
            tfLoginPasswordVisible.setText(pfLoginPassword.getText());
        } else {
            pfLoginPassword.setText(tfLoginPasswordVisible.getText());
        }
        applyLoginPasswordVisibility();
    }

    @FXML
    private void toggleSignupPasswordVisibility() {
        signupPasswordVisible = !signupPasswordVisible;
        if (signupPasswordVisible) {
            tfSignupPasswordVisible.setText(pfSignupPassword.getText());
        } else {
            pfSignupPassword.setText(tfSignupPasswordVisible.getText());
        }
        applySignupPasswordVisibility();
    }

    private void applyLoginPasswordVisibility() {
        pfLoginPassword.setVisible(!loginPasswordVisible);
        pfLoginPassword.setManaged(!loginPasswordVisible);
        tfLoginPasswordVisible.setVisible(loginPasswordVisible);
        tfLoginPasswordVisible.setManaged(loginPasswordVisible);
        btnToggleLoginPassword.setText(loginPasswordVisible ? "🙈" : "👁");
    }

    private void applySignupPasswordVisibility() {
        pfSignupPassword.setVisible(!signupPasswordVisible);
        pfSignupPassword.setManaged(!signupPasswordVisible);
        tfSignupPasswordVisible.setVisible(signupPasswordVisible);
        tfSignupPasswordVisible.setManaged(signupPasswordVisible);
        btnToggleSignupPassword.setText(signupPasswordVisible ? "🙈" : "👁");
    }

    private String getLoginPassword() {
        return loginPasswordVisible ? tfLoginPasswordVisible.getText() : pfLoginPassword.getText();
    }

    private String getSignupPassword() {
        return signupPasswordVisible ? tfSignupPasswordVisible.getText() : pfSignupPassword.getText();
    }

    private String passwordPolicyMessage(String password) {
        if (password == null || password.isBlank()) {
            return "Le mot de passe est obligatoire.";
        }

        StringBuilder sb = new StringBuilder();
        if (password.length() < 8) {
            sb.append("- 8 caracteres minimum\n");
        }
        if (!password.matches(".*[A-Z].*")) {
            sb.append("- Au moins une lettre majuscule\n");
        }
        if (!password.matches(".*[a-z].*")) {
            sb.append("- Au moins une lettre minuscule\n");
        }
        if (!password.matches(".*\\d.*")) {
            sb.append("- Au moins un chiffre\n");
        }
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            sb.append("- Au moins un caractere special\n");
        }

        if (sb.length() == 0) {
            return "";
        }

        return "Le mot de passe doit respecter:\n" + sb;
    }

    @FXML
    private void handleBackHome() {
        try {
            openHomePage();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
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
