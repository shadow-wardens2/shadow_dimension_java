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
                if (produit.getImage().startsWith("http")) {
                    productImage.setImage(new Image(produit.getImage(), true));
                } else {
                    File file = new File(produit.getImage());
                    if (file.exists()) {
                        productImage.setImage(new Image(file.toURI().toString()));
                    } else {
                        setPlaceholderImage();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading image: " + e.getMessage());
                setPlaceholderImage();
            }
        } else {
            setPlaceholderImage();
        }
    }

    private void setPlaceholderImage() {
        // Using a professional product placeholder
        productImage.setImage(new Image("https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?w=400&auto=format&fit=crop&q=60"));
        productImage.setOpacity(0.6);
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


