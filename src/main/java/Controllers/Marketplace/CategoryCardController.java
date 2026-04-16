package Controllers.Marketplace;

import Entities.Marketplace.Categorie;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.util.function.Consumer;

public class CategoryCardController {

    @FXML
    private Label categoryDescription;

    @FXML
    private Label categoryId;

    @FXML
    private Label categoryName;

    private Categorie category;
    private Consumer<Categorie> onEdit;
    private Consumer<Categorie> onDelete;

    public void setCategoryData(Categorie category, Consumer<Categorie> onEdit, Consumer<Categorie> onDelete) {
        this.category = category;
        this.onEdit = onEdit;
        this.onDelete = onDelete;

        categoryName.setText(category.getNom());
        categoryDescription.setText(category.getDescription());
        categoryId.setText("#" + category.getId());
    }

    @FXML
    void handleDelete(ActionEvent event) {
        if (onDelete != null) {
            onDelete.accept(category);
        }
    }

    @FXML
    void handleEdit(ActionEvent event) {
        if (onEdit != null) {
            onEdit.accept(category);
        }
    }
}
