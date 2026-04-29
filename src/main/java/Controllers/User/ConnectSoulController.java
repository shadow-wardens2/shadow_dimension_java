package Controllers.User;

import Entities.User.User;
import Services.User.ServiceUser;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

    @FXML
    private Label lblLoginIdentityError;
    @FXML
    private Label lblLoginPasswordError;
    @FXML
    private Label lblSignupEmailError;
    @FXML
    private Label lblSignupUsernameError;
    @FXML
    private Label lblSignupPasswordError;
    @FXML
    private Label lblSignupConfirmPasswordError;

    private final ServiceUser serviceUser = new ServiceUser();
    private boolean loginPasswordVisible;
    private boolean signupPasswordVisible;

    public void initialize() {
        // Clear login validation messages as the user types.
        tfLoginIdentity.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblLoginIdentityError, ""));
        pfLoginPassword.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblLoginPasswordError, ""));
        tfLoginPasswordVisible.textProperty()
                .addListener((obs, oldVal, newVal) -> setInlineError(lblLoginPasswordError, ""));

        // Clear signup validation messages as the user corrects each field.
        tfSignupEmail.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblSignupEmailError, ""));
        tfSignupUsername.textProperty()
                .addListener((obs, oldVal, newVal) -> setInlineError(lblSignupUsernameError, ""));
        pfSignupPassword.textProperty()
                .addListener((obs, oldVal, newVal) -> setInlineError(lblSignupPasswordError, ""));
        tfSignupPasswordVisible.textProperty()
                .addListener((obs, oldVal, newVal) -> setInlineError(lblSignupPasswordError, ""));
        pfSignupConfirmPassword.textProperty()
                .addListener((obs, oldVal, newVal) -> setInlineError(lblSignupConfirmPasswordError, ""));
    }

    @FXML
    private void handleLogin() {
        try {
            String identity = tfLoginIdentity.getText();
            String password = getLoginPassword();

            boolean hasError = false;
            if (identity == null || identity.isBlank()) {
                setInlineError(lblLoginIdentityError, "L'email ou le username est obligatoire.");
                hasError = true;
            }
            if (password == null || password.isBlank()) {
                setInlineError(lblLoginPasswordError, "Le mot de passe est obligatoire.");
                hasError = true;
            }
            if (hasError)
                return;

            User user = serviceUser.login(identity, password);
            if (user == null) {
                setInlineError(lblLoginPasswordError, "Identifiants invalides.");
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
            String email = tfSignupEmail.getText();
            String username = tfSignupUsername.getText();
            String password = getSignupPassword();
            String confirmPassword = pfSignupConfirmPassword.getText();

            boolean hasError = false;
            if (email == null || email.isBlank()) {
                setInlineError(lblSignupEmailError, "L'email est obligatoire.");
                hasError = true;
            }
            if (username == null || username.isBlank()) {
                setInlineError(lblSignupUsernameError, "Le username est obligatoire.");
                hasError = true;
            }
            if (password == null || password.isBlank()) {
                setInlineError(lblSignupPasswordError, "Le mot de passe est obligatoire.");
                hasError = true;
            } else {
                String policyError = passwordPolicyMessage(password);
                if (!policyError.isEmpty()) {
                    setInlineError(lblSignupPasswordError, policyError);
                    hasError = true;
                }
            }
            if (confirmPassword == null || confirmPassword.isBlank()) {
                setInlineError(lblSignupConfirmPasswordError, "La confirmation est obligatoire.");
                hasError = true;
            } else if (!password.equals(confirmPassword)) {
                setInlineError(lblSignupConfirmPasswordError, "Les mots de passe ne correspondent pas.");
                hasError = true;
            }

            if (hasError)
                return;

            User user = serviceUser.signup(email, username, password);
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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/HomeFront.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) tfLoginIdentity.getScene().getWindow();
        stage.setTitle("Shadow Dimensions");
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

    private void setInlineError(Label label, String msg) {
        if (label == null)
            return;
        label.setText(msg);
        boolean hasError = msg != null && !msg.isEmpty();
        label.setVisible(hasError);
        label.setManaged(hasError);
    }
}
