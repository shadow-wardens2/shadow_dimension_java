package Controllers.Marketplace;

import Entities.Marketplace.Produit;
import Services.Marketplace.ServiceProduit;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import java.util.Comparator;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ManagementProduitController implements Initializable {

    @FXML
    private FlowPane productsContainer;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortComboBox;

    private ServiceProduit serviceProduit = new ServiceProduit();
    private ObservableList<Produit> observableProducts = FXCollections.observableArrayList();
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
    }

    private void displayProducts(Iterable<Produit> products) {
        productsContainer.getChildren().clear();
        for (Produit p : products) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/ProductCard.fxml"));
                Parent card = loader.load();
                ProductCardController controller = loader.getController();

                controller.setProduit(p, () -> handleEditProduct(p), () -> handleDeleteProduct(p));
                productsContainer.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleEditProduct(Produit p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/EditProduit.fxml"));
            Parent root = loader.load();
            EditProduitController controller = loader.getController();
            controller.setProduit(p);
            Stage stage = new Stage();
            stage.setTitle("Editer Produit");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadProducts();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteProduct(Produit p) {
        try {
            serviceProduit.delete(p);
            loadProducts();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText("Produit supprimé avec succès.");
            alert.showAndWait();
        } catch (SQLException e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible de supprimer");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
    }

    private FilteredList<Produit> filteredData;
    private SortedList<Produit> sortedData;

    private void setupData() {
        try {
            observableProducts.setAll(serviceProduit.getAll());
            
            filteredData = new FilteredList<>(observableProducts, p -> true);
            sortedData = new SortedList<>(filteredData);
            
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applySortAndFilter());
            
            displayProducts(sortedData);
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
        displayProducts(sortedData);
    }

    private void loadProducts() {
        try {
            observableProducts.setAll(serviceProduit.getAll());
            displayProducts(sortedData);
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
            stage.showAndWait(); // Wait for it to close
            // refresh data after closing
            loadProducts();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
