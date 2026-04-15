package Controllers.Marketplace;

import Entities.Marketplace.Produit;
import Services.Marketplace.ServiceProduit;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.ResourceBundle;

public class ManagementProduitController implements Initializable {

    @FXML
    private TilePane productsContainer;

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

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySortAndFilter());
    }

    private void setupData() {
        try {
            observableProducts.setAll(serviceProduit.getAll());
            filteredData = new FilteredList<>(observableProducts, p -> true);
            sortedData = new SortedList<>(filteredData);
            displayProducts();
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
        if (sortOption != null) {
            Comparator<Produit> comparator = switch (sortOption) {
                case "Name (A-Z)" -> Comparator.comparing(Produit::getNom, String.CASE_INSENSITIVE_ORDER);
                case "Price: Low to High" -> Comparator.comparingDouble(Produit::getPrix);
                case "Price: High to Low" -> Comparator.comparingDouble(Produit::getPrix).reversed();
                case "Stock: High to Low" -> Comparator.comparingInt(Produit::getStock).reversed();
                default -> Comparator.comparingInt(Produit::getId);
            };
            sortedData.setComparator(comparator);
        }
        displayProducts();
    }

    private void displayProducts() {
        productsContainer.getChildren().clear();
        for (Produit p : sortedData) {
            productsContainer.getChildren().add(createProductCard(p));
        }
    }

    private VBox createProductCard(Produit p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card");
        card.setAlignment(Pos.TOP_LEFT);

        Label nameLabel = new Label(p.getNom());
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setWrapText(true);

        Label descLabel = new Label(p.getDescription());
        descLabel.getStyleClass().add("product-description");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(60);
        VBox.setVgrow(descLabel, Priority.ALWAYS);

        Label priceLabel = new Label(String.format("%.2f DT", p.getPrix()));
        priceLabel.getStyleClass().add("product-price");

        Label stockLabel = new Label("Stock: " + p.getStock());
        stockLabel.getStyleClass().add("product-stock");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("edit-button");
        btnEdit.setOnAction(e -> handleEditAction(p));

        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("delete-button");
        btnDelete.setOnAction(e -> handleDeleteAction(p));

        actions.getChildren().addAll(btnEdit, btnDelete);

        card.getChildren().addAll(nameLabel, descLabel, priceLabel, stockLabel, actions);
        
        // Make whole card clickable for edit maybe?
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                handleEditAction(p);
            }
        });

        return card;
    }

    private void handleEditAction(Produit p) {
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

    private void handleDeleteAction(Produit p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Product");
        alert.setHeaderText("Delete " + p.getNom() + "?");
        alert.setContentText("Are you sure you want to delete this product? This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    serviceProduit.delete(p);
                    loadProducts();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error", "Could not delete product: " + e.getMessage());
                }
            }
        });
    }

    private void loadProducts() {
        try {
            observableProducts.setAll(serviceProduit.getAll());
            displayProducts();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
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
