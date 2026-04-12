package Controllers.event;

import Entities.event.Category;
import Services.event.CategoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class CategoryController implements Initializable {

    @FXML
    private TableView<Category> categoryTable;
    @FXML
    private TableColumn<Category, Integer> colId;
    @FXML
    private TableColumn<Category, String> colNom;
    @FXML
    private TableColumn<Category, String> colDescription;
    @FXML
    private TableColumn<Category, String> colTarification;
    @FXML
    private TableColumn<Category, Double> colPrix;
    @FXML
    private TableColumn<Category, Integer> colActions;

    private final CategoryService categoryService = new CategoryService();
    private final ObservableList<Category> observableCategories = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colTarification.setCellValueFactory(new PropertyValueFactory<>("typeTarification"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));

        loadCategories();

        colActions.setCellFactory(param -> new TableCell<Category, Integer>() {
            private final Button btnUpdate = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(10, btnUpdate, btnDelete);

            {
                btnUpdate.getStyleClass().add("edit-button");
                btnDelete.getStyleClass().add("delete-button");

                btnDelete.setOnAction(event -> {
                    Category c = getTableView().getItems().get(getIndex());
                    try {
                        categoryService.delete(c);
                        loadCategories();
                        showAlert(Alert.AlertType.INFORMATION, "Succes", "Categorie supprimee avec succes.");
                    } catch (SQLException ex) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                    }
                });

                btnUpdate.setOnAction(event -> {
                    Category c = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/event/EditCategory.fxml"));
                        Parent root = loader.load();
                        EditCategoryController controller = loader.getController();
                        controller.setCategory(c);
                        Stage stage = new Stage();
                        stage.setTitle("Edit Category");
                        stage.setScene(new Scene(root));
                        stage.showAndWait();
                        loadCategories();
                    } catch (IOException ex) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                    }
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }

    @FXML
    void handleAddCategory(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/event/AddCategory.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add New Category");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadCategories();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void loadCategories() {
        observableCategories.clear();
        try {
            observableCategories.addAll(categoryService.getAll());
            categoryTable.setItems(observableCategories);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
