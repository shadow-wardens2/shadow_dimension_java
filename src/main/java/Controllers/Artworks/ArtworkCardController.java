package Controllers.Artworks;

import Entities.Artworks.Artworks;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.File;

public class ArtworkCardController {

    @FXML private VBox cardContainer;
    @FXML private ImageView artworkImage;
    @FXML private Label statusBadge;
    @FXML private Text titleText;
    @FXML private Label categoryLabel;
    @FXML private Label descriptionLabel;
    @FXML private Text priceText;

    private Artworks artwork;
    private ListerArtworksController mainController;

    public void setData(Artworks artwork, ListerArtworksController mainController) {
        this.artwork = artwork;
        this.mainController = mainController;

        titleText.setText(artwork.getTitle());
        descriptionLabel.setText(artwork.getDescription());
        priceText.setText(artwork.getPrice() + " DT");
        statusBadge.setText(artwork.getStatus().toUpperCase());
        
        // Category representation (simplification)
        categoryLabel.setText("ID: " + artwork.getCategoryID());

        // Status styling
        if ("Available".equalsIgnoreCase(artwork.getStatus())) {
            statusBadge.getStyleClass().add("status-available");
        } else {
            statusBadge.getStyleClass().add("status-sold");
        }

        // Image loading
        if (artwork.getImageurl() != null && !artwork.getImageurl().isEmpty()) {
            try {
                File file = new File(artwork.getImageurl());
                if (file.exists()) {
                    artworkImage.setImage(new Image(file.toURI().toString()));
                } else {
                    // Try as URL if not a local file
                    artworkImage.setImage(new Image(artwork.getImageurl(), true));
                }
            } catch (Exception e) {
                System.err.println("Could not load image: " + artwork.getImageurl());
            }
        }
    }

    @FXML
    private void handleEdit() {
        mainController.navigateToEdit(artwork);
    }

    @FXML
    private void handleDelete() {
        mainController.deleteArtwork(artwork);
    }

    @FXML
    private void handleCardClick() {
        mainController.navigateToDetails(artwork);
    }
}
