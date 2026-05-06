package Controllers.Artworks;

import Controllers.Marketplace.Back.PageHost;
import Entities.Artworks.Artworks;
import Entities.Artworks.Categories;
import Services.Artworks.ServiceArtworks;
import Services.Artworks.ServiceCategories;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
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
    @FXML private BarChart<String, Number> barChartValue;
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
            lbTotalValue.setText(totalValue + " USD");

            // 2. Data Preparation
            Map<Integer, Long> countMap = artworks.stream()
                    .collect(Collectors.groupingBy(Artworks::getCategoryID, Collectors.counting()));
            
            Map<Integer, Long> valueMap = artworks.stream()
                    .collect(Collectors.groupingBy(Artworks::getCategoryID, Collectors.summingLong(Artworks::getPrice)));

            pieChartCategories.getData().clear();
            barChartValue.getData().clear();
            categoryStatsPane.getChildren().clear();
            XYChart.Series<String, Number> valueSeries = new XYChart.Series<>();
            valueSeries.setName("Ethereal Value");
            barChartValue.getData().add(valueSeries);
            
            Categories emptiestCategory = null;
            Categories mostValuableCategory = null;
            long minCount = Long.MAX_VALUE;
            long maxValue = -1;

            for (Categories cat : categories) {
                int catId = 0;
                try {
                    catId = Integer.parseInt(cat.getID());
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Category ID is not a number: " + cat.getID());
                }
                long count = countMap.getOrDefault(catId, 0L);
                long value = valueMap.getOrDefault(catId, 0L);
                
                // Add to Pie Chart
                if (count > 0) {
                    pieChartCategories.getData().add(new PieChart.Data(cat.getTitle(), count));
                }
                
                // Add to Bar Chart
                valueSeries.getData().add(new XYChart.Data<>(cat.getTitle(), (double) value));
                System.out.println("DEBUG Stats: Category=" + cat.getTitle() + " | Count=" + count + " | Value=" + value);
                
                // Add to detailed cards
                VBox card = createCategoryCard(cat.getTitle(), count, value);
                categoryStatsPane.getChildren().add(card);
                
                // Tracking for AI advice
                if (count < minCount) { minCount = count; emptiestCategory = cat; }
                if (value > maxValue) { maxValue = value; mostValuableCategory = cat; }
            }

            System.out.println("DEBUG Stats: Total Artworks=" + artworks.size() + " | Total Value=" + totalValue);

            // 3. AI Advisor Logic
            generateAiAdvice(emptiestCategory, minCount, mostValuableCategory, maxValue);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox createCategoryCard(String titleStr, long count, long value) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-padding: 20; -fx-background-radius: 18; -fx-pref-width: 220; -fx-border-color: rgba(139, 92, 246, 0.2); -fx-border-radius: 18; -fx-border-width: 1;");
        
        Label title = new Label(titleStr.toUpperCase());
        title.setStyle("-fx-text-fill: #8b5cf6; -fx-font-size: 12px; -fx-font-weight: bold; -fx-letter-spacing: 1;");
        
        Label countLabel = new Label(count + " Relics");
        countLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label valueLabel = new Label("$ " + value);
        valueLabel.setStyle("-fx-text-fill: #adaaae; -fx-font-size: 13px;");
        
        card.getChildren().addAll(title, countLabel, valueLabel);
        
        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace("rgba(255,255,255,0.03)", "rgba(255,255,255,0.06)") + "-fx-border-color: #8b5cf6;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("rgba(255,255,255,0.06)", "rgba(255,255,255,0.03)").replace("-fx-border-color: #8b5cf6;", "-fx-border-color: rgba(139, 92, 246, 0.2);")));
        
        return card;
    }

    private void generateAiAdvice(Categories emptiest, long minCount, Categories mostValuable, long maxValue) {
        StringBuilder advice = new StringBuilder("👻 Shadow Ghost Insight: ");
        
        if (emptiest != null) {
            advice.append("The spectral void in '").append(emptiest.getTitle())
                  .append("' is too vast—only ").append(minCount).append(" relics are manifested there. You should fill this emptiness to stabilize the realm. ");
        }
        
        if (mostValuable != null) {
            if (mostValuable.equals(emptiest)) {
                advice.append("Surprisingly, this same realm is our most valuable source, yielding $").append(maxValue).append("! Imagine its power if fully populated. ");
            } else {
                advice.append("Behold! The '").append(mostValuable.getTitle())
                      .append("' realm holds the strongest ethereal resonance, with a total value of $").append(maxValue).append(". ");
            }
        }
        
        advice.append("The dimensional energies are flowing steadily through the ghost-lines... continue your curation.");
        
        lbAiSuggestion.setText(advice.toString());
    }
}
