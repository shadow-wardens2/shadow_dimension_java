package Controllers.User;

import Entities.User.User;
import Services.User.GoogleOAuthService;
import Services.User.ServiceUser;
import Utils.FaceCaptureUtil;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Optional;

public class ConnectSoulController {
    private static final String CAPTCHA_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CAPTCHA_LENGTH = 6;
    private static final double CAPTCHA_WIDTH = 360;
    private static final double CAPTCHA_HEIGHT = 76;

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
    private Canvas signupCaptchaCanvas;

    @FXML
    private TextField tfSignupCaptcha;

    @FXML
    private Label lblSignupCaptchaError;

    @FXML
    private TextField tfResetCode;

    @FXML
    private PasswordField pfResetPassword;

    @FXML
    private TextField tfResetPasswordVisible;

    @FXML
    private PasswordField pfResetConfirmPassword;

    @FXML
    private Button btnToggleResetPassword;

    @FXML
    private VBox loginBox;

    @FXML
    private VBox signupBox;

    @FXML
    private VBox resetBox;

    @FXML
    private Label lblResetEmailHint;

    @FXML
    private Label lblResetCodeError;

    @FXML
    private Label lblResetPasswordError;

    @FXML
    private Label lblResetConfirmPasswordError;

    @FXML
    private Canvas resetCaptchaCanvas;

    @FXML
    private TextField tfResetCaptcha;

    @FXML
    private Label lblResetCaptchaError;

    private final ServiceUser serviceUser = new ServiceUser();
    private final GoogleOAuthService googleOAuthService = new GoogleOAuthService();
    private final SecureRandom secureRandom = new SecureRandom();
    private boolean loginPasswordVisible;
    private boolean signupPasswordVisible;
    private boolean resetPasswordVisible;
    private String pendingResetEmail;
    private String signupCaptchaAnswer;
    private String resetCaptchaAnswer;

