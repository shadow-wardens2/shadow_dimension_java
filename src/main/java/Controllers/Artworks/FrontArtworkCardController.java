package Controllers.Artworks;

import Entities.Artworks.Artworks;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import java.io.File;

public class FrontArtworkCardController {

    @FXML private ImageView artworkImage;
    @FXML private Label statusBadge;
    @FXML private Text titleText;
    @FXML private Label categoryLabel;
    @FXML private Label descriptionLabel;
    @FXML private Text priceText;
    @FXML private Pane statusContainer;

    private Artworks artwork;
    private ArtworksFrontController mainController;

    public void setData(Artworks artwork, ArtworksFrontController mainController) {
        this.artwork = artwork;
        this.mainController = mainController;

        titleText.setText(artwork.getTitle());
        descriptionLabel.setText(artwork.getDescription());
        priceText.setText(artwork.getPrice() + " DT");
        statusBadge.setText(artwork.getStatus().toUpperCase());
        
        categoryLabel.setText("ID: " + artwork.getCategoryID());

        // Status styling
        if ("Available".equalsIgnoreCase(artwork.getStatus())) {
            statusBadge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #10b981; -fx-padding: 4 12; -fx-background-radius: 20;");
        } else {
            statusBadge.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #ef4444; -fx-padding: 4 12; -fx-background-radius: 20;");
        }

        // Image loading
        if (artwork.getImageurl() != null && !artwork.getImageurl().isEmpty()) {
            try {
                String path = artwork.getImageurl();
                File file;
                if (path.startsWith("/uploads/") || path.startsWith("\\uploads\\")) {
                    file = new File(System.getProperty("user.dir") + path);
                } else if (!path.startsWith("http") && !path.contains(":") && !path.startsWith("data:")) {
                    file = new File(System.getProperty("user.dir") + "/uploads/artworks/" + path);
                } else {
                    file = new File(path);
                }

                if (file.exists()) {
                    artworkImage.setImage(new Image(file.toURI().toString()));
                } else {
                    artworkImage.setImage(new Image(path, true));
                }
            } catch (Exception e) {
                System.err.println("Could not load image: " + artwork.getImageurl());
            }
        }
    }

    @FXML
    private void handleViewDetails() {
        mainController.navigateToDetails(artwork);
    }

    @FXML
    private void handleReserve() {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Relic Reservation Ritual");
        dialog.setHeaderText("Manifest your reservation for: " + artwork.getTitle());
        dialog.setContentText("Please enter your ethereal mail address:");

        // --- Premium Thematic Styling ---
        javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dialogPane.setStyle("-fx-background-color: #050507; -fx-border-color: #8b5cf6; -fx-border-width: 1; -fx-border-radius: 15; -fx-background-radius: 15;");
        
        // Style the content area
        javafx.scene.Node content = dialogPane.lookup(".content.label");
        if (content != null) content.setStyle("-fx-text-fill: #adaaae; -fx-font-family: 'Inter';");
        
        // Style the header
        javafx.scene.Node header = dialogPane.lookup(".header-panel");
        if (header != null) {
            header.setStyle("-fx-background-color: #1a1a24; -fx-border-color: #8b5cf6; -fx-border-width: 0 0 1 0;");
            javafx.scene.control.Label headerLabel = (javafx.scene.control.Label) header.lookup(".label");
            if (headerLabel != null) headerLabel.setStyle("-fx-text-fill: #ba9eff; -fx-font-family: 'Cinzel'; -fx-font-size: 16px; -fx-font-weight: bold;");
        }

        // --- Custom Icon Manifestation ---
        javafx.scene.shape.SVGPath iconPath = new javafx.scene.shape.SVGPath();
        iconPath.setContent("M12,2L4.5,20.29L5.21,21L12,18L18.79,21L19.5,20.29L12,2Z"); // A sigil-like icon
        iconPath.setFill(javafx.scene.paint.Color.web("#8b5cf6"));
        iconPath.setStroke(javafx.scene.paint.Color.web("#ba9eff"));
        iconPath.setStrokeWidth(1);
        
        javafx.scene.layout.StackPane iconContainer = new javafx.scene.layout.StackPane(iconPath);
        iconContainer.setStyle("-fx-padding: 10; -fx-background-color: rgba(139, 92, 246, 0.1); -fx-background-radius: 50;");
        dialog.setGraphic(iconContainer);

        dialog.showAndWait().ifPresent(email -> {
            if (isValidEmail(email)) {
                try {
                    Services.Artworks.ServiceReservations reservationService = new Services.Artworks.ServiceReservations();
                    if (reservationService.exists(artwork.getId(), email)) {
                        showError("Already Manifested", "You have already reserved this relic with this mail address.");
                        return;
                    }

                    // Proceed with reservation
                    reservationService.add(artwork.getId(), email);
                    
                    Services.Artworks.MailService mailService = new Services.Artworks.MailService();
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert waitAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        waitAlert.setTitle("Manifesting...");
                        waitAlert.setHeaderText(null);
                        waitAlert.setContentText("Sending your confirmation scroll through the void...");
                        waitAlert.show();
                        
                        new Thread(() -> {
                            try {
                                mailService.sendReservationEmail(email, artwork.getTitle(), artwork.getPrice());
                                javafx.application.Platform.runLater(() -> {
                                    waitAlert.close();
                                    showInfo("Reservation Manifested", "Check your scrolls (inbox) for confirmation.");
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                javafx.application.Platform.runLater(() -> {
                                    waitAlert.close();
                                    showError("Ritual Failed", "Could not send confirmation email: " + e.getMessage());
                                });
                            }
                        }).start();
                    });
                } catch (java.sql.SQLException e) {
                    e.printStackTrace();
                    showError("Database Error", "The dimension is currently unstable. Could not record reservation.");
                }
            } else {
                showError("Invalid Essence", "Only Gmail addresses (@gmail.com) are accepted for this ritual.");
            }
        });
    }

    private boolean isValidEmail(String email) {
        // Strict regex for @gmail.com
        String emailRegex = "^[A-Za-z0-9+_.-]+@gmail\\.com$";
        return email != null && email.matches(emailRegex);
    }

    private void showInfo(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    private void showError(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
