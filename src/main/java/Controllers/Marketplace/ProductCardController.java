package Controllers.Marketplace;

import Entities.Marketplace.Produit;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.util.function.Consumer;

public class ProductCardController {

    @FXML
    private Label productDescription;

    @FXML
    private ImageView productImage;

    @FXML
    private Label productName;

    @FXML
    private Label productPrice;

    @FXML
    private Label productStock;

    private Produit produit;
    private Consumer<Produit> onEdit;
    private Consumer<Produit> onDelete;

    public void setProductData(Produit produit, Consumer<Produit> onEdit, Consumer<Produit> onDelete) {
        this.produit = produit;
        this.onEdit = onEdit;
        this.onDelete = onDelete;

        productName.setText(produit.getNom());
        productDescription.setText(produit.getDescription());
        productPrice.setText(String.format("$%.2f", produit.getPrix()));
        productStock.setText(String.valueOf(produit.getStock()));

        // Handle image if available
        if (produit.getImage() != null && !produit.getImage().isEmpty()) {
            try {
                File file = new File(produit.getImage());
                if (file.exists()) {
                    productImage.setImage(new Image(file.toURI().toString()));
                } else {
                    // Fallback to default icon or placeholder
                    // productImage.setImage(new Image(getClass().getResourceAsStream("/icons/placeholder.png")));
                }
            } catch (Exception e) {
                System.err.println("Error loading image: " + e.getMessage());
            }
        }
    }

    @FXML
    void handleDelete(ActionEvent event) {
        if (onDelete != null) {
            onDelete.accept(produit);
        }
    }

    @FXML
    void handleEdit(ActionEvent event) {
        if (onEdit != null) {
            onEdit.accept(produit);
        }
    }
}
