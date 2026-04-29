package Controllers.Artworks;

import Entities.Artworks.Categories;
import Services.Artworks.ServiceCategories;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import Controllers.Marketplace.PageHost;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class ListerCategoriesController implements Initializable {

    @FXML private TableView<Categories> categoriesTable;
    @FXML private TableColumn<Categories, String> colId;
    @FXML private TableColumn<Categories, String> colName;
    @FXML private TableColumn<Categories, String> colDescription;
    @FXML private TableColumn<Categories, Void> colActions;

    private ServiceCategories serviceCategories = new ServiceCategories();
    private ObservableList<Categories> categoriesList = FXCollections.observableArrayList();
    private PageHost dashboardContext;

    public void setDashboardContext(PageHost ctx) {
        this.dashboardContext = ctx;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        loadData();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("ID"));
        colName.setCellValueFactory(new PropertyValueFactory<>("title"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(10, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("edit-button");
                btnDelete.getStyleClass().add("delete-button");
                btnEdit.setOnAction(event -> {
                    Categories c = getTableView().getItems().get(getIndex());
                    navigateToEdit(c);
                });
                btnDelete.setOnAction(event -> {
                    Categories c = getTableView().getItems().get(getIndex());
                    deleteCategory(c);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadData() {
        try {
            categoriesList.clear();
            List<Categories> data = serviceCategories.getAll();
            categoriesList.addAll(data);
            categoriesTable.setItems(categoriesList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteCategory(Categories c) {
        try {
            serviceCategories.delete(c);
            loadData();
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Delete Failed");
            alert.setContentText("Cannot delete this category. It may be linked to existing artworks.\nError: " + e.getMessage());
            alert.show();
        }
    }

    @FXML
    private void navigateToAdd(ActionEvent event) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Artworks/AjouterCategory.fxml");
        }
    }

    private void navigateToEdit(Categories c) {
        if (dashboardContext != null) {
            Object controller = dashboardContext.loadPage("/Artworks/AjouterCategory.fxml");
            if (controller instanceof AjouterCategoryController) {
                ((AjouterCategoryController) controller).setCategoryData(c);
            }
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Artworks/ListerArtworks.fxml");
        }
    }
}
