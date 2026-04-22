package Controllers.User;

import Entities.User.User;
import Services.User.GoogleOAuthService;
import Services.User.ServiceUser;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class ConnectSoulController {

    // Login form controls.

    @FXML
    private TextField tfLoginIdentity;

    @FXML
    private PasswordField pfLoginPassword;

    @FXML
    private TextField tfLoginPasswordVisible;

    @FXML
    private Button btnToggleLoginPassword;

    @FXML
    private Label lblLoginIdentityError;

    @FXML
    private Label lblLoginPasswordError;

    // Signup form controls.
    @FXML
    private TextField tfSignupEmail;

    @FXML
    private TextField tfSignupUsername;

    @FXML
    private PasswordField pfSignupPassword;

    @FXML
    private PasswordField pfSignupConfirmPassword;

    @FXML
    private Label lblSignupEmailError;

    @FXML
    private Label lblSignupUsernameError;

    @FXML
    private Label lblSignupPasswordError;

    @FXML
    private Label lblSignupConfirmPasswordError;

    @FXML
    private TextField tfSignupPasswordVisible;

    @FXML
    private Button btnToggleSignupPassword;

    @FXML
    private VBox loginBox;

    @FXML
    private VBox signupBox;

    // Service layer dependencies and local UI state flags.
    private final ServiceUser serviceUser = new ServiceUser();
    private final GoogleOAuthService googleOAuthService = new GoogleOAuthService();
    private boolean loginPasswordVisible;
    private boolean signupPasswordVisible;

    @FXML
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

    // Handles login with inline input validation then delegates credential check to the service.
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
                setInlineError(lblLoginIdentityError, "Identifiants invalides");
                return;
            }

            SessionManager.setCurrentUser(user);
            openHomePage();
        } catch (SQLException | IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() == null ? "Erreur de validation." : e.getMessage();
            String lower = msg.toLowerCase();
            if (lower.contains("mot de passe") || lower.contains("password") || lower.contains("obligatoire")) {
                setInlineError(lblLoginPasswordError, msg);
            } else {
                setInlineError(lblLoginIdentityError, msg);
            }
        }
    }

    // Handles signup with inline validation, then starts email verification flow.
    @FXML
    private void handleSignup() {
        try {
            // Signup validation is split in 2 layers: local input checks first, business checks second.
            clearSignupInlineErrors();

            String email = tfSignupEmail.getText() == null ? "" : tfSignupEmail.getText().trim();
            String username = tfSignupUsername.getText() == null ? "" : tfSignupUsername.getText().trim();
            String password = getSignupPassword();
            String confirmPassword = pfSignupConfirmPassword.getText() == null ? "" : pfSignupConfirmPassword.getText();
            boolean hasInputError = false;

            if (email.isBlank()) {
                setInlineError(lblSignupEmailError, "Email obligatoire (ex: nom@domaine.com)");
                hasInputError = true;
            }
            if (!email.isBlank() && !isValidEmail(email)) {
                setInlineError(lblSignupEmailError, "Format email invalide (ex: nom@domaine.com)");
                hasInputError = true;
            }

            if (username.isBlank()) {
                setInlineError(lblSignupUsernameError, "Username obligatoire");
                hasInputError = true;
            } else if (username.length() < 3) {
                setInlineError(lblSignupUsernameError, "Username: minimum 3 caracteres");
                hasInputError = true;
            }

            if (password == null || password.isBlank()) {
                setInlineError(lblSignupPasswordError, "Password obligatoire (8+ caracteres, majuscule, minuscule, chiffre, caractere special)");
                hasInputError = true;
            }

            if (confirmPassword.isBlank()) {
                setInlineError(lblSignupConfirmPasswordError, "Confirm Password obligatoire");
                hasInputError = true;
            }

            if (hasInputError) {
                return;
            }

            // Cross-field validation: confirm password must match password.
            if (!password.equals(confirmPassword)) {
                setInlineError(lblSignupConfirmPasswordError, "Doit etre identique au mot de passe");
                return;
            }

            String policyError = passwordPolicyMessage(password);
            if (!policyError.isEmpty()) {
                setInlineError(lblSignupPasswordError, policyError);
                return;
            }

            User user = serviceUser.signup(email, username, password);
            showAlert(Alert.AlertType.INFORMATION, "Verification", "Compte cree. Un code de verification a ete envoye a cet email.");
            startVerificationDialog(user.getEmail());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() == null ? "Erreur de validation." : e.getMessage();
            String lower = msg.toLowerCase();
            if (lower.contains("email")) {
                setInlineError(lblSignupEmailError, msg);
            } else if (lower.contains("username")) {
                setInlineError(lblSignupUsernameError, msg);
            } else if (lower.contains("mot de passe") || lower.contains("password")) {
                setInlineError(lblSignupPasswordError, msg);
            } else {
                showAlert(Alert.AlertType.WARNING, "Attention", msg);
            }
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Email", e.getMessage());
        }
    }

    // Handles Google OAuth login/signup flow.
    @FXML
    private void handleGoogleAuth() {
        try {
            GoogleOAuthService.GoogleProfile profile = googleOAuthService.authenticate();
            User user = serviceUser.loginOrSignupWithGoogle(profile.googleId(), profile.email(), profile.fullName());
            SessionManager.setCurrentUser(user);
            openHomePage();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.WARNING, "Google Auth", e.getMessage());
        }
    }

    // Switches UI to signup panel.
    @FXML
    private void showSignup() {
        loginBox.setVisible(false);
        loginBox.setManaged(false);
        signupBox.setVisible(true);
        signupBox.setManaged(true);
    }

    // Switches UI to login panel.
    @FXML
    private void showLogin() {
        signupBox.setVisible(false);
        signupBox.setManaged(false);
        loginBox.setVisible(true);
        loginBox.setManaged(true);
    }

    // Toggles login password masking.
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

    // Toggles signup password masking.
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

    // Applies visibility state to login password controls and eye icon.
    private void applyLoginPasswordVisibility() {
        pfLoginPassword.setVisible(!loginPasswordVisible);
        pfLoginPassword.setManaged(!loginPasswordVisible);
        tfLoginPasswordVisible.setVisible(loginPasswordVisible);
        tfLoginPasswordVisible.setManaged(loginPasswordVisible);
        btnToggleLoginPassword.setText(loginPasswordVisible ? "🙈" : "👁");
    }

    // Applies visibility state to signup password controls and eye icon.
    private void applySignupPasswordVisibility() {
        pfSignupPassword.setVisible(!signupPasswordVisible);
        pfSignupPassword.setManaged(!signupPasswordVisible);
        tfSignupPasswordVisible.setVisible(signupPasswordVisible);
        tfSignupPasswordVisible.setManaged(signupPasswordVisible);
        btnToggleSignupPassword.setText(signupPasswordVisible ? "🙈" : "👁");
    }

    // Returns the effective login password based on current visibility mode.
    private String getLoginPassword() {
        return loginPasswordVisible ? tfLoginPasswordVisible.getText() : pfLoginPassword.getText();
    }

    // Returns the effective signup password based on current visibility mode.
    private String getSignupPassword() {
        return signupPasswordVisible ? tfSignupPasswordVisible.getText() : pfSignupPassword.getText();
    }

    // Builds a combined password-policy message; empty string means valid password.
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

    // Basic email format check used by signup validation.
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    // Clears all signup inline error labels.
    private void clearSignupInlineErrors() {
        setInlineError(lblSignupEmailError, "");
        setInlineError(lblSignupUsernameError, "");
        setInlineError(lblSignupPasswordError, "");
        setInlineError(lblSignupConfirmPasswordError, "");
    }

    // Clears all login inline error labels.
    private void clearLoginInlineErrors() {
        setInlineError(lblLoginIdentityError, "");
        setInlineError(lblLoginPasswordError, "");
    }

    // Central helper for showing/hiding inline validation labels.
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

    // Email verification dialog loop used after signup (verify/resend/cancel).
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

    // Returns to home shell scene.
    @FXML
    private void handleBackHome() {
        try {
            openHomePage();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // Loads HomePage scene in the current stage.
    private void openHomePage() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/HomePage.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) tfLoginIdentity.getScene().getWindow();
        stage.setTitle("Home");
        stage.setScene(new Scene(root));
        stage.show();
    }

    // Generic popup helper for system/business messages.
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
