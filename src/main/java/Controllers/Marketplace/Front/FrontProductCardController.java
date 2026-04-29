package Controllers.Marketplace.Front;

import Entities.Marketplace.Produit;
import Services.Marketplace.CurrencyConverterService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import java.io.File;

public class FrontProductCardController {

    @FXML private ImageView ivProduct;
    @FXML private Label lbName;
    @FXML private Label lbPrice;
    @FXML private Label lbStock;
    @FXML private Label lbCategory;
    @FXML private Label lbDescription;
    @FXML private Rectangle imagePlaceholder;
    @FXML private javafx.scene.control.Button btnAddToCart;

    private Produit currentProduct;

    public void setData(Produit p, String categoryName) {
        this.currentProduct = p;
        if (lbName != null) lbName.setText(p.getNom());
        
        // Use CurrencyConverterService for price display
        double price = p.getPrix();
        String currency = CurrencyConverterService.getCurrentCurrency();
        double convertedPrice = CurrencyConverterService.convert(price, currency);
        String symbol = CurrencyConverterService.getCurrencySymbol(currency);
        
        if (lbPrice != null) lbPrice.setText(String.format("%.2f %s", convertedPrice, symbol));
        
        if (lbStock != null) {
            if (p.getStock() <= 0) {
                lbStock.setText("OUT OF STOCK");
                lbStock.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                if (btnAddToCart != null) {
                    btnAddToCart.setDisable(true);
                    btnAddToCart.setOpacity(0.5);
                    btnAddToCart.setText("Voided");
                }
            } else {
                lbStock.setText(String.valueOf(p.getStock()));
                lbStock.setStyle("-fx-text-fill: -fx-primary;");
                if (btnAddToCart != null) {
                    btnAddToCart.setDisable(false);
                    btnAddToCart.setOpacity(1.0);
                    btnAddToCart.setText("Cart +");
                }
            }
        }
        
        if (lbCategory != null) lbCategory.setText(categoryName.toUpperCase());
        if (lbDescription != null) lbDescription.setText(p.getDescription());

        if (p.getImage() != null && !p.getImage().isEmpty()) {
            try {
                Image image;
                if (p.getImage().startsWith("http")) {
                    image = new Image(p.getImage(), true);
                } else {
                    File file = new File(p.getImage());
                    image = new Image(file.toURI().toString());
                }
                if (ivProduct != null) ivProduct.setImage(image);
                if (imagePlaceholder != null) imagePlaceholder.setVisible(false);
            } catch (Exception e) {
                if (imagePlaceholder != null) imagePlaceholder.setVisible(true);
            }
        } else {
            if (imagePlaceholder != null) imagePlaceholder.setVisible(true);
        }
    }

    @FXML
    void handleAddToCart(javafx.event.ActionEvent event) {
        if (currentProduct != null) {
            Utils.CartManager.addProduct(currentProduct);
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Cart");
            alert.setHeaderText(null);
            alert.setContentText(currentProduct.getNom() + " added to cart!");
            alert.showAndWait();
        }
    }
}
