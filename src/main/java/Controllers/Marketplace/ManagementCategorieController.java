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

public class ManagementCategorieController implements Initializable {

    @FXML
    private TableColumn<Categorie, Integer> colActions;

    @FXML
    private TableColumn<Categorie, Integer> colId;

    @FXML
    private TableColumn<Categorie, String> colNom;

    @FXML
    private TableColumn<Categorie, String> colDescription;

    @FXML
    private TableView<Categorie> categorieTable;

    private ServiceCategorie serviceCategorie = new ServiceCategorie();
    private ObservableList<Categorie> observableCategories = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        loadCategories();

        colActions.setCellFactory(param -> new TableCell<Categorie, Integer>() {
            private final Button btnUpdate = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(10, btnUpdate, btnDelete);

            {
                btnUpdate.getStyleClass().add("edit-button");
                btnDelete.getStyleClass().add("delete-button");

                btnDelete.setOnAction(event -> {
                    Categorie c = getTableView().getItems().get(getIndex());
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
                });

                btnUpdate.setOnAction(event -> {
                    Categorie c = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/EditCategorie.fxml"));
                        javafx.scene.Parent root = loader.load();
                        EditCategorieController controller = loader.getController();
                        controller.setCategorie(c);
                        Stage stage = new Stage();
                        stage.setTitle("Editer Catégorie");
                        stage.setScene(new Scene(root));
                        stage.showAndWait();
                        loadCategories();
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

    private void loadCategories() {
        observableCategories.clear();
        try {
            observableCategories.addAll(serviceCategorie.getAll());
            categorieTable.setItems(observableCategories);
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
