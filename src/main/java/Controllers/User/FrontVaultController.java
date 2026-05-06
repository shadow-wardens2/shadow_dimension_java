package Controllers.User;

import Entities.Marketplace.Commande;
import Entities.User.User;
import Services.Marketplace.ServiceCommande;
import Services.Marketplace.MailService;
import Services.User.ServiceUser;
import Utils.FaceCaptureUtil;
import Utils.SessionManager;
import Utils.AvatarUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class FrontVaultController {

    @FXML
    private AnchorPane rootNode;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnAuth;

    @FXML
    private Button btnLogout;

    @FXML
    private Label lbVaultTitle;

    @FXML
    private Label lbAuthState;

    @FXML
    private Label lbAvatar;

    @FXML
    private ImageView imgAvatar;

    @FXML
    private Label lbUsername;

    @FXML
    private Label lbSummaryUsername;

    @FXML
    private Label lbEmail;

    @FXML
    private Label lbFullName;

    @FXML
    private Label lbPhone;

    @FXML
    private Label lbCountry;

    @FXML
    private Label lbSummaryCountry;

    @FXML
    private Label lbCity;

    @FXML
    private Label lbBio;

    @FXML
    private Label lbFaceIdStatus;

    @FXML
    private Label lbFaceIdHint;

    @FXML
    private Button btnFaceIdAction;

    @FXML
    private Button btnDisableFaceId;

    @FXML private TableView<Commande> ordersTable;
    @FXML private TableColumn<Commande, Integer> colOrderId;
    @FXML private TableColumn<Commande, String> colDate;
    @FXML private TableColumn<Commande, String> colTotal;
    @FXML private TableColumn<Commande, String> colStatus;
    @FXML private TableColumn<Commande, Void> colAction;

    private final ServiceUser serviceUser = new ServiceUser();
    private final ServiceCommande serviceCommande = new ServiceCommande();
    private ObservableList<Commande> userOrders = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn()) {
            if (btnAuth != null) {
                btnAuth.setText("Connect Soul");
            }
            if (btnLogout != null) {
                btnLogout.setVisible(false);
                btnLogout.setManaged(false);
            }
            lbVaultTitle.setText("Guest Vault");
            lbAuthState.setText("Enter the void freely. Connect your soul whenever you want to bind your identity.");
            AvatarUtil.applyDiceBearAvatar(imgAvatar, lbAvatar, null, 52);
            lbUsername.setText("Shadow Dweller");
            lbSummaryUsername.setText("Shadow Dweller");
            lbEmail.setText("-");
            lbFullName.setText("-");
            lbPhone.setText("-");
            lbCountry.setText("-");
            lbSummaryCountry.setText("-");
            lbCity.setText("-");
            lbBio.setText("No soul record detected.");
            applyGuestFaceIdState();
            return;
        }

        User user = SessionManager.getCurrentUser();
        if (user != null && user.isAdmin()) {
            btnDashboard.setVisible(true);
            btnDashboard.setManaged(true);
        }

        if (btnAuth != null) {
            btnAuth.setText("Edit Profile");
        }
        if (btnLogout != null) {
            btnLogout.setVisible(true);
            btnLogout.setManaged(true);
        }

        String username = safe(user.getUsername(), "Shadow Dweller");
        lbVaultTitle.setText(username + " Vault");
        lbAuthState.setText("Soul synchronized. Your identity and relic trail are bound to the void.");
        AvatarUtil.applyDiceBearAvatar(imgAvatar, lbAvatar, user, 52);
        lbUsername.setText(username);
        lbSummaryUsername.setText(username);
        lbEmail.setText(safe(user.getEmail(), "-"));
        lbFullName.setText(safe(user.getFullName(), "-"));
        lbPhone.setText(safe(user.getPhone(), "-"));
        lbCountry.setText(safe(user.getCountry(), "-"));
        lbSummaryCountry.setText(safe(user.getCountry(), "-"));
        lbCity.setText(safe(user.getCity(), "-"));
        lbBio.setText(safe(user.getBio(), "No bio engraved yet."));
        refreshFaceIdState(user);
        setupOrdersTable();
        loadUserOrders(user.getId());
    }

    private void setupOrdersTable() {
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateCommande().toString()));
        colTotal.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f TND", cellData.getValue().getTotalAmount())));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnCancel = new Button("Request Cancellation");
            {
                btnCancel.getStyleClass().add("secondary-button");
                btnCancel.setStyle("-fx-background-color: rgba(239, 68, 68, 0.1); -fx-text-fill: #ef4444; -fx-font-size: 11px;");
                btnCancel.setOnAction(event -> {
                    Commande c = getTableView().getItems().get(getIndex());
                    handleCancelRequest(c);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Commande c = getTableView().getItems().get(getIndex());
                    if ("CANCEL_REQUESTED".equals(c.getStatus()) || "CANCELLED".equals(c.getStatus())) {
                        setGraphic(new Label(c.getStatus().replace("_", " ")));
                    } else {
                        setGraphic(btnCancel);
                    }
                }
            }
        });

        ordersTable.setItems(userOrders);
    }

    private void loadUserOrders(int userId) {
        try {
            userOrders.setAll(serviceCommande.getByUserId(userId));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleCancelRequest(Commande c) {
        try {
            serviceCommande.updateStatus(c.getId(), "CANCEL_REQUESTED");
            showAlert(Alert.AlertType.INFORMATION, "Cancellation Requested", "The admin has been notified of your cancellation request.");
            loadUserOrders(c.getUserId());

            // Notify Admin via Email
            new Thread(() -> {
                try {
                    User admin = serviceUser.getFirstActiveAdmin();
                    if (admin != null) {
                        String subject = "Order Cancellation Request: #" + c.getId();
                        String body = "Hello " + admin.getUsername() + ",\n\n" +
                                      "User " + c.getPrenom() + " " + c.getNom() + " (ID: " + c.getUserId() + ") has requested to cancel order #" + c.getId() + ".\n\n" +
                                      "Please review this request in the dashboard.\n\n" +
                                      "Regards,\n" +
                                      "Shadow Dimensions System";
                        MailService.sendMail(admin.getEmail(), subject, body);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not process cancellation request: " + e.getMessage());
        }
    }

    @FXML
    private void navigateToHome() {
        loadPage("/HomeFront.fxml");
    }

    @FXML
    private void navigateToMarketplace() {
        loadPage("/Marketplace/Front/MarketplaceFront.fxml");
    }

    @FXML
    private void navigateToArtworks() {
        loadPage("/Artworks/ArtworksFront.fxml");
    }

    @FXML
    private void navigateToEvents() {
        loadPage("/event/Front/EventFront.fxml");
    }

    @FXML
    private void navigateToTutorials() {
        loadPage("/Tutorials/TutorialsFront.fxml");
    }

    @FXML
    private void navigateToForum() {
        loadPage("/Forum/ForumFront.fxml");
    }

    @FXML
    private void handleOpenDashboard() {
        loadPage("/HomePage.fxml");
    }

    @FXML
    private void handleAuthAction() {
        if (SessionManager.isLoggedIn()) {
            openEditProfile();
        } else {
            loadPage("/User/ConnectSoul.fxml");
        }
    }

    @FXML
    private void handleEditProfile() {
        if (SessionManager.isLoggedIn()) {
            openEditProfile();
        } else {
            loadPage("/User/ConnectSoul.fxml");
        }
    }

    @FXML
    private void handleFaceIdAction() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            loadPage("/User/ConnectSoul.fxml");
            return;
        }

        try {
            Stage owner = (Stage) rootNode.getScene().getWindow();
            List<BufferedImage> captures = FaceCaptureUtil.captureFaceProfile(
                    owner,
                    "Enable Face ID",
                    "Capture your biometric profile. Stay centered and move slightly so Face ID remains stable even if your position changes later."
            );
            if (captures == null || captures.isEmpty()) {
                return;
            }

            String signature = serviceUser.buildFaceSignature(captures);
            serviceUser.enrollFaceId(user.getId(), signature);
            refreshFaceIdState(serviceUser.getById(user.getId()));
            showAlert(Alert.AlertType.INFORMATION, "Face ID", "Face ID activee pour ce compte.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Face ID", e.getMessage());
        }
    }

    @FXML
    private void handleDisableFaceId() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }

        try {
            serviceUser.disableFaceId(user.getId());
            refreshFaceIdState(serviceUser.getById(user.getId()));
            showAlert(Alert.AlertType.INFORMATION, "Face ID", "Face ID desactivee.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        loadPage("/HomeFront.fxml");
    }

    private void openEditProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/EditProfile.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) rootNode.getScene().getWindow();
            stage.setTitle("Edit Profile");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            rootNode.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private void refreshFaceIdState(User user) {
        try {
            boolean enabled = serviceUser.isFaceIdEnabled(user.getId());
            lbFaceIdStatus.setText(enabled ? "Face ID enabled" : "Face ID disabled");
            lbFaceIdHint.setText(enabled
                    ? "Your face signature is ready for quick access from the login screen."
                    : "Enable Face ID to capture your face from this vault and unlock biometric login.");
            btnFaceIdAction.setText(enabled ? "Re-enroll Face ID" : "Enable Face ID");
            btnFaceIdAction.setDisable(false);
            btnDisableFaceId.setDisable(!enabled);
        } catch (SQLException e) {
            lbFaceIdStatus.setText("Face ID unavailable");
            lbFaceIdHint.setText("The biometric profile could not be loaded right now.");
            btnFaceIdAction.setDisable(true);
            btnDisableFaceId.setDisable(true);
        }
    }

    private void applyGuestFaceIdState() {
        lbFaceIdStatus.setText("Face ID locked");
        lbFaceIdHint.setText("Connect Soul first, then enable Face ID from your vault.");
        btnFaceIdAction.setText("Enable Face ID");
        btnFaceIdAction.setDisable(true);
        btnDisableFaceId.setDisable(true);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
