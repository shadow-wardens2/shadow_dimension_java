package Controllers.Artworks;

import Entities.Artworks.Artworks;
import Entities.Artworks.Categories;
import Services.Artworks.ServiceArtworks;
import Services.Artworks.ServiceCategories;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import Controllers.Marketplace.PageHost;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AjouterArtworkController implements Initializable {

    @FXML private Text titleText;
    @FXML private TextField txtTitle;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtPrice;
    @FXML private TextField txtImage;
    @FXML private ComboBox<String> comboStatus;
    @FXML private ComboBox<Categories> comboCategory;
    @FXML private Button btnSave;

    private ServiceArtworks serviceArtworks = new ServiceArtworks();
    private ServiceCategories serviceCategories = new ServiceCategories();
    private Artworks currentArtwork = null;
    private PageHost dashboardContext;

    public void setDashboardContext(PageHost ctx) {
        this.dashboardContext = ctx;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        comboStatus.getItems().addAll("Available", "Sold", "In Exhibition");
        try {
            comboCategory.getItems().addAll(serviceCategories.getAll());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Categories Error", "Failed to load categories: " + e.getMessage());
        }
    }

    public void setArtworkData(Artworks a) {
        this.currentArtwork = a;
        titleText.setText("EDIT ARTWORK");
        btnSave.setText("Update");

        txtTitle.setText(a.getTitle());
        txtDescription.setText(a.getDescription());
        txtPrice.setText(String.valueOf(a.getPrice()));
        txtImage.setText(a.getImageurl());
        comboStatus.setValue(a.getStatus());
        for (Categories c : comboCategory.getItems()) {
            if (c.getID() != null && c.getID().equals(String.valueOf(a.getCategoryID()))) {
                comboCategory.setValue(c);
                break;
            }
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (txtTitle.getText().isEmpty() || txtPrice.getText().isEmpty() || txtImage.getText().isEmpty() || comboCategory.getValue() == null) {
            showAlert("Validation Error", "Title, Price, Image URL, and Category are required.");
            return;
        }

        if (txtDescription.getText() == null || txtDescription.getText().trim().length() < 15) {
            showAlert("Validation Error", "The description must be at least 15 characters long.");
            return;
        }

        try {
            int price = Integer.parseInt(txtPrice.getText());
            int categoryId = Integer.parseInt(comboCategory.getValue().getID());

            if (currentArtwork == null) {
                // Add
                Artworks a = new Artworks();
                a.setTitle(txtTitle.getText());
                a.setDescription(txtDescription.getText());
                a.setPrice(price);
                a.setImageurl(txtImage.getText());
                a.setStatus(comboStatus.getValue());
                a.setCategoryID(categoryId);

                serviceArtworks.add(a);
            } else {
                // Update
                currentArtwork.setTitle(txtTitle.getText());
                currentArtwork.setDescription(txtDescription.getText());
                currentArtwork.setPrice(price);
                currentArtwork.setImageurl(txtImage.getText());
                currentArtwork.setStatus(comboStatus.getValue());
                currentArtwork.setCategoryID(categoryId);

                serviceArtworks.update(currentArtwork);
            }

            closeWindow();
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Price must be a valid number.");
        } catch (SQLException e) {
            showAlert("Database Error", e.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Artworks/ListerArtworks.fxml");
        } else {
            Stage stage = (Stage) btnSave.getScene().getWindow();
            stage.close();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
