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

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import java.io.IOException;
import java.util.Random;

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
    void openArtworksManagement(ActionEvent event) {
        loadPage("/Artworks/ListerArtworks.fxml");
    }

    @FXML
    void openArtworksStatistics(ActionEvent event) {
        loadPage("/Artworks/ArtworksStatistics.fxml");
    }

    @FXML
    void openEvaluationsManagement(ActionEvent event) {
        loadPage("/Artworks/EvaluationsManagement.fxml");
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
        } else {
            lbUserName.setText("Shadow Dweller");
            AvatarUtil.applyDiceBearAvatar(imgUserAvatar, lbUserAvatar, null, 40);
            btnTopAuth.setText("Connect Soul");
            btnBottomAuth.setText("Connect Soul");
            btnUserManagement.setVisible(false);
            btnUserManagement.setManaged(false);
            btnUserStatistics.setVisible(false);
            btnUserStatistics.setManaged(false);
        }
    }

    // Loads a center page and injects dashboard context when needed.
    public Object loadPage(String fxmlPath) {
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
            if (controller instanceof Controllers.Artworks.ListerArtworksController) {
                ((Controllers.Artworks.ListerArtworksController) controller).setDashboardContext(this);
            }
            if (controller instanceof Controllers.Artworks.AjouterArtworkController) {
                ((Controllers.Artworks.AjouterArtworkController) controller).setDashboardContext(this);
            }
            if (controller instanceof Controllers.Artworks.DetailArtworkController) {
                ((Controllers.Artworks.DetailArtworkController) controller).setDashboardContext(this);
            }
            if (controller instanceof Controllers.Artworks.ListerCategoriesController) {
                ((Controllers.Artworks.ListerCategoriesController) controller).setDashboardContext(this);
            }
            if (controller instanceof Controllers.Artworks.AjouterCategoryController) {
                ((Controllers.Artworks.AjouterCategoryController) controller).setDashboardContext(this);
            }
            if (controller instanceof Controllers.Artworks.ArtworksStatisticsController) {
                ((Controllers.Artworks.ArtworksStatisticsController) controller).setDashboardContext(this);
            }
            if (controller instanceof Controllers.Artworks.EvaluationsManagementController) {
                // Evaluations controller doesn't need context currently but could in future
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);
            return controller;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    // PageHost hook (currently no-op).
    public void refreshStatistics() {
        // Implementation for refreshing statistics if needed
    }
}

