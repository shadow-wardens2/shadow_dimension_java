package Controllers.Marketplace;

import Entities.Marketplace.Produit;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;

public class ProductCardController {

    @FXML
    private Button btnDelete;

    @FXML
    private Button btnEdit;

    @FXML
    private Label productCategory;

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
    private Runnable onEdit;
    private Runnable onDelete;

    public void setProduit(Produit p, Runnable onEdit, Runnable onDelete) {
        this.produit = p;
        this.onEdit = onEdit;
        this.onDelete = onDelete;

        productName.setText(p.getNom());
        productDescription.setText(p.getDescription());
        productPrice.setText(String.format("%.2f DT", p.getPrix()));
        productStock.setText(String.valueOf(p.getStock()));
        productCategory.setText("ID: " + p.getCategorieId()); // Could fetch category name if needed

        if (p.getImage() != null && !p.getImage().isEmpty()) {
            try {
                File file = new File(p.getImage());
                if (file.exists()) {
                    productImage.setImage(new Image(file.toURI().toString()));
                } else {
                    // Placeholder if file not found
                    // productImage.setImage(new Image(getClass().getResourceAsStream("/images/placeholder.png")));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        btnEdit.setOnAction(e -> onEdit.run());
        btnDelete.setOnAction(e -> onDelete.run());
    }
}
