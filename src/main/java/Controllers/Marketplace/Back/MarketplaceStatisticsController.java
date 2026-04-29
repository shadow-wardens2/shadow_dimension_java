package Controllers.Marketplace.Back;

import Entities.Marketplace.Categorie;
import Entities.Marketplace.Produit;
import Entities.Marketplace.Commande;
import Entities.Marketplace.Type;
import Services.Marketplace.ServiceCategorie;
import Services.Marketplace.ServiceType;
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
    @FXML private PieChart typePieChart;

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

            // Pie Chart: Products per Type
            ServiceType stype = new ServiceType();
            List<Type> types = stype.getAll();
            Map<Integer, Long> prodCountByType = products.stream()
                    .collect(Collectors.groupingBy(Produit::getTypeId, Collectors.counting()));

            typePieChart.getData().clear();
            for (Type t : types) {
                long count = prodCountByType.getOrDefault(t.getId(), 0L);
                if (count > 0) {
                    typePieChart.getData().add(new PieChart.Data(t.getNom(), count));
                }
            }

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
