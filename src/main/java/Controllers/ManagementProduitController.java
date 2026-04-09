package Controllers;

import Entities.Produit;
import Services.ServiceProduit;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ManagementProduitController implements Initializable {

    @FXML
    private TableColumn<Produit, Integer> colActions;

    @FXML
    private TableColumn<Produit, Integer> colCatId;

    @FXML
    private TableColumn<Produit, String> colDescription;

    @FXML
    private TableColumn<Produit, Integer> colId;

    @FXML
    private TableColumn<Produit, String> colNom;

    @FXML
    private TableColumn<Produit, Double> colPrix;

    @FXML
    private TableColumn<Produit, Integer> colStock;

    @FXML
    private TableColumn<Produit, Integer> colTypeId;

    @FXML
    private TableView<Produit> productsTable;

    private ServiceProduit serviceProduit = new ServiceProduit();
    private ObservableList<Produit> observableProducts = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colCatId.setCellValueFactory(new PropertyValueFactory<>("categorieId"));
        colTypeId.setCellValueFactory(new PropertyValueFactory<>("typeId"));

        loadProducts();

        // Custom action cell
        colActions.setCellFactory(param -> new TableCell<Produit, Integer>() {
            private final Button btnUpdate = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(10, btnUpdate, btnDelete);

            {
                btnUpdate.getStyleClass().add("edit-button");
                btnDelete.getStyleClass().add("delete-button");

                btnDelete.setOnAction(event -> {
                    Produit p = getTableView().getItems().get(getIndex());
                    try {
                        serviceProduit.delete(p);
                        loadProducts(); // refresh table
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Succès");
                        alert.setHeaderText(null);
                        alert.setContentText("Produit supprimé avec succès.");
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
                    Produit p = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/EditProduit.fxml"));
                        javafx.scene.Parent root = loader.load();
                        EditProduitController controller = loader.getController();
                        controller.setProduit(p);
                        Stage stage = new Stage();
                        stage.setTitle("Editer Produit");
                        stage.setScene(new Scene(root));
                        stage.showAndWait();
                        loadProducts();
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

    private void loadProducts() {
        observableProducts.clear();
        try {
            observableProducts.addAll(serviceProduit.getAll());
            productsTable.setItems(observableProducts);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleAddProduct(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/AjouterProduit.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add New Product");
            stage.setScene(new Scene(root));
            stage.showAndWait(); // Wait for it to close
            // refresh data after closing
            loadProducts();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
