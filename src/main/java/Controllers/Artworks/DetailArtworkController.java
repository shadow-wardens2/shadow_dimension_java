package Controllers.Artworks;

import Entities.Artworks.Artworks;
import Services.Artworks.ServiceArtworks;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import Controllers.Marketplace.PageHost;

import java.io.File;
import java.sql.SQLException;

public class DetailArtworkController {

    @FXML private ImageView artworkImage;
    @FXML private Label statusBadge;
    @FXML private Label categoryLabel;
    @FXML private Text titleText;
    @FXML private Text priceText;
    @FXML private Label idLabel;
    @FXML private Label descriptionLabel;

    private Artworks artwork;
    private ServiceArtworks serviceArtworks = new ServiceArtworks();
    private PageHost dashboardContext;

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    public void setArtworkData(Artworks artwork) {
        this.artwork = artwork;
        
        idLabel.setText("#" + artwork.getId());
        titleText.setText(artwork.getTitle());
        descriptionLabel.setText(artwork.getDescription());
        priceText.setText(artwork.getPrice() + " DT");
        statusBadge.setText(artwork.getStatus().toUpperCase());
        categoryLabel.setText("CATEGORY ID: " + artwork.getCategoryID());

        // Status styling
        statusBadge.getStyleClass().removeAll("status-available", "status-sold");
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
                    artworkImage.setImage(new Image(artwork.getImageurl(), true));
                }
            } catch (Exception e) {
                System.err.println("Could not load image: " + artwork.getImageurl());
            }
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        navigateToGallery();
    }

    @FXML
    private void handleEdit(ActionEvent event) {
        if (dashboardContext != null) {
            Object controller = dashboardContext.loadPage("/Artworks/AjouterArtwork.fxml");
            if (controller instanceof AjouterArtworkController) {
                ((AjouterArtworkController) controller).setArtworkData(artwork);
            }
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Artwork: " + artwork.getTitle());
        alert.setContentText("Are you sure you want to permanentely delete this artwork?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                try {
                    serviceArtworks.delete(artwork);
                    navigateToGallery();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void navigateToGallery() {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Artworks/ListerArtworks.fxml");
        } else if (artworkImage.getScene().getWindow() instanceof javafx.stage.Stage) {
             ((javafx.stage.Stage) artworkImage.getScene().getWindow()).close();
        }
    }
}
