package Controllers.Marketplace;

import Entities.Marketplace.Type;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.util.function.Consumer;

public class TypeCardController {

    @FXML
    private Label typeId;

    @FXML
    private Label typeName;

    private Type type;
    private Consumer<Type> onEdit;
    private Consumer<Type> onDelete;

    public void setTypeData(Type type, Consumer<Type> onEdit, Consumer<Type> onDelete) {
        this.type = type;
        this.onEdit = onEdit;
        this.onDelete = onDelete;

        typeName.setText(type.getNom());
        typeId.setText("#" + type.getId());
    }

    @FXML
    void handleDelete(ActionEvent event) {
        if (onDelete != null) {
            onDelete.accept(type);
        }
    }

    @FXML
    void handleEdit(ActionEvent event) {
        if (onEdit != null) {
            onEdit.accept(type);
        }
    }
}
