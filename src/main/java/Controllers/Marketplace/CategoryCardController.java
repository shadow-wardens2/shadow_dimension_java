package Controllers.Marketplace;

import Entities.Marketplace.Categorie;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class CategoryCardController {

    @FXML
    private Label categoryDescription;

    @FXML
    private Label categoryId;

    @FXML
    private Label categoryName;

    @FXML
    private Button btnDelete;

    @FXML
    private Button btnEdit;

    public void setCategory(Categorie c, Runnable onEdit, Runnable onDelete) {
        categoryName.setText(c.getNom());
        categoryDescription.setText(c.getDescription());
        categoryId.setText("ID: " + c.getId());

        btnEdit.setOnAction(e -> onEdit.run());
        btnDelete.setOnAction(e -> onDelete.run());
    }
}
