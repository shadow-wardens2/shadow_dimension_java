package Controllers.Artworks;

import Entities.Artworks.Artworks;
import Entities.Artworks.Categories;
import Services.Artworks.ServiceArtworks;
import Services.Artworks.ServiceCategories;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class ArtworksFrontController implements Initializable {

    @FXML private AnchorPane rootNode;
    @FXML private FlowPane artworksGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<Categories> filterCategory;
    @FXML private ComboBox<String> sortOptions;
    @FXML private Label itemCountLabel;

    private ServiceArtworks serviceArtworks = new ServiceArtworks();
    private ServiceCategories serviceCategories = new ServiceCategories();
    private ObservableList<Artworks> artworksList = FXCollections.observableArrayList();
    private FilteredList<Artworks> filteredData;
    private SortedList<Artworks> sortedData;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        filteredData = new FilteredList<>(artworksList, p -> true);
        sortedData = new SortedList<>(filteredData);
        
        sortedData.addListener((javafx.collections.ListChangeListener<Artworks>) c -> refreshGrid());

        loadData();
        
        try {
            Categories allCat = new Categories("0", "All Realms", "");
            filterCategory.getItems().add(allCat);
            filterCategory.getItems().addAll(serviceCategories.getAll());
            filterCategory.setValue(allCat);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        sortOptions.getItems().addAll("Order of Manifestation", "Value: Low to High", "Value: High to Low", "Echoes (A-Z)");
        sortOptions.setValue("Order of Manifestation");
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        filterCategory.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        sortOptions.valueProperty().addListener((observable, oldValue, newValue) -> applySort(newValue));
        
        refreshGrid();
    }

    private void loadData() {
        try {
            artworksList.clear();
            List<Artworks> data = serviceArtworks.getAll();
            artworksList.addAll(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshGrid() {
        artworksGrid.getChildren().clear();
        for (Artworks artwork : sortedData) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Artworks/FrontArtworkCard.fxml"));
                Parent card = loader.load();
                FrontArtworkCardController controller = loader.getController();
                controller.setData(artwork, this);
                artworksGrid.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        itemCountLabel.setText(sortedData.size() + " RELICS FOUND");
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
        if (criteria == null || criteria.equals("Order of Manifestation")) {
            sortedData.setComparator(null);
            return;
        }

        sortedData.setComparator((a1, a2) -> {
            switch (criteria) {
                case "Value: Low to High":
                    return Integer.compare(a1.getPrice(), a2.getPrice());
                case "Value: High to Low":
                    return Integer.compare(a2.getPrice(), a1.getPrice());
                case "Echoes (A-Z)":
                    return a1.getTitle().compareToIgnoreCase(a2.getTitle());
                default:
                    return 0;
            }
        });
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HomeFront.fxml"));
            rootNode.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void navigateToDetails(Artworks a) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Artworks/DetailArtwork.fxml"));
            Parent root = loader.load();
            DetailArtworkController controller = loader.getController();
            controller.setArtworkData(a);
            controller.setIsFrontOffice(true);
            
            rootNode.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
