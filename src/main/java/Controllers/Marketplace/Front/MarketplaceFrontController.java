package Controllers.Marketplace.Front;

import Entities.Marketplace.Categorie;
import Entities.Marketplace.Produit;
import Services.Marketplace.ServiceCategorie;
import Services.Marketplace.ServiceProduit;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import Utils.SessionManager;
import Entities.User.User;
import javafx.scene.control.Button;

public class MarketplaceFrontController {

    @FXML private TilePane productsGrid;
    @FXML private AnchorPane rootNode;
    @FXML private Button btnDashboard;
    @FXML private Button btnLogout;
    @FXML private javafx.scene.control.Label lbProductCount;
    @FXML private javafx.scene.control.TextField searchField;
    @FXML private javafx.scene.control.ComboBox<String> categoryFilter;

    private ServiceProduit sp = new ServiceProduit();
    private ServiceCategorie sc = new ServiceCategorie();
    
    private List<Produit> allProducts = new ArrayList<>();
    private List<Categorie> allCategories = new ArrayList<>();

    @FXML
    public void initialize() {
        if (SessionManager.isLoggedIn()) {
            User user = SessionManager.getCurrentUser();
            if (user.isAdmin()) {
                btnDashboard.setVisible(true);
                btnDashboard.setManaged(true);
            }
            if (btnLogout != null) {
                btnLogout.setVisible(true);
                btnLogout.setManaged(true);
            }
        }
        
        try {
            allProducts = sp.getAll();
            allCategories = sc.getAll();
            
            categoryFilter.getItems().add("All Categories");
            for (Categorie c : allCategories) {
                categoryFilter.getItems().add(c.getNom());
            }
            categoryFilter.getSelectionModel().selectFirst();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        displayProducts(allProducts);
    }

    @FXML
    void handleSearch() {
        String searchText = searchField.getText().toLowerCase().trim();
        String selectedCategory = categoryFilter.getValue();
        
        List<Produit> filtered = allProducts.stream()
            .filter(p -> {
                boolean matchesSearch = p.getNom().toLowerCase().contains(searchText) || 
                                      p.getDescription().toLowerCase().contains(searchText);
                
                boolean matchesCategory = true;
                if (selectedCategory != null && !selectedCategory.equals("All Categories")) {
                    matchesCategory = allCategories.stream()
                        .anyMatch(c -> c.getNom().equals(selectedCategory) && c.getId() == p.getCategorieId());
                }
                
                return matchesSearch && matchesCategory;
            })
            .collect(Collectors.toList());
            
        displayProducts(filtered);
    }

    private void displayProducts(List<Produit> products) {
        productsGrid.getChildren().clear();
        lbProductCount.setText(products.size() + " ARTIFACTS DISCOVERED");
        
        for (Produit p : products) {
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
                productsGrid.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
    void navigateToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HomeFront.fxml"));
            rootNode.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
