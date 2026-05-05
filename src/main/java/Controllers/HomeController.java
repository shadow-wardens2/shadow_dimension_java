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

    @FXML private javafx.scene.layout.AnchorPane rootNode;
    @FXML private Button btnDashboard;

    @FXML private VBox recommendationBox;
    @FXML private HBox recommendationsContainer;
    @FXML private javafx.scene.Group ghostGroup;
    @FXML private javafx.scene.layout.Pane mascotGlow;
    @FXML private javafx.scene.layout.Region leftPupil;
    @FXML private javafx.scene.layout.Region rightPupil;

    @FXML
    public void initialize() {
        if (SessionManager.isLoggedIn()) {
            User user = SessionManager.getCurrentUser();
            if (user.isAdmin()) {
                btnDashboard.setVisible(true);
                btnDashboard.setManaged(true);
            }
            loadAiRecommendations();
        }
        setupMascotAnimation();
        setupEyeTracking();
    }

    private void setupEyeTracking() {
        if (rootNode != null && leftPupil != null && rightPupil != null) {
            rootNode.setOnMouseMoved(event -> {
                double mouseX = event.getSceneX();
                double mouseY = event.getSceneY();

                updatePupil(leftPupil, mouseX, mouseY);
                updatePupil(rightPupil, mouseX, mouseY);
            });
        }
    }

    private void updatePupil(javafx.scene.layout.Region pupil, double mouseX, double mouseY) {
        javafx.geometry.Point2D pupilScenePos = pupil.localToScene(pupil.getWidth() / 2, pupil.getHeight() / 2);
        
        double dx = mouseX - pupilScenePos.getX();
        double dy = mouseY - pupilScenePos.getY();
        
        double angle = Math.atan2(dy, dx);
        double distance = Math.min(6, Math.sqrt(dx * dx + dy * dy) / 20); // Max 6px move
        
        pupil.setTranslateX(Math.cos(angle) * distance);
        pupil.setTranslateY(Math.sin(angle) * distance);
    }

    private void setupMascotAnimation() {
        if (ghostGroup != null) {
            // Floating effect
            javafx.animation.TranslateTransition floatAnim = new javafx.animation.TranslateTransition(javafx.util.Duration.seconds(3), ghostGroup);
            floatAnim.setByY(-20);
            floatAnim.setCycleCount(javafx.animation.Animation.INDEFINITE);
            floatAnim.setAutoReverse(true);
            floatAnim.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
            floatAnim.play();

            // Pulse effect for glow
            if (mascotGlow != null) {
                javafx.animation.FadeTransition pulse = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(2), mascotGlow);
                pulse.setFromValue(0.2);
                pulse.setToValue(0.6);
                pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
                pulse.setAutoReverse(true);
                pulse.play();
            }
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
        if (SessionManager.isLoggedIn()) {
            loadPage("/Marketplace/Front/MarketplaceFront.fxml");
        } else {
            loadPage("/User/ConnectSoul.fxml");
        }
    }

    @FXML
    void navigateToArtworks() {
        loadPage("/Artworks/ArtworksFront.fxml");
    }

    @FXML
    void navigateToManagement() {
        if (SessionManager.isLoggedIn()) {
            loadPage("/Marketplace/Back/MarketplaceManagement.fxml");
        } else {
            loadPage("/User/ConnectSoul.fxml");
        }
    }

    @FXML
    void handleOpenDashboard() {
        loadPage("/HomePage.fxml");
    }

    @FXML
    void navigateToLogin() {
        loadPage("/User/ConnectSoul.fxml");
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
