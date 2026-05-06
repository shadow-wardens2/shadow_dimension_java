package Controllers.event.Front;

import Entities.event.Event;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.function.Consumer;

public class EventCardFrontController {

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

    private Event event;
    private Consumer<Event> onDetails;

    public void setEventData(Event event, Consumer<Event> onDetails) {
        this.event = event;
        this.onDetails = onDetails;

        eventTitle.setText(safe(event.getTitle()));
        eventDescription.setText(safe(event.getDescription()));
        eventStatus.setText(safe(event.getStatus()));
        eventCapacity.setText(String.valueOf(event.getCapacity()));
        eventCategory.setText("Category: " + safe(event.getCategoryName()));
        eventMeta.setText(buildMeta(event.getLocation(), event.getStartDate(), event.getEndDate()));

        loadImage(event.getImage());
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
    private void handleDetails(ActionEvent event) {
        if (onDetails != null) {
            onDetails.accept(this.event);
        }
    }
}
