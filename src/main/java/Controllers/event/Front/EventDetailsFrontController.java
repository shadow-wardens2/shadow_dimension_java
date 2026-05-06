package Controllers.event.Front;

import Entities.User.User;
import Entities.event.Event;
import Entities.event.EventRatingSummary;
import Entities.event.Reservation;
import Entities.event.Review;
import Services.event.ReservationService;
import Services.event.ReviewService;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

public class EventDetailsFrontController {

    @FXML
    private Label lbTitle;
    @FXML
    private Label lbMeta;
    @FXML
    private Label lbDescription;
    @FXML
    private Label lbReservationStatus;
    @FXML
    private Label lbRatingSummary;
    @FXML
    private ImageView eventImageView;
    @FXML
    private Label lbImageStatus;
    @FXML
    private TextArea taComment;
    @FXML
    private Button btnReserve;
    @FXML
    private Button btnSaveReview;
    @FXML
    private Button btnStar1;
    @FXML
    private Button btnStar2;
    @FXML
    private Button btnStar3;
    @FXML
    private Button btnStar4;
    @FXML
    private Button btnStar5;

    private final ReservationService reservationService = new ReservationService();
    private final ReviewService reviewService = new ReviewService();

    private Event event;
    private int selectedRating = 0;

    public void setEvent(Event event) {
        this.event = event;
        refreshView();
    }

    @FXML
    private void handleReserve() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Authentication", "Please login before reserving.");
            return;
        }

        try {
            reservationService.reserve(user.getId(), event.getId());
            refreshReservationStatus();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Reservation created with PENDING status.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.WARNING, "Reservation", ex.getMessage());
        }
    }

    @FXML
    private void handleSaveReview() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Authentication", "Please login to submit review.");
            return;
        }

        try {
            reviewService.createOrUpdate(user.getId(), event.getId(), selectedRating, taComment.getText());
            refreshReviews();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Review saved.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.WARNING, "Review", ex.getMessage());
        }
    }

    @FXML
    private void handleBackHome() {
        loadPage("/HomeFront.fxml");
    }

    @FXML
    private void handleBackToEvents() {
        loadPage("/event/Front/EventFront.fxml");
    }

    @FXML
    private void handleOpenMyEvents() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Authentication", "Please login to view your accepted reservations.");
            return;
        }
        loadPage("/event/Front/MyEvents.fxml");
    }

    @FXML
    private void pickStar1() {
        setRating(1);
    }

    @FXML
    private void pickStar2() {
        setRating(2);
    }

    @FXML
    private void pickStar3() {
        setRating(3);
    }

    @FXML
    private void pickStar4() {
        setRating(4);
    }

    @FXML
    private void pickStar5() {
        setRating(5);
    }

    private void refreshView() {
        if (event == null) {
            return;
        }

        lbTitle.setText(event.getTitle());
        lbMeta.setText(event.getLocation() + " | " + event.getStartDate() + " -> " + event.getEndDate());
        lbDescription.setText(event.getDescription());
        loadEventImage();

        refreshReservationStatus();
        refreshReviews();
        loadCurrentUserReview();
    }

    private void loadEventImage() {
        String imageUrl = event.getImage() == null ? "" : event.getImage().trim();
        if (imageUrl.isBlank()) {
            eventImageView.setImage(null);
            lbImageStatus.setText("No image URL for this event.");
            return;
        }

        try {
            Image image;
            if (imageUrl.startsWith("/uploads/")) {
                String absolutePath = System.getProperty("user.dir") + imageUrl;
                image = new Image(new java.io.File(absolutePath).toURI().toString(), true);
            } else {
                image = new Image(imageUrl, true);
            }
            eventImageView.setImage(image);
            lbImageStatus.setText("");
        } catch (Exception e) {
            eventImageView.setImage(null);
            lbImageStatus.setText("Unable to load image from URL.");
        }
    }

    private void refreshReservationStatus() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            lbReservationStatus.setText("Not logged in");
            btnReserve.setText("Reserve");
            btnReserve.setDisable(false);
            return;
        }

        Optional<Reservation> reservation = reservationService.getUserReservation(user.getId(), event.getId());
        if (reservation.isPresent()) {
            String status = reservation.get().getStatus().name();
            lbReservationStatus.setText("Your reservation: " + status);
            btnReserve.setText(status);
            btnReserve.setDisable(true);
        } else {
            lbReservationStatus.setText("No reservation yet");
            btnReserve.setText("Reserve");
            btnReserve.setDisable(false);
        }
    }

    private void refreshReviews() {
        EventRatingSummary summary = reviewService.getRatingSummary(event.getId());
        double average = summary.getTotalReviews() == 0 ? 0.0 : summary.getAverageRating();
        lbRatingSummary.setText(String.format(Locale.ROOT, "Average rating: %.1f / 5", average));
    }

    private void loadCurrentUserReview() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }

        Optional<Review> existing = reviewService.findByUserAndEvent(user.getId(), event.getId());
        existing.ifPresent(review -> {
            taComment.setText(review.getComment());
            setRating(review.getRating());
        });
    }

    private void setRating(int rating) {
        selectedRating = rating;
        styleStar(btnStar1, rating >= 1);
        styleStar(btnStar2, rating >= 2);
        styleStar(btnStar3, rating >= 3);
        styleStar(btnStar4, rating >= 4);
        styleStar(btnStar5, rating >= 5);
    }

    private void styleStar(Button button, boolean active) {
        button.setStyle(active ? "-fx-background-color: #f4c430; -fx-text-fill: black;" : "");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadPage(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            btnReserve.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation", e.getMessage());
        }
    }
}
