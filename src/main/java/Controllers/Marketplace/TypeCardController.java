package Controllers.Marketplace;

import Entities.Marketplace.Type;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class TypeCardController {

    @FXML
    private Label typeId;

    @FXML
    private Label typeName;

    @FXML
    private Button btnDelete;

    @FXML
    private Button btnEdit;

    public void setType(Type t, Runnable onEdit, Runnable onDelete) {
        typeName.setText(t.getNom());
        typeId.setText("ID: " + t.getId());

        btnEdit.setOnAction(e -> onEdit.run());
        btnDelete.setOnAction(e -> onDelete.run());
    }
}
