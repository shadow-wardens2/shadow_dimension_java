package Controllers.Marketplace.Back;

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
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import java.util.Comparator;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
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
    private PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @FXML
    void goBack(ActionEvent event) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Marketplace/Back/MarketplaceSelector.fxml");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupData();

        // Populate sort options
        sortComboBox.getItems().addAll("Default (ID)", "Name (A-Z)", "Price: Low to High", "Price: High to Low", "Stock: High to Low");
        sortComboBox.setValue("Default (ID)");
        sortComboBox.setOnAction(e -> applySortAndFilter());

        searchDebounce.setOnFinished(e -> applySortAndFilter());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> searchDebounce.playFromStart());
    }

    private void setupData() {
        Task<List<Produit>> loadTask = new Task<List<Produit>>() {
            @Override
            protected List<Produit> call() throws Exception {
                return serviceProduit.getAll();
            }

            @Override
            protected void succeeded() {
                List<Produit> products = getValue();
                Platform.runLater(() -> {
                    observableProducts.setAll(products);
                    filteredData = new FilteredList<>(observableProducts, p -> true);
                    sortedData = new SortedList<>(filteredData);
                    refreshGrid();
                });
            }

            @Override
            protected void failed() {
                getException().printStackTrace();
            }
        };
        new Thread(loadTask).start();
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

    private Thread currentDisplayThread;

    private void refreshGrid() {
        if (currentDisplayThread != null && currentDisplayThread.isAlive()) {
            currentDisplayThread.interrupt();
        }

        productsTilePane.getChildren().clear();
        
        Task<Void> displayTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                for (Produit product : sortedData) {
                    if (Thread.currentThread().isInterrupted()) break;
                    
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/Back/ProductCard.fxml"));
                    Parent card = loader.load();
                    ProductCardController controller = loader.getController();
                    
                    Platform.runLater(() -> {
                        controller.setProductData(product, ManagementProduitController.this::editProduct, ManagementProduitController.this::deleteProduct);
                        productsTilePane.getChildren().add(card);
                    });
                    
                    Thread.sleep(20);
                }
                return null;
            }
        };

        currentDisplayThread = new Thread(displayTask);
        currentDisplayThread.setDaemon(true);
        currentDisplayThread.start();
    }

    private void editProduct(Produit p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/Back/EditProduit.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/Back/AjouterProduit.fxml"));
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
