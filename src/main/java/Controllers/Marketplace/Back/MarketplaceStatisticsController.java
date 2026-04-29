package Controllers.Marketplace.Back;

import Entities.Marketplace.Categorie;
import Entities.Marketplace.Produit;
import Entities.Marketplace.Commande;
import Services.Marketplace.ServiceCategorie;
import Services.Marketplace.ServiceProduit;
import Services.Marketplace.ServiceCommande;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MarketplaceStatisticsController {

    @FXML private Label lbTotalProducts;
    @FXML private Label lbTotalOrders;
    @FXML private Label lbTotalRevenue;
    @FXML private PieChart categoryPieChart;
    @FXML private BarChart<String, Number> stockBarChart;

    private PageHost dashboardContext;

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @FXML
    public void initialize() {
        loadStatistics();
    }

    private void loadStatistics() {
        try {
            ServiceProduit sp = new ServiceProduit();
            ServiceCommande sc = new ServiceCommande();
            ServiceCategorie scat = new ServiceCategorie();

            List<Produit> products = sp.getAll();
            List<Commande> orders = sc.getAll();
            List<Categorie> categories = scat.getAll();

            // Total Stats
            lbTotalProducts.setText(String.valueOf(products.size()));
            lbTotalOrders.setText(String.valueOf(orders.size()));
            double totalRevenue = orders.stream().mapToDouble(Commande::getTotalAmount).sum();
            lbTotalRevenue.setText(String.format("%.2f TND", totalRevenue));

            // Pie Chart: Products per Category
            Map<Integer, Long> prodCountByCat = products.stream()
                    .collect(Collectors.groupingBy(Produit::getCategorieId, Collectors.counting()));

            categoryPieChart.getData().clear();
            for (Categorie cat : categories) {
                long count = prodCountByCat.getOrDefault(cat.getId(), 0L);
                if (count > 0) {
                    categoryPieChart.getData().add(new PieChart.Data(cat.getNom(), count));
                }
            }

            // Bar Chart: Low Stock Products
            stockBarChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Stock Level");
            
            List<Produit> lowStock = products.stream()
                    .filter(p -> p.getStock() < 10)
                    .limit(8)
                    .collect(Collectors.toList());

            for (Produit p : lowStock) {
                series.getData().add(new XYChart.Data<>(p.getNom(), p.getStock()));
            }
            stockBarChart.getData().add(series);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goBack(ActionEvent event) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Marketplace/Back/MarketplaceSelector.fxml");
        }
    }
}
