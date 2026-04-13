package Controllers.User;

import Controllers.Marketplace.MarketplaceSelectorController;
import Controllers.Marketplace.ManagementCategorieController;
import Controllers.Marketplace.ManagementProduitController;
import Controllers.Marketplace.ManagementTypeController;
import Controllers.Marketplace.PageHost;
import Entities.User.User;
import Utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class HomePageController implements PageHost {

    @FXML
    private StackPane contentArea;

    @FXML
    private Label lbUserName;

    @FXML
    private Button btnTopAuth;

    @FXML
    private Button btnBottomAuth;

    @FXML
    private Button btnUserManagement;

    @FXML
    public void initialize() {
        loadPage("/HomeContent.fxml");
        refreshAuthUi();
    }

    @FXML
    void openHome(ActionEvent event) {
        loadPage("/HomeContent.fxml");
    }

    @FXML
    void openVault() {
        loadPage("/User/VaultContent.fxml");
    }

    @FXML
    void openMarketplaceManagement(ActionEvent event) {
        if (!SessionManager.isLoggedIn()) {
            handleAuthAction(null);
            return;
        }
        loadPage("/Marketplace/MarketplaceSelector.fxml");
    }

    @FXML
    void openUserManagement(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user != null && user.isAdmin()) {
            loadPage("/User/ManagementUsersContent.fxml");
        }
    }

    @FXML
    void handleAuthAction(ActionEvent event) {
        if (SessionManager.isLoggedIn()) {
            SessionManager.clear();
            refreshAuthUi();
            loadPage("/HomeContent.fxml");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/ConnectSoul.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setTitle("Connect Soul");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshAuthUi() {
        if (SessionManager.isLoggedIn()) {
            User user = SessionManager.getCurrentUser();
            String username = user.getUsername() == null || user.getUsername().isBlank() ? "Shadow Dweller" : user.getUsername();
            lbUserName.setText(username);
            btnTopAuth.setText("Logout");
            btnBottomAuth.setText("Logout");
            btnUserManagement.setVisible(user.isAdmin());
            btnUserManagement.setManaged(user.isAdmin());
        } else {
            lbUserName.setText("Shadow Dweller");
            btnTopAuth.setText("Connect Soul");
            btnBottomAuth.setText("Connect Soul");
            btnUserManagement.setVisible(false);
            btnUserManagement.setManaged(false);
        }
    }

    public void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof MarketplaceSelectorController) {
                ((MarketplaceSelectorController) controller).setDashboardContext(this);
            } else if (controller instanceof ManagementCategorieController) {
                ((ManagementCategorieController) controller).setDashboardContext(this);
            } else if (controller instanceof ManagementProduitController) {
                ((ManagementProduitController) controller).setDashboardContext(this);
            } else if (controller instanceof ManagementTypeController) {
                ((ManagementTypeController) controller).setDashboardContext(this);
            }
            if (controller instanceof VaultController) {
                ((VaultController) controller).setDashboardContext(this);
            }
            if (controller instanceof EditProfileContentController) {
                ((EditProfileContentController) controller).setDashboardContext(this);
            }
            if (controller instanceof ManagementUsersController) {
                // no context needed yet, but this keeps room for shared host behavior later
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
