package Controllers.event;

import Entities.event.Event;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.function.Consumer;

public class EventCardController {

    @FXML
    private ImageView eventImage;
    @FXML
    private Label eventTitle;
    @FXML
    private Label eventDescription;
    @FXML
    private Label eventMeta;
    @FXML
    private Label eventStatus;
    @FXML
    private Label eventCapacity;
    @FXML
    private Label eventCategory;
    @FXML
    private Button btnWeather;

    private Event event;
    private Consumer<Event> onEdit;
    private Consumer<Event> onDelete;
    private Consumer<Event> onWeather;

    public void setEventData(Event event, Consumer<Event> onEdit, Consumer<Event> onDelete, Consumer<Event> onWeather) {
        this.event = event;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.onWeather = onWeather;

        eventTitle.setText(safe(event.getTitle()));
        eventDescription.setText(safe(event.getDescription()));
        eventStatus.setText(safe(event.getStatus()));
        eventCapacity.setText(String.valueOf(event.getCapacity()));
        eventCategory.setText("Category: " + safe(event.getCategoryName()));
        eventMeta.setText(buildMeta(event.getLocation(), event.getStartDate(), event.getEndDate()));

        loadImage(event.getImage());
        updateWeatherVisibility(event.getLocationType());
    }

    private void updateWeatherVisibility(String locationType) {
        boolean outdoor = locationType != null && locationType.equalsIgnoreCase("outdoor");
        btnWeather.setVisible(outdoor);
        btnWeather.setManaged(outdoor);
    }

    private String buildMeta(String location, Timestamp start, Timestamp end) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String startText = start == null ? "N/A" : format.format(start);
        String endText = end == null ? "N/A" : format.format(end);
        String loc = safe(location);
        return loc + " | " + startText + " -> " + endText;
    }

    private void loadImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            setPlaceholderImage();
            return;
        }

        String trimmed = imageUrl.trim();
        try {
            if (trimmed.startsWith("http")) {
                eventImage.setImage(new Image(trimmed, true));
                return;
            }

            if (trimmed.startsWith("/uploads/")) {
                String absolutePath = System.getProperty("user.dir") + trimmed;
                File file = new File(absolutePath);
                if (file.exists()) {
                    eventImage.setImage(new Image(file.toURI().toString()));
                    return;
                }
            }

            File file = new File(trimmed);
            if (file.exists()) {
                eventImage.setImage(new Image(file.toURI().toString()));
                return;
            }

            setPlaceholderImage();
        } catch (Exception e) {
            setPlaceholderImage();
        }
    }

    private void setPlaceholderImage() {
        eventImage.setImage(new Image("https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=400&auto=format&fit=crop&q=60"));
        eventImage.setOpacity(0.6);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }

    @FXML
    private void handleEdit(ActionEvent event) {
        if (onEdit != null) {
            onEdit.accept(this.event);
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (onDelete != null) {
            onDelete.accept(this.event);
        }
    }

    @FXML
    private void handleWeather(ActionEvent event) {
        if (onWeather != null) {
            onWeather.accept(this.event);
        }
    }
}
