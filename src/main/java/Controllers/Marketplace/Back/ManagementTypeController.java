package Controllers.Marketplace;

import Entities.Marketplace.Type;
import Services.Marketplace.ServiceType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.collections.transformation.FilteredList;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ManagementTypeController implements Initializable {

    @FXML
    private TilePane typeTilePane;

    @FXML
    private TextField searchField;

    private ServiceType serviceType = new ServiceType();
    private ObservableList<Type> observableTypes = FXCollections.observableArrayList();
    private FilteredList<Type> filteredData;
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

        // Listen for changes in the filtered list to update the UI
        filteredData.addListener((ListChangeListener<Type>) c -> refreshGrid());

        // Initial display
        refreshGrid();
    }

    private void setupData() {
        try {
            observableTypes.setAll(serviceType.getAll());
            filteredData = new FilteredList<>(observableTypes, t -> true);
            
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredData.setPredicate(type -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    String lower = newVal.toLowerCase();
                    return type.getNom().toLowerCase().contains(lower) || 
                           String.valueOf(type.getId()).contains(lower);
                });
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshGrid() {
        typeTilePane.getChildren().clear();
        for (Type type : filteredData) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/TypeCard.fxml"));
                Parent card = loader.load();
                TypeCardController controller = loader.getController();
                controller.setTypeData(type, this::editType, this::deleteType);
                typeTilePane.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void editType(Type t) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/EditType.fxml"));
            Parent root = loader.load();
            EditTypeController controller = loader.getController();
            controller.setType(t);
            Stage stage = new Stage();
            stage.setTitle("Edit Type");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadTypes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteType(Type t) {
        try {
            serviceType.delete(t);
            loadTypes();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Type deleted successfully.");
            alert.showAndWait();
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not delete");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void loadTypes() {
        try {
            observableTypes.setAll(serviceType.getAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleAddType(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/AjouterType.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add New Type");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadTypes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
