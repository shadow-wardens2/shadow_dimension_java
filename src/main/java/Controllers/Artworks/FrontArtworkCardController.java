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
                File file = new File(artwork.getImageurl());
                if (file.exists()) {
                    artworkImage.setImage(new Image(file.toURI().toString()));
                } else {
                    artworkImage.setImage(new Image(artwork.getImageurl(), true));
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
}
