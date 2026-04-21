package Controllers.Marketplace;

import Entities.Marketplace.Categorie;
import Services.Marketplace.ServiceCategorie;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.collections.transformation.FilteredList;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ManagementCategorieController implements Initializable {

    @FXML
    private TilePane categoryTilePane;

    @FXML
    private TextField searchField;

    private ServiceCategorie serviceCategorie = new ServiceCategorie();
    private ObservableList<Categorie> observableCategories = FXCollections.observableArrayList();
    private FilteredList<Categorie> filteredData;
    private PageHost dashboardContext;

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @FXML
    void goBack(ActionEvent event) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Marketplace/MarketplaceSelector.fxml");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupData();
        
        // Listen for changes in the filtered list to update the UI
        filteredData.addListener((ListChangeListener<Categorie>) c -> refreshGrid());

        // Initial display
        refreshGrid();
    }

    private void setupData() {
        try {
            observableCategories.setAll(serviceCategorie.getAll());
            filteredData = new FilteredList<>(observableCategories, c -> true);
            
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredData.setPredicate(categorie -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    String lower = newVal.toLowerCase();
                    return categorie.getNom().toLowerCase().contains(lower) || 
                           (categorie.getDescription() != null && categorie.getDescription().toLowerCase().contains(lower)) ||
                           String.valueOf(categorie.getId()).contains(lower);
                });
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshGrid() {
        categoryTilePane.getChildren().clear();
        for (Categorie category : filteredData) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/CategoryCard.fxml"));
                Parent card = loader.load();
                CategoryCardController controller = loader.getController();
                controller.setCategoryData(category, this::editCategory, this::deleteCategory);
                categoryTilePane.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void editCategory(Categorie c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/EditCategorie.fxml"));
            Parent root = loader.load();
            EditCategorieController controller = loader.getController();
            controller.setCategorie(c);
            Stage stage = new Stage();
            stage.setTitle("Edit Category");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadCategories();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteCategory(Categorie c) {
        try {
            serviceCategorie.delete(c);
            loadCategories();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Category deleted successfully.");
            alert.showAndWait();
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not delete");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void loadCategories() {
        try {
            observableCategories.setAll(serviceCategorie.getAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleAddCategorie(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/AjouterCategorie.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add New Category");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadCategories();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
