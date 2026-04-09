package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class MarketplaceManagementController {

    @FXML
    private Button btnCategories;

    @FXML
    private Button btnProducts;

    @FXML
    private Button btnTypes;

    @FXML
    void openCategories(ActionEvent event) {
        openWindow("/Marketplace/ManagementCategorie.fxml", "Manage Categories");
    }

    @FXML
    void openProducts(ActionEvent event) {
        openWindow("/Marketplace/ManagementProduit.fxml", "Manage Products");
    }

    @FXML
    void openTypes(ActionEvent event) {
        openWindow("/Marketplace/ManagementType.fxml", "Manage Types");
    }

    private void openWindow(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
