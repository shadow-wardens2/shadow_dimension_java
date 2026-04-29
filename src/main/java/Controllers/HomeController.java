package Controllers;

import Utils.SessionManager;
import Entities.User.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import Entities.Marketplace.Categorie;
import Entities.Marketplace.Produit;
import Services.Marketplace.AiRecommendationService;
import Services.Marketplace.ServiceCategorie;
import Services.Marketplace.ServiceCommande;
import Services.Marketplace.ServiceProduit;
import Controllers.Marketplace.Front.FrontProductCardController;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import java.util.List;
import java.io.IOException;

public class HomeController {

    @FXML
    private javafx.scene.layout.AnchorPane rootNode;
    @FXML
    private Button btnDashboard;
    @FXML private Button btnAuth;

    @FXML private VBox recommendationBox;
    @FXML private HBox recommendationsContainer;

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
            loadAiRecommendations();
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
                    Platform.runLater(() -> {
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

<<<<<<< HEAD

=======
    @FXML
    void navigateToTutorials() {
        if (SessionManager.isLoggedIn()) {
            loadPage("/Tutorials/TutorialsFront.fxml");
        } else {
            loadPage("/User/ConnectSoul.fxml");
        }
    }
>>>>>>> 73fe0a5ccaa4f1602eae93ef8d62a25d03c4a37b

    @FXML
    void navigateToManagement() {
        loadPage("/Tutorials/TutorialsSelector.fxml");
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
