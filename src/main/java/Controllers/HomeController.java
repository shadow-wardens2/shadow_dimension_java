package Controllers;

import Utils.SessionManager;
import Utils.AvatarUtil;
import Entities.User.User;
import Entities.Marketplace.Categorie;
import Entities.Marketplace.Produit;
import Services.Marketplace.AiRecommendationService;
import Services.Marketplace.ServiceCategorie;
import Services.Marketplace.ServiceCommande;
import Services.Marketplace.ServiceProduit;
import Controllers.Marketplace.Front.FrontProductCardController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.List;

public class HomeController {

    @FXML
    private javafx.scene.layout.AnchorPane rootNode;
    @FXML
    private Button btnDashboard;
    @FXML private Button btnAuth;
    @FXML private Button btnLogout;
    @FXML private ImageView imgUserAvatar;
    @FXML private Label lbUserAvatar;

    @FXML private VBox recommendationBox;
    @FXML private HBox recommendationsContainer;
    @FXML private Label recommendationSubtitle;

    @FXML
    public void initialize() {
        if (SessionManager.isLoggedIn()) {
            User user = SessionManager.getCurrentUser();
            if (user.isAdmin()) {
                btnDashboard.setVisible(true);
                btnDashboard.setManaged(true);
            }
            if (btnAuth != null) {
                btnAuth.setText("My Soul");
            }
            if (imgUserAvatar != null && lbUserAvatar != null) {
                AvatarUtil.applyDiceBearAvatar(imgUserAvatar, lbUserAvatar, user, 42);
                imgUserAvatar.setVisible(true);
                imgUserAvatar.setManaged(true);
                lbUserAvatar.setVisible(true);
                lbUserAvatar.setManaged(true);
            }
            loadAiRecommendations();
            if (btnLogout != null) {
                btnLogout.setVisible(true);
                btnLogout.setManaged(true);
            }
        } else if (lbUserAvatar != null && imgUserAvatar != null) {
            lbUserAvatar.setVisible(false);
            lbUserAvatar.setManaged(false);
            imgUserAvatar.setVisible(false);
            imgUserAvatar.setManaged(false);
        }
    }

    private void loadAiRecommendations() {
        new Thread(() -> {
            try {
                ServiceProduit sp = new ServiceProduit();
                ServiceCategorie sc = new ServiceCategorie();
                List<Produit> allProducts = sp.getAll();
                List<Categorie> allCategories = sc.getAll();

                ServiceCommande scmd = new ServiceCommande();
                List<Produit> pastOrders = scmd.getOrderedProductsByUserId(SessionManager.getCurrentUser().getId());
                
                AiRecommendationService aiService = new AiRecommendationService();
                List<Produit> recommendations = aiService.getRecommendations(allProducts, pastOrders);
                
                if (!recommendations.isEmpty() && recommendationBox != null && recommendationsContainer != null) {
                    boolean fallbackUsed = aiService.isFallback();
                    Platform.runLater(() -> {
                        if (recommendationSubtitle != null) {
                            if (fallbackUsed) {
                                recommendationSubtitle.setText("Here are some popular relics from the Void while we reconnect with the AI Oracle.");
                                recommendationSubtitle.setStyle("-fx-text-fill: #a1a1aa;");
                            } else {
                                recommendationSubtitle.setText("Our ShadowAI has analyzed your soul's journey to find these perfect relics.");
                                recommendationSubtitle.setStyle("-fx-text-fill: #d3bbff;");
                            }
                        }
                        recommendationBox.setVisible(true);
                        recommendationBox.setManaged(true);
                        recommendationsContainer.getChildren().clear();
                        for (Produit p : recommendations) {
                            try {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/Front/FrontProductCard.fxml"));
                                VBox card = loader.load();
                                FrontProductCardController controller = loader.getController();
                                String catName = allCategories.stream()
                                        .filter(c -> c.getId() == p.getCategorieId())
                                        .findFirst()
                                        .map(Categorie::getNom)
                                        .orElse("Unknown Tier");
                                controller.setData(p, catName);
                                recommendationsContainer.getChildren().add(card);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    void navigateToMarketplace() {
        loadPage("/Marketplace/Front/MarketplaceFront.fxml");
    }

    @FXML
    void navigateToVault() {
        loadPage("/User/VaultFront.fxml");
    }

    @FXML
    void navigateToTutorials() {
        if (SessionManager.isLoggedIn()) {
            loadPage("/Tutorials/TutorialsFront.fxml");
        } else {
            loadPage("/User/ConnectSoul.fxml");
        }
    }

    @FXML
    void navigateToManagement() {
        loadPage("/Tutorials/TutorialsSelector.fxml");
    }

    @FXML
    void navigateToEvents() {
        loadPage("/event/Front/EventFront.fxml");
    }

    @FXML
    void navigateToForum() {
        loadPage("/Forum/ForumFront.fxml");
    }

    @FXML
    void handleOpenDashboard() {
        loadPage("/HomePage.fxml");
    }

    @FXML
    void handleAuthAction() {
        if (SessionManager.isLoggedIn()) {
            navigateToVault();
        } else {
            loadPage("/User/ConnectSoul.fxml");
        }
    }

    @FXML
    void handleLogout() {
        SessionManager.clear();
        loadPage("/HomeFront.fxml");
    }

    private void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            rootNode.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEnterNight() {
        navigateToMarketplace();
    }
}
