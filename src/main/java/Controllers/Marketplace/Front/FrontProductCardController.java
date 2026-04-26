package Controllers.Marketplace.Front;

import Entities.Marketplace.Produit;
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

    public void setData(Produit p, String categoryName) {
        if (lbName != null) lbName.setText(p.getNom());
        if (lbPrice != null) lbPrice.setText(p.getPrix() + " SD");
        if (lbStock != null) lbStock.setText(String.valueOf(p.getStock()));
        if (lbCategory != null) lbCategory.setText(categoryName.toUpperCase());
        if (lbDescription != null) lbDescription.setText(p.getDescription());

        if (p.getImage() != null && !p.getImage().isEmpty()) {
            try {
                Image image;
                if (p.getImage().startsWith("http")) {
                    image = new Image(p.getImage(), true);
                } else {
                    File file = new File(p.getImage());
                    image = new Image(file.toURI().toString(), true);
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
}
