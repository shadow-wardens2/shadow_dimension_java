package Controllers.Marketplace;

import Entities.Marketplace.Type;
import Services.Marketplace.ServiceType;
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

public class ManagementTypeController implements Initializable {

    @FXML
    private FlowPane typesContainer;

    @FXML
    private TextField searchField;

    private ServiceType serviceType = new ServiceType();
    private ObservableList<Type> observableTypes = FXCollections.observableArrayList();
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
            observableTypes.setAll(serviceType.getAll());
            FilteredList<Type> filteredData = new FilteredList<>(observableTypes, t -> true);

            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(type -> {
                    if (newValue == null || newValue.isEmpty()) return true;
                    String lower = newValue.toLowerCase();
                    return type.getNom().toLowerCase().contains(lower) ||
                           String.valueOf(type.getId()).contains(lower);
                });
                displayTypes(filteredData);
            });

            displayTypes(filteredData);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayTypes(Iterable<Type> types) {
        typesContainer.getChildren().clear();
        for (Type t : types) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/TypeCard.fxml"));
                Parent card = loader.load();
                TypeCardController controller = loader.getController();
                controller.setType(t, () -> handleEditType(t), () -> handleDeleteType(t));
                typesContainer.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleEditType(Type t) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/EditType.fxml"));
            Parent root = loader.load();
            EditTypeController controller = loader.getController();
            controller.setType(t);
            Stage stage = new Stage();
            stage.setTitle("Editer Type");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadTypes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteType(Type t) {
        try {
            serviceType.delete(t);
            loadTypes();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText("Type supprimé avec succès.");
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

    private void loadTypes() {
        try {
            observableTypes.setAll(serviceType.getAll());
            setupData();
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
