package Controllers.Forum;

import Services.Forum.PostService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class ForumStatisticsController implements Initializable {

    @FXML private PieChart pieChart;
    @FXML private LineChart<String, Number> lineChart;

    private final PostService postService = new PostService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadStatistics();
    }

    private void loadStatistics() {
        // Load in a background thread to prevent UI freezing
        new Thread(() -> {
            try {
                // Fetch data
                Map<String, Integer> categoryStats = postService.getPostsCountByCategory();
                Map<String, Integer> dateStats = postService.getPostsCountByDate();

                Platform.runLater(() -> {
                    // Populate Pie Chart
                    ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
                    for (Map.Entry<String, Integer> entry : categoryStats.entrySet()) {
                        pieChartData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
                    }
                    pieChart.setData(pieChartData);

                    // Populate Line Chart
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Scrolls Inscribed");
                    // Data might be unsorted or reversed from the query depending on DB logic, but since we limited to 7 DESC, we should reverse to chronological
                    dateStats.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey()) // Sort chronologically
                            .forEach(entry -> {
                                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                            });

                    lineChart.getData().clear();
                    lineChart.getData().add(series);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
