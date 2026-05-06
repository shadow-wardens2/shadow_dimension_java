package Controllers.Forum;

import Services.Forum.GiphyService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

public class GiphyPickerController {

    @FXML private TextField txtSearch;
    @FXML private Button btnSearch;
    @FXML private FlowPane gifGrid;

    private final GiphyService giphyService = new GiphyService();
    private Consumer<String> onGifSelected;

    public void setOnGifSelected(Consumer<String> callback) {
        this.onGifSelected = callback;
    }

    @FXML
    public void initialize() {
        loadTrending();
        
        txtSearch.setOnAction(e -> handleSearch()); // Enter key searches
    }

    private void loadTrending() {
        gifGrid.getChildren().clear();
        new Thread(() -> {
            try {
                List<String> urls = giphyService.getTrendingGifs(20);
                Platform.runLater(() -> renderGifs(urls));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    void handleSearch() {
        String query = txtSearch.getText().trim();
        if (query.isEmpty()) {
            loadTrending();
            return;
        }

        gifGrid.getChildren().clear();
        new Thread(() -> {
            try {
                List<String> urls = giphyService.searchGifs(query, 20);
                Platform.runLater(() -> renderGifs(urls));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void renderGifs(List<String> urls) {
        gifGrid.getChildren().clear();
        for (String url : urls) {
            ImageView imgView = new ImageView();
            // Load in background
            Image img = new Image(url, 200, 200, true, true, true);
            imgView.setImage(img);
            imgView.setFitWidth(130);
            imgView.setPreserveRatio(true);
            imgView.setCursor(Cursor.HAND);
            imgView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 5, 0, 0, 0);");
            
            imgView.setOnMouseEntered(e -> imgView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(139,92,246,0.8), 10, 0, 0, 0);"));
            imgView.setOnMouseExited(e -> imgView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 5, 0, 0, 0);"));
            
            imgView.setOnMouseClicked(e -> {
                if (onGifSelected != null) {
                    onGifSelected.accept(url);
                }
                handleClose();
            });

            gifGrid.getChildren().add(imgView);
        }
    }

    @FXML
    void handleClose() {
        Stage stage = (Stage) txtSearch.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
