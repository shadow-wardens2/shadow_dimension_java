package Controllers.User;

import Entities.User.User;
import Services.User.ServiceUser;
import Utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserStatisticsController {

    // KPI labels.

    @FXML
    private Label lbTotalUsers;

    @FXML
    private Label lbAdmins;

    @FXML
    private Label lbCreators;

    @FXML
    private Label lbLocked;

    @FXML
    private PieChart pieRoles;

    @FXML
    private PieChart pieStatus;

    @FXML
    private BarChart<String, Number> barRegistrations;

    // Service dependency for user dataset.
    private final ServiceUser serviceUser = new ServiceUser();

    // Initializes admin-only statistics page.
    @FXML
    public void initialize() {
        User current = SessionManager.getCurrentUser();
        if (current == null || !current.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Access denied", "Only admins can view user statistics.");
            return;
        }

        loadStatistics();
    }

    // Manual refresh action.
    @FXML
    private void handleRefreshStats() {
        loadStatistics();
    }

    // Aggregates role/status/registration data and updates charts.
    private void loadStatistics() {
        try {
            List<User> users = serviceUser.getAllUsers();

            int total = users.size();
            int admins = 0;
            int creators = 0;
            int usersRole = 0;
            int locked = 0;
            int active = 0;
            int inactive = 0;

            Map<String, Integer> byMonth = new LinkedHashMap<>();

            for (User user : users) {
                String role = user.getRank();
                if ("ADMIN".equals(role)) {
                    admins++;
                } else if ("CREATOR".equals(role)) {
                    creators++;
                } else {
                    usersRole++;
                }

                if (user.getIsLocked() == 1) {
                    locked++;
                }
                if (user.getIsActive() == 1) {
                    active++;
                } else {
                    inactive++;
                }

                String joined = user.getLastPresence();
                String monthKey = monthKey(joined);
                byMonth.put(monthKey, byMonth.getOrDefault(monthKey, 0) + 1);
            }

            lbTotalUsers.setText(String.valueOf(total));
            lbAdmins.setText(String.valueOf(admins));
            lbCreators.setText(String.valueOf(creators));
            lbLocked.setText(String.valueOf(locked));

            pieRoles.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Admin", admins),
                    new PieChart.Data("Creator", creators),
                    new PieChart.Data("User", usersRole)
            ));

            pieStatus.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Active", active),
                    new PieChart.Data("Inactive", inactive),
                    new PieChart.Data("Locked", locked)
            ));

            barRegistrations.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Users Joined");

            List<Map.Entry<String, Integer>> ordered = new ArrayList<>(byMonth.entrySet());
            for (Map.Entry<String, Integer> entry : ordered) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
            barRegistrations.getData().add(series);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "SQL Error", e.getMessage());
        }
    }

    // Groups joined date into year-month bucket for bar chart.
    private String monthKey(String lastPresence) {
        if (lastPresence == null || lastPresence.isBlank() || "-".equals(lastPresence)) {
            return "Unknown";
        }

        try {
            LocalDate date = LocalDate.parse(lastPresence, DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH));
            return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
        } catch (Exception ignored) {
            return "Unknown";
        }
    }

    // Generic alert helper.
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
