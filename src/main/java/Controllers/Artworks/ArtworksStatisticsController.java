package Controllers.Artworks;

import Controllers.Marketplace.Back.PageHost;
import Entities.Artworks.Artworks;
import Entities.Artworks.Categories;
import Services.Artworks.ServiceArtworks;
import Services.Artworks.ServiceCategories;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArtworksStatisticsController {

    @FXML private Label lbTotalArtworks;
    @FXML private Label lbTotalValue;
    @FXML private Label lbTotalCategories;
    @FXML private Label lbAiSuggestion;
    @FXML private PieChart pieChartCategories;
    @FXML private FlowPane categoryStatsPane;

    private PageHost dashboardContext;
    private ServiceArtworks serviceArtworks = new ServiceArtworks();
    private ServiceCategories serviceCategories = new ServiceCategories();

    public void setDashboardContext(PageHost context) {
        this.dashboardContext = context;
        loadStatistics();
    }

    private void loadStatistics() {
        try {
            List<Artworks> artworks = serviceArtworks.getAll();
            List<Categories> categories = serviceCategories.getAll();

            // 1. Basic Stats
            lbTotalArtworks.setText(String.valueOf(artworks.size()));
            lbTotalCategories.setText(String.valueOf(categories.size()));
            
            long totalValue = artworks.stream().mapToLong(Artworks::getPrice).sum();
            lbTotalValue.setText(totalValue + " ETH");

            // 2. Pie Chart: Artworks per Category
            Map<Integer, Long> countMap = artworks.stream()
                    .collect(Collectors.groupingBy(Artworks::getCategoryID, Collectors.counting()));

            pieChartCategories.getData().clear();
            categoryStatsPane.getChildren().clear();
            Categories emptiestCategory = null;
            long minCount = Long.MAX_VALUE;

            for (Categories cat : categories) {
                Long count = countMap.getOrDefault(Integer.parseInt(cat.getID()), 0L);
                
                // Add to Pie Chart
                if (count > 0) {
                    pieChartCategories.getData().add(new PieChart.Data(cat.getTitle() + " (" + count + ")", count));
                }
                
                // Add to detailed cards
                VBox card = new VBox(5);
                card.setAlignment(javafx.geometry.Pos.CENTER);
                card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-padding: 15; -fx-background-radius: 15; -fx-pref-width: 180; -fx-border-color: rgba(139, 92, 246, 0.2); -fx-border-radius: 15;");
                
                Label title = new Label(cat.getTitle().toUpperCase());
                title.setStyle("-fx-text-fill: #adaaae; -fx-font-size: 11px; -fx-font-weight: bold;");
                
                Label val = new Label(String.valueOf(count));
                val.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 20px; -fx-font-weight: bold;");
                
                card.getChildren().addAll(title, val);
                categoryStatsPane.getChildren().add(card);
                
                if (count < minCount) {
                    minCount = count;
                    emptiestCategory = cat;
                }
            }

            // AI Suggestion
            if (emptiestCategory != null) {
                lbAiSuggestion.setText("The realm of '" + emptiestCategory.getTitle() + "' feels empty (" + minCount + " relics). You should manifest more artworks there to balance the dimension's energies.");
            } else {
                lbAiSuggestion.setText("The dimension is balanced... for now.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