    @FXML
    public void initialize() {
        tfLoginIdentity.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblLoginIdentityError, ""));
        pfLoginPassword.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblLoginPasswordError, ""));
        tfLoginPasswordVisible.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblLoginPasswordError, ""));
        tfSignupEmail.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblSignupEmailError, ""));
        tfSignupUsername.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblSignupUsernameError, ""));
        pfSignupPassword.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblSignupPasswordError, ""));
        tfSignupPasswordVisible.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblSignupPasswordError, ""));
        pfSignupConfirmPassword.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblSignupConfirmPasswordError, ""));
        tfSignupCaptcha.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblSignupCaptchaError, ""));

        tfResetCode.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblResetCodeError, ""));
        pfResetPassword.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblResetPasswordError, ""));
        tfResetPasswordVisible.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblResetPasswordError, ""));
        pfResetConfirmPassword.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblResetConfirmPasswordError, ""));
        tfResetCaptcha.textProperty().addListener((obs, oldVal, newVal) -> setInlineError(lblResetCaptchaError, ""));

        applyLoginPasswordVisibility();
        applySignupPasswordVisibility();
        applyResetPasswordVisibility();
        refreshAllCaptchas();
    }

    @FXML
    private void handleLogin() {
        try {
            clearLoginInlineErrors();

            String identity = value(tfLoginIdentity).trim();
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
            String msg = fallbackMessage(e);
            String lower = msg.toLowerCase();
            if (lower.contains("mot de passe") || lower.contains("password") || lower.contains("obligatoire")) {
                setInlineError(lblLoginPasswordError, msg);
            } else {
                setInlineError(lblLoginIdentityError, msg);
            }
        }
    }

    @FXML
    private void handleSignup() {
        try {
            clearSignupInlineErrors();

            String email = value(tfSignupEmail).trim();
            String username = value(tfSignupUsername).trim();
            String password = getSignupPassword();
            String confirmPassword = value(pfSignupConfirmPassword);
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

            if (!password.equals(confirmPassword)) {
                setInlineError(lblSignupConfirmPasswordError, "Doit etre identique au mot de passe");
                return;
            }

            String policyError = serviceUser.getPasswordPolicyMessage(password);
            if (!policyError.isEmpty()) {
                setInlineError(lblSignupPasswordError, policyError);
                return;
            }

            if (!validateCaptcha(signupCaptchaAnswer, tfSignupCaptcha, lblSignupCaptchaError, this::refreshSignupCaptcha)) {
                return;
            }

            User user = serviceUser.signup(email, username, password);
            showAlert(Alert.AlertType.INFORMATION, "Verification", "Compte cree. Un code de verification a ete envoye a cet email.");
            startVerificationDialog(user.getEmail());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        } catch (IllegalArgumentException e) {
            String msg = fallbackMessage(e);
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

    @FXML
    private void handleForgotPassword() {
        try {
            clearLoginInlineErrors();

            String identity = value(tfLoginIdentity).trim();
            if (identity.isBlank()) {
                setInlineError(lblLoginIdentityError, "Entrez votre email ou username pour reinitialiser le mot de passe");
                return;
            }

            serviceUser.sendPasswordResetCode(identity);
            User user = serviceUser.getByIdentity(identity);
            if (user == null) {
                setInlineError(lblLoginIdentityError, "Aucun utilisateur trouve pour cet identifiant");
                return;
            }

            pendingResetEmail = user.getEmail();
            lblResetEmailHint.setText("Enter the code sent to " + user.getEmail() + " and forge a new password that respects the project rules.");
            tfResetCode.clear();
            setResetPasswordValue("");
            pfResetConfirmPassword.clear();
            tfResetCaptcha.clear();
            clearResetInlineErrors();
            showReset();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        } catch (IllegalArgumentException e) {
            setInlineError(lblLoginIdentityError, fallbackMessage(e));
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Email", e.getMessage());
        }
    }

    @FXML
    private void handleResetPassword() {
        try {
            clearResetInlineErrors();

            if (pendingResetEmail == null || pendingResetEmail.isBlank()) {
                showLogin();
                setInlineError(lblLoginIdentityError, "Relancez la procedure de mot de passe oublie.");
                return;
            }

            String code = value(tfResetCode).trim();
            String password = getResetPassword();
            String confirmPassword = value(pfResetConfirmPassword);
            boolean hasInputError = false;

            if (code.isBlank()) {
                setInlineError(lblResetCodeError, "Code de reinitialisation obligatoire");
                hasInputError = true;
            }

            if (password == null || password.isBlank()) {
                setInlineError(lblResetPasswordError, "Nouveau mot de passe obligatoire");
                hasInputError = true;
            }

            if (confirmPassword.isBlank()) {
                setInlineError(lblResetConfirmPasswordError, "Confirmation obligatoire");
                hasInputError = true;
            }

            if (hasInputError) {
                return;
            }

            if (!password.equals(confirmPassword)) {
                setInlineError(lblResetConfirmPasswordError, "Doit etre identique au nouveau mot de passe");
                return;
            }

            String policyError = serviceUser.getPasswordPolicyMessage(password);
            if (!policyError.isEmpty()) {
                setInlineError(lblResetPasswordError, policyError);
                return;
            }

            if (!validateCaptcha(resetCaptchaAnswer, tfResetCaptcha, lblResetCaptchaError, this::refreshResetCaptcha)) {
                return;
            }

            boolean reset = serviceUser.resetPassword(pendingResetEmail, code, password);
            if (!reset) {
                setInlineError(lblResetCodeError, "Code invalide ou expire");
                return;
            }

            tfLoginIdentity.setText(pendingResetEmail);
            setLoginPasswordValue(password);
            if (!loginPasswordVisible) {
                loginPasswordVisible = true;
                applyLoginPasswordVisibility();
            }

            showAlert(Alert.AlertType.INFORMATION, "Reset Password", "Mot de passe reinitialise. Vous pouvez maintenant vous connecter.");
            showLogin();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        } catch (IllegalArgumentException e) {
            setInlineError(lblResetPasswordError, fallbackMessage(e));
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Email", e.getMessage());
        }
    }

    @FXML
    private void handleResendResetCode() {
        try {
            clearResetInlineErrors();
            if (pendingResetEmail == null || pendingResetEmail.isBlank()) {
                showLogin();
                setInlineError(lblLoginIdentityError, "Relancez la procedure de mot de passe oublie.");
                return;
            }

            if (!validateCaptcha(resetCaptchaAnswer, tfResetCaptcha, lblResetCaptchaError, this::refreshResetCaptcha)) {
                return;
            }

            serviceUser.sendPasswordResetCode(pendingResetEmail);
            showAlert(Alert.AlertType.INFORMATION, "Reset Password", "Un nouveau code a ete envoye.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        } catch (IllegalArgumentException e) {
            setInlineError(lblResetCodeError, fallbackMessage(e));
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Email", e.getMessage());
        }
    }

    @FXML
    private void handleSuggestPassword() {
        clearSignupInlineErrors();

        String suggestedPassword = serviceUser.generateSuggestedPassword();
        setSignupPasswordValue(suggestedPassword);
        pfSignupConfirmPassword.setText(suggestedPassword);

        if (!signupPasswordVisible) {
            signupPasswordVisible = true;
            applySignupPasswordVisibility();
        }
    }

    @FXML
    private void handleSuggestResetPassword() {
        clearResetInlineErrors();

        String suggestedPassword = serviceUser.generateSuggestedPassword();
        setResetPasswordValue(suggestedPassword);
        pfResetConfirmPassword.setText(suggestedPassword);

        if (!resetPasswordVisible) {
            resetPasswordVisible = true;
            applyResetPasswordVisibility();
        }
    }

    @FXML
    private void handleGoogleAuth() {
        try {
            if (signupBox.isVisible()) {
                clearSignupInlineErrors();
                if (!validateCaptcha(signupCaptchaAnswer, tfSignupCaptcha, lblSignupCaptchaError, this::refreshSignupCaptcha)) {
                    return;
                }
            } else {
                clearLoginInlineErrors();
            }

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

    @FXML
    private void handleFaceLogin() {
        try {
            clearLoginInlineErrors();

            Stage owner = (Stage) tfLoginIdentity.getScene().getWindow();
            User user = FaceCaptureUtil.recognizeFace(
                    owner,
                    "Login with Face",
                    "Look at the camera and the app will recognize your enrolled face automatically.",
                    frame -> {
                        try {
                            String signature = serviceUser.buildFaceSignature(frame);
                            return serviceUser.loginWithFace(signature);
                        } catch (SQLException e) {
                            throw new IllegalStateException("Erreur SQL: " + e.getMessage(), e);
                        }
                    }
            );
            if (user == null) {
                setInlineError(lblLoginIdentityError, "Aucune Face ID reconnue. Essayez a nouveau.");
                return;
            }

            SessionManager.setCurrentUser(user);
            openHomePage();
        } catch (IllegalArgumentException | IllegalStateException e) {
            String message = fallbackMessage(e);
            if (message.toLowerCase().contains("face")) {
                setInlineError(lblLoginIdentityError, message);
            } else {
                showAlert(Alert.AlertType.WARNING, "Face ID", message);
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void showSignup() {
        loginBox.setVisible(false);
        loginBox.setManaged(false);
        resetBox.setVisible(false);
        resetBox.setManaged(false);
        signupBox.setVisible(true);
        signupBox.setManaged(true);
        tfSignupCaptcha.clear();
        refreshSignupCaptcha();
    }

    @FXML
    private void showLogin() {
        signupBox.setVisible(false);
        signupBox.setManaged(false);
        resetBox.setVisible(false);
        resetBox.setManaged(false);
        loginBox.setVisible(true);
        loginBox.setManaged(true);
    }

    @FXML
    private void showReset() {
        loginBox.setVisible(false);
        loginBox.setManaged(false);
        signupBox.setVisible(false);
        signupBox.setManaged(false);
        resetBox.setVisible(true);
        resetBox.setManaged(true);
        tfResetCaptcha.clear();
        refreshResetCaptcha();
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

    @FXML
    private void toggleResetPasswordVisibility() {
        resetPasswordVisible = !resetPasswordVisible;
        if (resetPasswordVisible) {
            tfResetPasswordVisible.setText(pfResetPassword.getText());
        } else {
            pfResetPassword.setText(tfResetPasswordVisible.getText());
        }
        applyResetPasswordVisibility();
    }

    @FXML
    private void refreshSignupCaptcha() {
        signupCaptchaAnswer = refreshCaptcha(signupCaptchaCanvas);
        tfSignupCaptcha.clear();
        setInlineError(lblSignupCaptchaError, "");
    }

    @FXML
    private void refreshResetCaptcha() {
        resetCaptchaAnswer = refreshCaptcha(resetCaptchaCanvas);
        tfResetCaptcha.clear();
        setInlineError(lblResetCaptchaError, "");
    }

    private void refreshAllCaptchas() {
        refreshSignupCaptcha();
        refreshResetCaptcha();
    }

    private void applyLoginPasswordVisibility() {
        pfLoginPassword.setVisible(!loginPasswordVisible);
        pfLoginPassword.setManaged(!loginPasswordVisible);
        tfLoginPasswordVisible.setVisible(loginPasswordVisible);
        tfLoginPasswordVisible.setManaged(loginPasswordVisible);
        btnToggleLoginPassword.setText(loginPasswordVisible ? "Hide" : "Show");
    }

    private void applySignupPasswordVisibility() {
        pfSignupPassword.setVisible(!signupPasswordVisible);
        pfSignupPassword.setManaged(!signupPasswordVisible);
        tfSignupPasswordVisible.setVisible(signupPasswordVisible);
        tfSignupPasswordVisible.setManaged(signupPasswordVisible);
        btnToggleSignupPassword.setText(signupPasswordVisible ? "Hide" : "Show");
    }

    private void applyResetPasswordVisibility() {
        pfResetPassword.setVisible(!resetPasswordVisible);
        pfResetPassword.setManaged(!resetPasswordVisible);
        tfResetPasswordVisible.setVisible(resetPasswordVisible);
        tfResetPasswordVisible.setManaged(resetPasswordVisible);
        btnToggleResetPassword.setText(resetPasswordVisible ? "Hide" : "Show");
    }

    private String getLoginPassword() {
        return loginPasswordVisible ? tfLoginPasswordVisible.getText() : pfLoginPassword.getText();
    }

    private String getSignupPassword() {
        return signupPasswordVisible ? tfSignupPasswordVisible.getText() : pfSignupPassword.getText();
    }

    private String getResetPassword() {
        return resetPasswordVisible ? tfResetPasswordVisible.getText() : pfResetPassword.getText();
    }

    private void setSignupPasswordValue(String password) {
        pfSignupPassword.setText(password);
        tfSignupPasswordVisible.setText(password);
    }

    private void setLoginPasswordValue(String password) {
        pfLoginPassword.setText(password);
        tfLoginPasswordVisible.setText(password);
    }

    private void setResetPasswordValue(String password) {
        pfResetPassword.setText(password);
        tfResetPasswordVisible.setText(password);
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private void clearSignupInlineErrors() {
        setInlineError(lblSignupEmailError, "");
        setInlineError(lblSignupUsernameError, "");
        setInlineError(lblSignupPasswordError, "");
        setInlineError(lblSignupConfirmPasswordError, "");
        setInlineError(lblSignupCaptchaError, "");
    }

    private void clearLoginInlineErrors() {
        setInlineError(lblLoginIdentityError, "");
        setInlineError(lblLoginPasswordError, "");
    }

    private void clearResetInlineErrors() {
        setInlineError(lblResetCodeError, "");
        setInlineError(lblResetPasswordError, "");
        setInlineError(lblResetConfirmPasswordError, "");
        setInlineError(lblResetCaptchaError, "");
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

    private boolean validateCaptcha(String expected, TextField input, Label errorLabel, Runnable refreshAction) {
        String userValue = value(input).trim();
        if (userValue.isBlank()) {
            setInlineError(errorLabel, "Captcha obligatoire");
            return false;
        }

        if (expected == null || !expected.equalsIgnoreCase(userValue)) {
            setInlineError(errorLabel, "Captcha invalide. Essayez le nouveau sceau.");
            refreshAction.run();
            return false;
        }

        return true;
    }

    private String refreshCaptcha(Canvas canvas) {
        String captcha = generateCaptchaCode();
        drawCaptcha(canvas, captcha);
        return captcha;
    }

    private String generateCaptchaCode() {
        StringBuilder builder = new StringBuilder(CAPTCHA_LENGTH);
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            int index = secureRandom.nextInt(CAPTCHA_ALPHABET.length());
            builder.append(CAPTCHA_ALPHABET.charAt(index));
        }
        return builder.toString();
    }

    private void drawCaptcha(Canvas canvas, String captcha) {
        if (canvas == null) {
            return;
        }

        canvas.setWidth(CAPTCHA_WIDTH);
        canvas.setHeight(CAPTCHA_HEIGHT);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, CAPTCHA_WIDTH, CAPTCHA_HEIGHT);
        gc.setFill(Color.web("#121018"));
        gc.fillRoundRect(0, 0, CAPTCHA_WIDTH, CAPTCHA_HEIGHT, 18, 18);

        for (int i = 0; i < 34; i++) {
            gc.setFill(Color.rgb(139 + secureRandom.nextInt(70), 92, 246, 0.08 + secureRandom.nextDouble() * 0.12));
            double x = secureRandom.nextDouble() * CAPTCHA_WIDTH;
            double y = secureRandom.nextDouble() * CAPTCHA_HEIGHT;
            double size = 2 + secureRandom.nextDouble() * 5;
            gc.fillOval(x, y, size, size);
        }

        for (int i = 0; i < 5; i++) {
            gc.setStroke(i % 2 == 0 ? Color.rgb(186, 158, 255, 0.38) : Color.rgb(139, 92, 246, 0.28));
            gc.setLineWidth(1.5 + secureRandom.nextDouble() * 1.7);
            gc.strokeLine(
                    secureRandom.nextDouble() * 24,
                    secureRandom.nextDouble() * CAPTCHA_HEIGHT,
                    CAPTCHA_WIDTH - secureRandom.nextDouble() * 24,
                    secureRandom.nextDouble() * CAPTCHA_HEIGHT
            );
        }

        double spacing = CAPTCHA_WIDTH / (captcha.length() + 1.35);
        for (int i = 0; i < captcha.length(); i++) {
            char character = captcha.charAt(i);
            double x = 28 + spacing * i;
            double y = 50 + secureRandom.nextDouble() * 9;
            double angle = -18 + secureRandom.nextDouble() * 36;

            gc.save();
            gc.setTransform(new Affine(new Rotate(angle, x, y)));
            gc.setFont(Font.font("Manrope", FontWeight.EXTRA_BOLD, 30 + secureRandom.nextDouble() * 9));
            gc.setFill(i % 2 == 0 ? Color.web("#ebe7ff") : Color.web("#d9d1ff"));
            gc.fillText(String.valueOf(character), x, y);
            gc.restore();
        }
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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/HomeFront.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) tfLoginIdentity.getScene().getWindow();
        stage.setTitle("Shadow Dimensions - The Void");
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

    private String value(TextField field) {
        return field.getText() == null ? "" : field.getText();
    }

    private String fallbackMessage(Exception e) {
        return e.getMessage() == null ? "Erreur de validation." : e.getMessage();
    }
}
