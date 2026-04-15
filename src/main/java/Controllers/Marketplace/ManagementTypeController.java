package Controllers.Marketplace;

import Entities.Marketplace.Type;
import Services.Marketplace.ServiceType;
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
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.ResourceBundle;

public class ManagementTypeController implements Initializable {

    @FXML
    private TilePane typesContainer;

    @FXML
    private TextField searchField;

    private ServiceType serviceType = new ServiceType();
    private ObservableList<Type> observableTypes = FXCollections.observableArrayList();
    private FilteredList<Type> filteredData;
    private SortedList<Type> sortedData;
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
            observableTypes.setAll(serviceType.getAll());
            filteredData = new FilteredList<>(observableTypes, p -> true);
            sortedData = new SortedList<>(filteredData);
            sortedData.setComparator(Comparator.comparingInt(Type::getId));
            displayTypes();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void applyFilter() {
        String filter = searchField.getText();
        filteredData.setPredicate(type -> {
            if (filter == null || filter.isEmpty()) return true;
            String lower = filter.toLowerCase();
            return type.getNom().toLowerCase().contains(lower) || 
                   String.valueOf(type.getId()).contains(lower);
        });
        displayTypes();
    }

    private void displayTypes() {
        typesContainer.getChildren().clear();
        for (Type t : sortedData) {
            typesContainer.getChildren().add(createTypeCard(t));
        }
    }

    private VBox createTypeCard(Type t) {
        VBox card = new VBox(15);
        card.getStyleClass().add("product-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(240);

        Label nameLabel = new Label(t.getNom());
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setWrapText(true);

        Label idLabel = new Label("ID: " + t.getId());
        idLabel.getStyleClass().add("subheadline");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("edit-button");
        btnEdit.setOnAction(e -> handleEditAction(t));

        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("delete-button");
        btnDelete.setOnAction(e -> handleDeleteAction(t));

        actions.getChildren().addAll(btnEdit, btnDelete);

        card.getChildren().addAll(nameLabel, idLabel, actions);

        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                handleEditAction(t);
            }
        });

        return card;
    }

    private void handleEditAction(Type t) {
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

    private void handleDeleteAction(Type t) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Type");
        alert.setHeaderText("Delete " + t.getNom() + "?");
        alert.setContentText("Are you sure? This might affect products using this type.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    serviceType.delete(t);
                    loadTypes();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error", "Could not delete type: " + e.getMessage());
                }
            }
        });
    }

    private void loadTypes() {
        try {
            observableTypes.setAll(serviceType.getAll());
            displayTypes();
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
