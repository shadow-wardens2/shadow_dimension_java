package Controllers.event.Back;

import Entities.event.Event;
import Services.event.EventService;
import Services.event.ReservationService;
import Services.event.WeatherService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class EventWeatherController {

    @FXML
    private Label lbEventTitle;
    @FXML
    private Label lbLocation;
    @FXML
    private Label lbDates;
    @FXML
    private Label lbWarning;
    @FXML
    private VBox forecastContainer;
    @FXML
    private DatePicker dpNewDate;
    @FXML
    private Button btnReschedule;
    @FXML
    private Button btnRefresh;

    private final WeatherService weatherService = new WeatherService();
    private final EventService eventService = new EventService();
    private final ReservationService reservationService = new ReservationService();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Event event;

    public void setEvent(Event event) {
        this.event = event;
        refreshHeader();
        loadForecast();
    }

    @FXML
    private void handleRefresh() {
        loadForecast();
    }

    @FXML
    private void handleReschedule() {
        if (event == null) {
            showAlert(Alert.AlertType.ERROR, "Event", "No event selected.");
            return;
        }

        LocalDate newDate = dpNewDate.getValue();
        if (newDate == null) {
            showAlert(Alert.AlertType.WARNING, "Reschedule", "Select a new start date first.");
            return;
        }

        Timestamp oldStart = event.getStartDate();
        Timestamp oldEnd = event.getEndDate();
        if (oldStart == null || oldEnd == null) {
            showAlert(Alert.AlertType.ERROR, "Reschedule", "Existing dates are missing for this event.");
            return;
        }

        long durationSeconds = Duration.between(oldStart.toInstant(), oldEnd.toInstant()).getSeconds();
        if (durationSeconds <= 0) {
            durationSeconds = Duration.ofHours(2).getSeconds();
        }

        LocalDateTime newStartDateTime = LocalDateTime.of(newDate, LocalTime.of(0, 0));
        Timestamp newStart = Timestamp.valueOf(newStartDateTime);
        Timestamp newEnd = Timestamp.from(newStart.toInstant().plusSeconds(durationSeconds));

        event.setStartDate(newStart);
        event.setEndDate(newEnd);

        try {
            eventService.update(event);
            int notified = reservationService.notifyEventReschedule(event, oldStart, oldEnd);
            refreshHeader();
            showAlert(Alert.AlertType.INFORMATION, "Reschedule",
                    "Event updated. Notifications sent to " + notified + " reservations.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Reschedule", e.getMessage());
        }
    }

    private void refreshHeader() {
        if (event == null) {
            return;
        }

        lbEventTitle.setText(event.getTitle() == null ? "Event Weather" : event.getTitle());
        lbLocation.setText(event.getLocation() == null ? "" : event.getLocation());

        String start = event.getStartDate() == null
                ? "TBA"
                : formatter.format(event.getStartDate().toInstant().atZone(ZoneId.systemDefault()));
        String end = event.getEndDate() == null
                ? "TBA"
                : formatter.format(event.getEndDate().toInstant().atZone(ZoneId.systemDefault()));

        lbDates.setText("Schedule: " + start + " to " + end);
    }

    private void loadForecast() {
        if (event == null) {
            return;
        }

        btnRefresh.setDisable(true);
        forecastContainer.getChildren().clear();
        lbWarning.setText("Loading forecast...");

        CompletableFuture.supplyAsync(() -> {
            try {
                return weatherService.getForecast(event.getLocation());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(result -> Platform.runLater(() -> {
            btnRefresh.setDisable(false);
            renderForecast(result);
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                btnRefresh.setDisable(false);
                showForecastError(ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage());
            });
            return null;
        });
    }

    private void renderForecast(WeatherService.ForecastResult result) {
        forecastContainer.getChildren().clear();
        if (result == null || result.days().isEmpty()) {
            lbWarning.setText("No forecast available.");
            return;
        }

        boolean badWeather = result.days().stream().anyMatch(WeatherService.ForecastDay::badWeather);
        lbWarning.setText(badWeather
                ? "Warning: bad weather expected in the next 7 days. Consider rescheduling."
                : "No severe weather warnings for the next 7 days.");

        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
        for (WeatherService.ForecastDay day : result.days()) {
            HBox row = new HBox(12);
            row.setStyle("-fx-padding: 8 12; -fx-background-color: rgba(14, 14, 17, 0.7); -fx-background-radius: 10;");

            Label date = new Label(dayFormatter.format(day.date()));
            Label condition = new Label(day.description());
            Label temps = new Label(String.format(Locale.ROOT, "%.0f/%.0f C", day.minTemp(), day.maxTemp()));
            Label rain = new Label(String.format(Locale.ROOT, "%.1f mm", day.precipitation()));

            date.getStyleClass().add("body-md");
            condition.getStyleClass().add("body-md");
            temps.getStyleClass().add("body-md");
            rain.getStyleClass().add("body-md");

            row.getChildren().addAll(date, condition, temps, rain);
            forecastContainer.getChildren().add(row);
        }
    }

    private void showForecastError(String message) {
        forecastContainer.getChildren().clear();
        lbWarning.setText("Weather service unavailable.");
        if (message != null && !message.isBlank()) {
            Label error = new Label(message);
            error.getStyleClass().add("subheadline");
            forecastContainer.getChildren().add(error);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
