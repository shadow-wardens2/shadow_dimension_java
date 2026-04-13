package Controllers.User;

import Services.event.EventService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;

import java.net.URL;
import java.sql.SQLException;
import java.util.Map;
import java.util.ResourceBundle;

public class HomeContentController implements Initializable {

    @FXML
    private PieChart eventsByCategoryChart;

    private final EventService eventService = new EventService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadEventsByCategoryChart();
    }

    private void loadEventsByCategoryChart() {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        try {
            Map<String, Integer> counts = eventService.getEventCountByCategory();
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (entry.getValue() > 0) {
                    pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (pieData.isEmpty()) {
            pieData.add(new PieChart.Data("No events yet", 1));
        }

        eventsByCategoryChart.setData(pieData);
        eventsByCategoryChart.setLegendVisible(true);
        eventsByCategoryChart.setLabelsVisible(true);
    }
}
