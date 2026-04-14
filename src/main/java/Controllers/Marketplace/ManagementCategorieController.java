package Controllers.Marketplace;

import Entities.Marketplace.Categorie;
import Services.Marketplace.ServiceCategorie;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.transformation.FilteredList;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ManagementCategorieController implements Initializable {

    @FXML
    private FlowPane categoriesContainer;

    @FXML
    private TextField searchField;

    private ServiceCategorie serviceCategorie = new ServiceCategorie();
    private ObservableList<Categorie> observableCategories = FXCollections.observableArrayList();
    private PageHost dashboardContext;

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @FXML
    void goBack(ActionEvent event) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Marketplace/MarketplaceSelector.fxml");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupData();
    }

    private void setupData() {
        try {
            observableCategories.setAll(serviceCategorie.getAll());
            FilteredList<Categorie> filteredData = new FilteredList<>(observableCategories, c -> true);

            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(categorie -> {
                    if (newValue == null || newValue.isEmpty()) return true;
                    String lower = newValue.toLowerCase();
                    return categorie.getNom().toLowerCase().contains(lower) ||
                           (categorie.getDescription() != null && categorie.getDescription().toLowerCase().contains(lower)) ||
                           String.valueOf(categorie.getId()).contains(lower);
                });
                displayCategories(filteredData);
            });

            displayCategories(filteredData);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayCategories(Iterable<Categorie> categories) {
        categoriesContainer.getChildren().clear();
        for (Categorie c : categories) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/CategoryCard.fxml"));
                Parent card = loader.load();
                CategoryCardController controller = loader.getController();
                controller.setCategory(c, () -> handleEditCategory(c), () -> handleDeleteCategory(c));
                categoriesContainer.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleEditCategory(Categorie c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/EditCategorie.fxml"));
            Parent root = loader.load();
            EditCategorieController controller = loader.getController();
            controller.setCategorie(c);
            Stage stage = new Stage();
            stage.setTitle("Editer Catégorie");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadCategories();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteCategory(Categorie c) {
        try {
            serviceCategorie.delete(c);
            loadCategories();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText("Catégorie supprimée avec succès.");
            alert.showAndWait();
        } catch (SQLException e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible de supprimer");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void loadCategories() {
        try {
            observableCategories.setAll(serviceCategorie.getAll());
            // No need to re-setup search listener, just display the data
            // However, displayCategories needs to know the current filtered state.
            // Simplified: just call setupData if it's easier, or just refresh display.
            setupData(); 
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleAddCategorie(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/AjouterCategorie.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add New Category");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadCategories();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
