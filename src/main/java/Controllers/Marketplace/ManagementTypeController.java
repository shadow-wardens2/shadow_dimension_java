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
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
    private TableColumn<Type, Integer> colActions;

    @FXML
    private TableColumn<Type, Integer> colId;

    @FXML
    private TableColumn<Type, String> colNom;

    @FXML
    private TableView<Type> typeTable;

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
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));

        loadTypes();

        colActions.setCellFactory(param -> new TableCell<Type, Integer>() {
            private final Button btnUpdate = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(10, btnUpdate, btnDelete);

            {
                btnUpdate.getStyleClass().add("edit-button");
                btnDelete.getStyleClass().add("delete-button");

                btnDelete.setOnAction(event -> {
                    Type t = getTableView().getItems().get(getIndex());
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
                });

                btnUpdate.setOnAction(event -> {
                    Type t = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/EditType.fxml"));
                        javafx.scene.Parent root = loader.load();
                        EditTypeController controller = loader.getController();
                        controller.setType(t);
                        Stage stage = new Stage();
                        stage.setTitle("Editer Type");
                        stage.setScene(new Scene(root));
                        stage.showAndWait();
                        loadTypes();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }

    private void loadTypes() {
        observableTypes.clear();
        try {
            observableTypes.addAll(serviceType.getAll());
            
            FilteredList<Type> filteredData = new FilteredList<>(observableTypes, t -> true);
            
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(type -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    
                    String lowerCaseFilter = newValue.toLowerCase();
                    
                    if (type.getNom().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    } else if (String.valueOf(type.getId()).contains(lowerCaseFilter)) {
                        return true;
                    }
                    
                    return false;
                });
            });
            
            typeTable.setItems(filteredData);
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
