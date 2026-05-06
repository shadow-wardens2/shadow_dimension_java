package Controllers.User;

import Controllers.Marketplace.Back.*;
import Controllers.event.EventSelectorController;
import Controllers.Tutorials.*;
import Controllers.User.VaultController;
import Controllers.User.EditProfileContentController;
import Controllers.User.ManagementUsersController;
import Entities.User.User;
import Utils.SessionManager;
import Utils.AvatarUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
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
    private Label lbUserAvatar;

    @FXML
    private ImageView imgUserAvatar;

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

    @FXML
    private Button btnForumManagement;

    @FXML
    private Button btnForumCategoryManagement;

    @FXML
    private Button btnForumStatistics;

    // Initial landing content + auth UI state.
    @FXML
    public void initialize() {
        loadPage("/HomeContent.fxml");
        refreshAuthUi();
    }

    // Sidebar navigation actions.
    @FXML
    void openFrontOffice(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HomeFront.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setTitle("Shadow Dimensions - The Void");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        loadPage("/Marketplace/Back/MarketplaceSelector.fxml");
    }

    @FXML
    void openMarketplaceStatistics(ActionEvent event) {
        loadPage("/Marketplace/Back/MarketplaceStatisticsContent.fxml");
    }

    @FXML
    void openTutorialsManagement(ActionEvent event) {
        loadPage("/Tutorials/TutorialsSelector.fxml");
    }

    @FXML
    void openEventManagement(ActionEvent event) {
        loadPage("/event/EventSelector.fxml");
    }

    @FXML
    void openForumManagement(ActionEvent event) {
        loadPage("/Forum/ForumBack.fxml");
    }

    @FXML
    void openForumCategoryManagement(ActionEvent event) {
        loadPage("/Forum/CategoryBack.fxml");
    }

    @FXML
    void openForumStatistics(ActionEvent event) {
        loadPage("/Forum/ForumStatistics.fxml");
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

        if (loggedIn) {
            User user = SessionManager.getCurrentUser();
            String username = user.getUsername() == null || user.getUsername().isBlank() ? "Shadow Dweller"
                    : user.getUsername();
            lbUserName.setText(username);
            AvatarUtil.applyDiceBearAvatar(imgUserAvatar, lbUserAvatar, user, 40);
            btnTopAuth.setText("Logout");
            btnBottomAuth.setText("Logout");
            btnUserManagement.setVisible(user.isAdmin());
            btnUserManagement.setManaged(user.isAdmin());
            btnUserStatistics.setVisible(user.isAdmin());
            btnUserStatistics.setManaged(user.isAdmin());
            if (btnForumManagement != null) {
                btnForumManagement.setVisible(user.isAdmin());
                btnForumManagement.setManaged(user.isAdmin());
            }
            if (btnForumCategoryManagement != null) {
                btnForumCategoryManagement.setVisible(user.isAdmin());
                btnForumCategoryManagement.setManaged(user.isAdmin());
            }
            if (btnForumStatistics != null) {
                btnForumStatistics.setVisible(user.isAdmin());
                btnForumStatistics.setManaged(user.isAdmin());
            }
        } else {
            lbUserName.setText("Shadow Dweller");
            AvatarUtil.applyDiceBearAvatar(imgUserAvatar, lbUserAvatar, null, 40);
            btnTopAuth.setText("Connect Soul");
            btnBottomAuth.setText("Connect Soul");
            btnUserManagement.setVisible(false);
            btnUserManagement.setManaged(false);
            btnUserStatistics.setVisible(false);
            btnUserStatistics.setManaged(false);
            if (btnForumManagement != null) {
                btnForumManagement.setVisible(false);
                btnForumManagement.setManaged(false);
            }
            if (btnForumCategoryManagement != null) {
                btnForumCategoryManagement.setVisible(false);
                btnForumCategoryManagement.setManaged(false);
            }
            if (btnForumStatistics != null) {
                btnForumStatistics.setVisible(false);
                btnForumStatistics.setManaged(false);
            }
        }
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
            } else if (controller instanceof ManagementCommandeController) {
                ((ManagementCommandeController) controller).setDashboardContext(this);
            } else if (controller instanceof MarketplaceStatisticsController) {
                ((MarketplaceStatisticsController) controller).setDashboardContext(this);
            }
            if (controller instanceof EventSelectorController) {
                ((EventSelectorController) controller).setDashboardContext(this);
            } else if (controller instanceof Controllers.Tutorials.TutorialsSelectorController) {
                ((Controllers.Tutorials.TutorialsSelectorController) controller).setDashboardContext(this);
            } else if (controller instanceof VaultController) {
                ((VaultController) controller).setDashboardContext(this);
            } else if (controller instanceof EditProfileContentController) {
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

