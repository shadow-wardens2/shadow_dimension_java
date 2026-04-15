package Controllers.User;

import Controllers.Marketplace.MarketplaceSelectorController;
import Controllers.Marketplace.ManagementCategorieController;
import Controllers.Marketplace.ManagementProduitController;
import Controllers.Marketplace.ManagementTypeController;
import Controllers.Marketplace.PageHost;
import Controllers.event.EventSelectorController;
import Entities.User.User;
import Utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class HomePageController implements PageHost {

    // Main content host where center pages are swapped dynamically.

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
    private Button btnUserStatistics;

    @FXML
    private Button btnVault;

    @FXML
    private Button btnMarketplaceManagement;

    @FXML
    private Button btnMarketplaceStatistics;

    @FXML
    private Button btnTutorialsManagement;

    @FXML
    private Button btnEventManagement;

    // Initial landing content + auth UI state.
    @FXML
    public void initialize() {
        loadPage("/HomeContent.fxml");
        refreshAuthUi();
    }

    // Sidebar navigation actions.
    @FXML
    void openHome(ActionEvent event) {
        loadPage("/HomeContent.fxml");
    }

    @FXML
    void openVault() {
        if (!requireLoggedIn("My Vault")) {
            return;
        }
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
    void openMarketplaceStatistics(ActionEvent event) {
        if (!requireLoggedIn("Marketplace Statistics")) {
            return;
        }
        loadPage("/Marketplace/MarketplaceStatisticsContent.fxml");
    }

    @FXML
    void openTutorialsManagement(ActionEvent event) {
        if (!requireLoggedIn("Tutorials Management")) {
            return;
        }
        loadPage("/Tutorials/TutorialsSelector.fxml");
    }

    @FXML
    void openEventManagement(ActionEvent event) {
        if (!requireLoggedIn("Event Management")) {
            return;
        }
        loadPage("/event/EventSelector.fxml");
    }

    @FXML
    void openUserManagement(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user != null && user.isAdmin()) {
            loadPage("/User/ManagementUsersContent.fxml");
        }
    }

    @FXML
    void openUserStatistics(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user != null && user.isAdmin()) {
            loadPage("/User/UserStatisticsContent.fxml");
        }
    }

    // Connect Soul / Logout action shared by top and bottom buttons.
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

    // Refreshes labels/buttons and section availability based on session state.
    private void refreshAuthUi() {
        boolean loggedIn = SessionManager.isLoggedIn();

        if (btnVault != null) {
            btnVault.setDisable(!loggedIn);
        }
        if (btnMarketplaceManagement != null) {
            btnMarketplaceManagement.setDisable(!loggedIn);
        }
        if (btnMarketplaceStatistics != null) {
            btnMarketplaceStatistics.setDisable(!loggedIn);
        }
        if (btnTutorialsManagement != null) {
            btnTutorialsManagement.setDisable(!loggedIn);
        }
        if (btnEventManagement != null) {
            btnEventManagement.setDisable(!loggedIn);
        }

        if (loggedIn) {
            User user = SessionManager.getCurrentUser();
            String username = user.getUsername() == null || user.getUsername().isBlank() ? "Shadow Dweller"
                    : user.getUsername();
            lbUserName.setText(username);
            btnTopAuth.setText("Logout");
            btnBottomAuth.setText("Logout");
            btnUserManagement.setVisible(user.isAdmin());
            btnUserManagement.setManaged(user.isAdmin());
            btnUserStatistics.setVisible(user.isAdmin());
            btnUserStatistics.setManaged(user.isAdmin());
        } else {
            lbUserName.setText("Shadow Dweller");
            btnTopAuth.setText("Connect Soul");
            btnBottomAuth.setText("Connect Soul");
            btnUserManagement.setVisible(false);
            btnUserManagement.setManaged(false);
            btnUserStatistics.setVisible(false);
            btnUserStatistics.setManaged(false);
        }
    }

    // Central guard for sections that require authentication.
    private boolean requireLoggedIn(String sectionName) {
        if (SessionManager.isLoggedIn()) {
            return true;
        }

        showAlert(Alert.AlertType.WARNING, "Acces restreint", "Connectez-vous pour acceder a " + sectionName + ".");
        handleAuthAction(null);
        return false;
    }

    // Generic blocking alert helper for home shell.
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Loads a center page and injects dashboard context when needed.
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
            if (controller instanceof EventSelectorController) {
                ((EventSelectorController) controller).setDashboardContext(this);
            }
            if (controller instanceof Controllers.Tutorials.TutorialsSelectorController) {
                ((Controllers.Tutorials.TutorialsSelectorController) controller).setDashboardContext(this);
            }
            if (controller instanceof Controllers.Tutorials.TutorialsSelectorController) {
                ((Controllers.Tutorials.TutorialsSelectorController) controller).setDashboardContext(this);
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
            if (controller instanceof Controllers.Tutorials.ManagementQuizController) {
                ((Controllers.Tutorials.ManagementQuizController) controller).setDashboardContext(this);
            }
            if (controller instanceof Controllers.Tutorials.ManagementQuizDetailsController) {
                ((Controllers.Tutorials.ManagementQuizDetailsController) controller).setDashboardContext(this);
            }
            if (controller instanceof Controllers.Tutorials.ManagementFormationController) {
                ((Controllers.Tutorials.ManagementFormationController) controller).setDashboardContext(this);
            }
            if (controller instanceof Controllers.Tutorials.ManagementJeuController) {
                ((Controllers.Tutorials.ManagementJeuController) controller).setDashboardContext(this);
            }
            if (controller instanceof Controllers.Tutorials.ManagementLeconController) {
                ((Controllers.Tutorials.ManagementLeconController) controller).setDashboardContext(this);
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override

    // PageHost hook (currently no-op).
    public void refreshStatistics() {
        // Implementation for refreshing statistics if needed
    }
}

