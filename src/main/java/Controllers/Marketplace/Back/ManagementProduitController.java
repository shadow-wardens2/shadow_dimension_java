package Controllers.Marketplace;

import Entities.Marketplace.Produit;
import Services.Marketplace.ServiceProduit;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import java.util.Comparator;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ManagementProduitController implements Initializable {

    @FXML
    private TilePane productsTilePane;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortComboBox;

    private ServiceProduit serviceProduit = new ServiceProduit();
    private ObservableList<Produit> observableProducts = FXCollections.observableArrayList();
    private FilteredList<Produit> filteredData;
    private SortedList<Produit> sortedData;
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

        // Populate sort options
        sortComboBox.getItems().addAll("Default (ID)", "Name (A-Z)", "Price: Low to High", "Price: High to Low", "Stock: High to Low");
        sortComboBox.setValue("Default (ID)");
        sortComboBox.setOnAction(e -> applySortAndFilter());

        // Listen for changes in the sorted/filtered list to update the UI
        sortedData.addListener((ListChangeListener<Produit>) c -> refreshGrid());

        // Initial display
        refreshGrid();
    }

    private void setupData() {
        try {
            observableProducts.setAll(serviceProduit.getAll());
            filteredData = new FilteredList<>(observableProducts, p -> true);
            sortedData = new SortedList<>(filteredData);
            
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applySortAndFilter());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void applySortAndFilter() {
        String filter = searchField.getText();
        filteredData.setPredicate(produit -> {
            if (filter == null || filter.isEmpty()) return true;
            String lower = filter.toLowerCase();
            return produit.getNom().toLowerCase().contains(lower) || 
                   (produit.getDescription() != null && produit.getDescription().toLowerCase().contains(lower)) ||
                   String.valueOf(produit.getId()).contains(lower);
        });

        String sortOption = sortComboBox.getValue();
        if (sortOption == null) return;

        Comparator<Produit> comparator = switch (sortOption) {
            case "Name (A-Z)" -> Comparator.comparing(Produit::getNom, String.CASE_INSENSITIVE_ORDER);
            case "Price: Low to High" -> Comparator.comparingDouble(Produit::getPrix);
            case "Price: High to Low" -> Comparator.comparingDouble(Produit::getPrix).reversed();
            case "Stock: High to Low" -> Comparator.comparingInt(Produit::getStock).reversed();
            default -> Comparator.comparingInt(Produit::getId);
        };

        sortedData.setComparator(comparator);
        refreshGrid(); // Explicitly refresh after sorting
    }

    private void refreshGrid() {
        productsTilePane.getChildren().clear();
        for (Produit product : sortedData) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/ProductCard.fxml"));
                Parent card = loader.load();
                ProductCardController controller = loader.getController();
                controller.setProductData(product, this::editProduct, this::deleteProduct);
                productsTilePane.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void editProduct(Produit p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/EditProduit.fxml"));
            Parent root = loader.load();
            EditProduitController controller = loader.getController();
            controller.setProduit(p);
            Stage stage = new Stage();
            stage.setTitle("Edit Product");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadProducts();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteProduct(Produit p) {
        try {
            serviceProduit.delete(p);
            loadProducts();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Product deleted successfully.");
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

    private void loadProducts() {
        try {
            observableProducts.setAll(serviceProduit.getAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleAddProduct(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/AjouterProduit.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add New Product");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadProducts();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
