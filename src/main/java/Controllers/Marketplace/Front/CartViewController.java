package Controllers.Marketplace.Front;

import Entities.Marketplace.Produit;
import Entities.Marketplace.Commande;
import Services.Marketplace.ServiceCommande;
import Utils.CartManager;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.Alert;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;

import java.io.IOException;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.util.List;

public class CartViewController {

    @FXML private VBox cartItemsContainer;
    @FXML private Label lbTotal;
    @FXML private AnchorPane rootNode;

    @FXML
    public void initialize() {
        refreshCart();
    }

    private void refreshCart() {
        cartItemsContainer.getChildren().clear();
        List<Produit> items = CartManager.getCartItems();
        for (Produit p : items) {
            HBox row = new HBox(15);
            row.setStyle("-fx-padding: 15; -fx-background-color: #24202b; -fx-background-radius: 12;");
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            Label name = new Label(p.getNom());
            name.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            Label price = new Label(String.format("%.2f TND", p.getPrix()));
            price.setStyle("-fx-text-fill: #d3bbff; -fx-font-size: 16px;");
            
            Button removeBtn = new Button("Remove");
            removeBtn.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #ef4444; -fx-background-radius: 6; -fx-cursor: hand;");
            removeBtn.setOnAction(e -> {
                CartManager.removeProduct(p);
                refreshCart();
            });
            
            row.getChildren().addAll(name, spacer, price, removeBtn);
            cartItemsContainer.getChildren().add(row);
        }
        lbTotal.setText(String.format("Total: %.2f TND", CartManager.getTotal()));
    }

    @FXML
    void handleCheckout() {
        if (CartManager.getCartItems().isEmpty()) {
            showAlert("Cart Empty", "Your cart is empty!");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/Front/CheckoutForm.fxml"));
            rootNode.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/Front/MarketplaceFront.fxml"));
            rootNode.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
