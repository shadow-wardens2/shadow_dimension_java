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
    public void initialize() {
        // Clear login validation messages as the user types.
        tfLoginIdentity.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblLoginIdentityError, ""));
        pfLoginPassword.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblLoginPasswordError, ""));
        tfLoginPasswordVisible.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblLoginPasswordError, ""));

        // Clear signup validation messages as the user corrects each field.
        tfSignupEmail.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblSignupEmailError, ""));
        tfSignupUsername.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblSignupUsernameError, ""));
        pfSignupPassword.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblSignupPasswordError, ""));
        tfSignupPasswordVisible.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblSignupPasswordError, ""));
        pfSignupConfirmPassword.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblSignupConfirmPasswordError, ""));
    }

    @FXML
    private void handleLogin() {
        try {
            // Login validation is shown inline, not via popup.
            clearLoginInlineErrors();

            String identity = tfLoginIdentity.getText() == null ? "" : tfLoginIdentity.getText().trim();
            String password = getLoginPassword();
            boolean hasInputError = false;

            if (identity.isBlank()) {
                setInlineError(lblLoginIdentityError, "Email/Username obligatoire");
                hasInputError = true;
            }

            if (password == null || password.isBlank()) {
                setInlineError(lblLoginPasswordError, "Mot de passe obligatoire");
                hasInputError = true;
            }

            if (hasInputError) {
                return;
            }

            // Delegate credential verification to service layer after local checks pass.
            User user = serviceUser.login(identity, password);
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
            // Signup validation is split in 2 layers: local input checks first, business checks second.
            clearSignupInlineErrors();

            String email = tfSignupEmail.getText() == null ? "" : tfSignupEmail.getText().trim();
            String username = tfSignupUsername.getText() == null ? "" : tfSignupUsername.getText().trim();
            String password = getSignupPassword();
            String confirmPassword = pfSignupConfirmPassword.getText();

            // Cross-field validation: confirm password must match password.
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

        // Returns all unmet password rules in one message for better user guidance.
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

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private void clearSignupInlineErrors() {
        setInlineError(lblSignupEmailError, "");
        setInlineError(lblSignupUsernameError, "");
        setInlineError(lblSignupPasswordError, "");
        setInlineError(lblSignupConfirmPasswordError, "");
    }

    private void clearLoginInlineErrors() {
        setInlineError(lblLoginIdentityError, "");
        setInlineError(lblLoginPasswordError, "");
    }

    private void setInlineError(Label label, String message) {
        if (label == null) {
            return;
        }
        // Show/hide error labels dynamically to keep layout compact when there is no error.
        boolean show = message != null && !message.isBlank();
        label.setText(show ? message : "");
        label.setVisible(show);
        label.setManaged(show);
    }

    private void startVerificationDialog(String email) {
        while (true) {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Verification Email");
            dialog.setHeaderText("Entrez le code recu sur: " + email);

            TextField codeInput = new TextField();
            codeInput.setPromptText("6-digit code");
            dialog.getDialogPane().setContent(codeInput);

            ButtonType verifyType = new ButtonType("Verify");
            ButtonType resendType = new ButtonType("Resend Code");
            dialog.getDialogPane().getButtonTypes().setAll(verifyType, resendType, ButtonType.CANCEL);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() == ButtonType.CANCEL) {
                return;
            }

            try {
                if (result.get() == resendType) {
                    serviceUser.resendVerificationCode(email);
                    showAlert(Alert.AlertType.INFORMATION, "Verification", "Un nouveau code a ete envoye.");
                    continue;
                }

                String code = codeInput.getText();
                if (code == null || code.isBlank()) {
                    showAlert(Alert.AlertType.WARNING, "Verification", "Veuillez entrer le code de verification.");
                    continue;
                }

                boolean verified = serviceUser.verifyEmailCode(email, code);
                if (!verified) {
                    showAlert(Alert.AlertType.WARNING, "Verification", "Code invalide ou expire.");
                    continue;
                }

                showAlert(Alert.AlertType.INFORMATION, "Verification", "Email verifie. Vous pouvez maintenant vous connecter.");
                tfLoginIdentity.setText(email);
                showLogin();
                return;
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
                return;
            } catch (IllegalArgumentException e) {
                showAlert(Alert.AlertType.WARNING, "Attention", e.getMessage());
                return;
            } catch (RuntimeException e) {
                showAlert(Alert.AlertType.ERROR, "Email", e.getMessage());
                return;
            }
        }
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
