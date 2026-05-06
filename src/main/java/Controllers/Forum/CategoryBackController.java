package Controllers.Forum;

import Entities.Forum.ForumCategory;
import Services.Forum.CategoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.sql.SQLException;

public class CategoryBackController {

    @FXML private TableView<ForumCategory> categoryTable;
    @FXML private TableColumn<ForumCategory, Integer> colId;
    @FXML private TableColumn<ForumCategory, String> colName;
    @FXML private TableColumn<ForumCategory, String> colSlug;
    @FXML private TableColumn<ForumCategory, String> colDescription;
    @FXML private TableColumn<ForumCategory, String> colColor;

    @FXML private TextField tfName;
    @FXML private TextField tfSlug;
    @FXML private TextArea taDescription;
    @FXML private ColorPicker colorPicker;
    @FXML private Label lblError;

    private final CategoryService categoryService = new CategoryService();
    private final ObservableList<ForumCategory> categoryList = FXCollections.observableArrayList();
    private ForumCategory selectedCategory = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSlug.setCellValueFactory(new PropertyValueFactory<>("slug"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        // Custom cell factory to show color circles
        colColor.setCellValueFactory(new PropertyValueFactory<>("color"));
        colColor.setCellFactory(column -> new TableCell<ForumCategory, String>() {
            @Override
            protected void updateItem(String colorHex, boolean empty) {
                super.updateItem(colorHex, empty);
                if (empty || colorHex == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    try {
                        Circle circle = new Circle(8, Color.web(colorHex));
                        setGraphic(circle);
                        setText(colorHex);
                    } catch (Exception e) {
                        setText(colorHex);
                    }
                }
            }
        });

        categoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedCategory = newSel;
                tfName.setText(newSel.getName());
                tfSlug.setText(newSel.getSlug());
                taDescription.setText(newSel.getDescription());
                try {
                    colorPicker.setValue(Color.web(newSel.getColor()));
                } catch (Exception e) {
                    colorPicker.setValue(Color.WHITE);
                }
            }
        });

        loadCategories();
    }

    private void loadCategories() {
        try {
            categoryList.setAll(categoryService.getAll());
            categoryTable.setItems(categoryList);
        } catch (SQLException e) {
            showError("Failed to load categories: " + e.getMessage());
        }
    }

    @FXML
    void handleAdd() {
        if (!validateInput()) return;
        ForumCategory fc = new ForumCategory();
        fc.setName(tfName.getText().trim());
        fc.setSlug(tfSlug.getText().trim());
        fc.setDescription(taDescription.getText().trim());
        fc.setColor(toHexString(colorPicker.getValue()));

        try {
            categoryService.add(fc);
            loadCategories();
            handleClear();
        } catch (SQLException e) {
            showError("Failed to add category: " + e.getMessage());
        }
    }

    @FXML
    void handleUpdate() {
        if (selectedCategory == null) {
            showError("Select a category to update.");
            return;
        }
        if (!validateInput()) return;

        selectedCategory.setName(tfName.getText().trim());
        selectedCategory.setSlug(tfSlug.getText().trim());
        selectedCategory.setDescription(taDescription.getText().trim());
        selectedCategory.setColor(toHexString(colorPicker.getValue()));

        try {
            categoryService.update(selectedCategory);
            loadCategories();
            handleClear();
        } catch (SQLException e) {
            showError("Failed to update category: " + e.getMessage());
        }
    }

    @FXML
    void handleDelete() {
        if (selectedCategory == null) {
            showError("Select a category to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Category");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete category '" + selectedCategory.getName() + "'?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    categoryService.delete(selectedCategory);
                    loadCategories();
                    handleClear();
                } catch (SQLException e) {
                    showError("Failed to delete: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    void handleClear() {
        selectedCategory = null;
        categoryTable.getSelectionModel().clearSelection();
        tfName.clear();
        tfSlug.clear();
        taDescription.clear();
        colorPicker.setValue(Color.WHITE);
        if (lblError != null) {
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }

    private boolean validateInput() {
        if (tfName.getText() == null || tfName.getText().trim().isEmpty()) {
            showError("Name is required.");
            return false;
        }
        if (tfSlug.getText() == null || tfSlug.getText().trim().isEmpty()) {
            showError("Slug is required.");
            return false;
        }
        return true;
    }

    private void showError(String msg) {
        if (lblError != null) {
            lblError.setText(msg);
            lblError.setVisible(true);
            lblError.setManaged(true);
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText(msg);
            alert.showAndWait();
        }
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }
}
