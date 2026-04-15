package Controllers.Marketplace;

import Entities.Marketplace.Categorie;
import Services.Marketplace.ServiceCategorie;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.ResourceBundle;

public class ManagementCategorieController implements Initializable {

    @FXML
    private TilePane categoriesContainer;

    @FXML
    private TextField searchField;

    private ServiceCategorie serviceCategorie = new ServiceCategorie();
    private ObservableList<Categorie> observableCategories = FXCollections.observableArrayList();
    private FilteredList<Categorie> filteredData;
    private SortedList<Categorie> sortedData;
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
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    private void setupData() {
        try {
            observableCategories.setAll(serviceCategorie.getAll());
            filteredData = new FilteredList<>(observableCategories, p -> true);
            sortedData = new SortedList<>(filteredData);
            sortedData.setComparator(Comparator.comparingInt(Categorie::getId));
            displayCategories();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void applyFilter() {
        String filter = searchField.getText();
        filteredData.setPredicate(cat -> {
            if (filter == null || filter.isEmpty()) return true;
            String lower = filter.toLowerCase();
            return cat.getNom().toLowerCase().contains(lower) || 
                   (cat.getDescription() != null && cat.getDescription().toLowerCase().contains(lower)) ||
                   String.valueOf(cat.getId()).contains(lower);
        });
        displayCategories();
    }

    private void displayCategories() {
        categoriesContainer.getChildren().clear();
        for (Categorie c : sortedData) {
            categoriesContainer.getChildren().add(createCategoryCard(c));
        }
    }

    private VBox createCategoryCard(Categorie c) {
        VBox card = new VBox(12);
        card.getStyleClass().add("product-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(240);

        Label nameLabel = new Label(c.getNom());
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setWrapText(true);

        Label descLabel = new Label(c.getDescription());
        descLabel.getStyleClass().add("product-description");
        descLabel.setWrapText(true);
        descLabel.setMinHeight(60);
        VBox.setVgrow(descLabel, Priority.ALWAYS);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("edit-button");
        btnEdit.setOnAction(e -> handleEditAction(c));

        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("delete-button");
        btnDelete.setOnAction(e -> handleDeleteAction(c));

        actions.getChildren().addAll(btnEdit, btnDelete);

        card.getChildren().addAll(nameLabel, descLabel, actions);

        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                handleEditAction(c);
            }
        });

        return card;
    }

    private void handleEditAction(Categorie c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/EditCategorie.fxml"));
            Parent root = loader.load();
            EditCategorieController controller = loader.getController();
            controller.setCategorie(c);
            Stage stage = new Stage();
            stage.setTitle("Edit Category");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadCategories();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteAction(Categorie c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Category");
        alert.setHeaderText("Delete " + c.getNom() + "?");
        alert.setContentText("Are you sure? All products in this category might be affected.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    serviceCategorie.delete(c);
                    loadCategories();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error", "Could not delete category: " + e.getMessage());
                }
            }
        });
    }

    private void loadCategories() {
        try {
            observableCategories.setAll(serviceCategorie.getAll());
            displayCategories();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
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
