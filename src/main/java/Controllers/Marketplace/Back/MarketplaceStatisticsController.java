package Controllers.Marketplace;

import Entities.Marketplace.Categorie;
import Entities.Marketplace.Produit;
import Entities.Marketplace.Type;
import Services.Marketplace.ServiceCategorie;
import Services.Marketplace.ServiceProduit;
import Services.Marketplace.ServiceType;
import Controllers.Marketplace.PageHost;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MarketplaceStatisticsController {

    @FXML
    private Label lbTotalProducts;

    @FXML
    private Label lbTotalCategories;

    @FXML
    private Label lbTotalStock;

    @FXML
    private Label lbAveragePrice;

    @FXML
    private PieChart pieCategories;

    @FXML
    private PieChart pieStockByType;

    private PageHost dashboardContext;

    private final ServiceProduit serviceProduit = new ServiceProduit();
    private final ServiceCategorie serviceCategorie = new ServiceCategorie();
    private final ServiceType serviceType = new ServiceType();

    @FXML
    public void initialize() {
        loadStatistics();
    }

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @FXML
    private void handleRefreshStats() {
        loadStatistics();
    }

    private void loadStatistics() {
        try {
            List<Produit> produits = serviceProduit.getAll();
            List<Categorie> categories = serviceCategorie.getAll();
            List<Type> types = serviceType.getAll();

            // Total Stats
            int totalProducts = produits.size();
            int totalCategories = categories.size();
            int totalStock = produits.stream().mapToInt(Produit::getStock).sum();
            double avgPrice = produits.isEmpty() ? 0 : produits.stream().mapToDouble(Produit::getPrix).average().orElse(0.0);

            lbTotalProducts.setText(String.valueOf(totalProducts));
            lbTotalCategories.setText(String.valueOf(totalCategories));
            lbTotalStock.setText(String.valueOf(totalStock));
            lbAveragePrice.setText(String.format("%.2f", avgPrice));

            // Pie Chart: Products by Category
            Map<Integer, String> categoryNames = categories.stream()
                    .collect(Collectors.toMap(Categorie::getId, Categorie::getNom));
            
            Map<String, Long> productCountByCategory = produits.stream()
                    .collect(Collectors.groupingBy(p -> categoryNames.getOrDefault(p.getCategorieId(), "Unknown"), Collectors.counting()));

            pieCategories.setData(FXCollections.observableArrayList(
                    productCountByCategory.entrySet().stream()
                            .map(entry -> new PieChart.Data(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toList())
            ));

            // Pie Chart: Stock by Type
            Map<Integer, String> typeNames = types.stream()
                    .collect(Collectors.toMap(Type::getId, Type::getNom));

            Map<String, Integer> stockByType = produits.stream()
                    .collect(Collectors.groupingBy(p -> typeNames.getOrDefault(p.getTypeId(), "Unknown"), 
                            Collectors.summingInt(Produit::getStock)));

            pieStockByType.setData(FXCollections.observableArrayList(
                    stockByType.entrySet().stream()
                            .map(entry -> new PieChart.Data(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toList())
            ));

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load statistics: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }
}
