package Controllers.Artworks;

import Entities.Artworks.Artworks;
import Entities.Artworks.Categories;
import Services.Artworks.ServiceArtworks;
import Services.Artworks.ServiceCategories;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import Controllers.Marketplace.PageHost;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class ListerArtworksController implements Initializable {

    @FXML private FlowPane artworksGrid;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<Categories> filterCategory;
    @FXML private ComboBox<String> sortOptions;

    private ServiceArtworks serviceArtworks = new ServiceArtworks();
    private ServiceCategories serviceCategories = new ServiceCategories();
    private ObservableList<Artworks> artworksList = FXCollections.observableArrayList();
    private FilteredList<Artworks> filteredData;
    private SortedList<Artworks> sortedData;
    private PageHost dashboardContext;

    public void setDashboardContext(PageHost ctx) {
        this.dashboardContext = ctx;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        filteredData = new FilteredList<>(artworksList, p -> true);
        sortedData = new SortedList<>(filteredData);
        
        // Listen to changes in the sorted list to refresh the grid
        sortedData.addListener((javafx.collections.ListChangeListener<Artworks>) c -> refreshGrid());

        loadData();
        
        try {
            Categories allCat = new Categories("0", "Sort By Category", "");
            filterCategory.getItems().add(allCat);
            filterCategory.getItems().addAll(serviceCategories.getAll());
            filterCategory.setValue(allCat);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        sortOptions.getItems().addAll("Default", "Newest", "Price: Low to High", "Price: High to Low", "Title (A-Z)", "Title (Z-A)");
        sortOptions.setValue("Default");
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        filterCategory.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        sortOptions.valueProperty().addListener((observable, oldValue, newValue) -> applySort(newValue));
        
        // Initial grid fill
        refreshGrid();
    }

    private void refreshGrid() {
        artworksGrid.getChildren().clear();
        for (Artworks artwork : sortedData) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Artworks/ArtworkCard.fxml"));
                Parent card = loader.load();
                ArtworkCardController controller = loader.getController();
                controller.setData(artwork, this);
                artworksGrid.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void applyFilters() {
        String keyword = searchField.getText();
        Categories selectedCat = filterCategory.getValue();
        
        String lowerCaseFilter = (keyword == null) ? "" : keyword.toLowerCase();
        
        filteredData.setPredicate(artwork -> {
            boolean matchesSearch = artwork.getTitle() != null && artwork.getTitle().toLowerCase().contains(lowerCaseFilter);
            
            boolean matchesCategory = true;
            if (selectedCat != null && !selectedCat.getID().equals("0")) {
                matchesCategory = String.valueOf(artwork.getCategoryID()).equals(selectedCat.getID());
            }
            
            return matchesSearch && matchesCategory;
        });
    }

    private void applySort(String criteria) {
        if (criteria == null || criteria.equals("Default")) {
            sortedData.setComparator(null);
            return;
        }

        sortedData.setComparator((a1, a2) -> {
            switch (criteria) {
                case "Newest":
                    return Integer.compare(a2.getId(), a1.getId());
                case "Price: Low to High":
                    return Integer.compare(a1.getPrice(), a2.getPrice());
                case "Price: High to Low":
                    return Integer.compare(a2.getPrice(), a1.getPrice());
                case "Title (A-Z)":
                    return a1.getTitle().compareToIgnoreCase(a2.getTitle());
                case "Title (Z-A)":
                    return a2.getTitle().compareToIgnoreCase(a1.getTitle());
                default:
                    return 0;
            }
        });
    }

    private void loadData() {
        try {
            artworksList.clear();
            List<Artworks> data = serviceArtworks.getAll();
            artworksList.addAll(data);
        } catch (SQLException e) {
            System.err.println("Error Loading Data: " + e.getMessage());
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Database Loading Error");
                alert.setHeaderText("Failed to retrieve artworks");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            });
        }
    }

    void deleteArtwork(Artworks a) {
        try {
            serviceArtworks.delete(a);
            loadData();
        } catch (SQLException e) {
            System.err.println("Error Deleting Artwork: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Failed to delete artwork");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void navigateToAdd(ActionEvent actionEvent) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Artworks/AjouterArtwork.fxml");
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Artworks/AjouterArtwork.fxml"));
                Parent root = loader.load();
                Stage stage = new Stage();
                stage.setTitle("Add New Artwork");
                stage.setScene(new Scene(root));
                stage.showAndWait();
                loadData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void navigateToCategories(ActionEvent actionEvent) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Artworks/ListerCategories.fxml");
        } else {
            System.err.println("Dashboard context is null, skipping navigation");
        }
    }

    void navigateToEdit(Artworks a) {
        if (dashboardContext != null) {
            Object controller = dashboardContext.loadPage("/Artworks/AjouterArtwork.fxml");
            if (controller instanceof AjouterArtworkController) {
                ((AjouterArtworkController) controller).setArtworkData(a);
            }
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Artworks/AjouterArtwork.fxml"));
                Parent root = loader.load();
                
                AjouterArtworkController controller = loader.getController();
                controller.setArtworkData(a);
                
                Stage stage = new Stage();
                stage.setTitle("Edit Artwork");
                stage.setScene(new Scene(root));
                stage.showAndWait();
                loadData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void navigateToDetails(Artworks a) {
        if (dashboardContext != null) {
            Object controller = dashboardContext.loadPage("/Artworks/DetailArtwork.fxml");
            if (controller instanceof DetailArtworkController) {
                ((DetailArtworkController) controller).setDashboardContext(dashboardContext);
                ((DetailArtworkController) controller).setArtworkData(a);
            }
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Artworks/DetailArtwork.fxml"));
                Parent root = loader.load();
                
                DetailArtworkController controller = loader.getController();
                controller.setArtworkData(a);
                
                Stage stage = new Stage();
                stage.setTitle("Artwork Details");
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
