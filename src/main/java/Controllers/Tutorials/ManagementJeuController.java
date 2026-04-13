package Controllers.Tutorials;

import Entities.Tutorials.Jeu;
import Services.Tutorials.ServiceJeu;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

import Controllers.Marketplace.PageHost;

public class ManagementJeuController implements Initializable {
    private PageHost dashboardContext;

    public void setDashboardContext(PageHost context) {
        this.dashboardContext = context;
    }

    @FXML
    void handleBack(javafx.event.ActionEvent event) {
        if (dashboardContext != null)
            dashboardContext.loadPage("/Tutorials/TutorialsSelector.fxml");
    }

    @FXML
    private TableColumn<Jeu, Integer> colActions;
    @FXML
    private TableColumn<Jeu, Integer> colId;
    @FXML
    private TableColumn<Jeu, String> colNom;
    @FXML
    private TableColumn<Jeu, String> colGenre;
    @FXML
    private TableView<Jeu> jeuTable;

    @FXML
    private TextField searchField;

    private ServiceJeu serviceJeu = new ServiceJeu();
    private ObservableList<Jeu> observableList = FXCollections.observableArrayList();
    private FilteredList<Jeu> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colGenre.setCellValueFactory(new PropertyValueFactory<>("genre"));

        loadJeux();

        filteredList = new FilteredList<>(observableList, p -> true);
        jeuTable.setItems(filteredList);
        searchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredList.setPredicate(j -> lower.isEmpty()
                    || j.getNom().toLowerCase().contains(lower)
                    || j.getGenre().toLowerCase().contains(lower));
        });

        colActions.setCellFactory(param -> new TableCell<Jeu, Integer>() {
            private final Button btnUpdate = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(10, btnUpdate, btnDelete);

            {
                btnUpdate.getStyleClass().add("edit-button");
                btnDelete.getStyleClass().add("delete-button");

                btnDelete.setOnAction(event -> {
                    Jeu item = getTableView().getItems().get(getIndex());
                    try {
                        serviceJeu.delete(item);
                        loadJeux();
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Succès");
                        alert.setHeaderText(null);
                        alert.setContentText("Jeu supprimé avec succès.");
                        alert.showAndWait();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

                btnUpdate.setOnAction(event -> {
                    Jeu item = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/EditJeu.fxml"));
                        Parent root = loader.load();
                        EditJeuController controller = loader.getController();
                        controller.setJeu(item);
                        Stage stage = new Stage();
                        stage.setTitle("Edit Jeu");
                        stage.setScene(new Scene(root));
                        stage.showAndWait();
                        loadJeux();
                    } catch (IOException e) {
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

    private void loadJeux() {
        observableList.clear();
        try {
            observableList.addAll(serviceJeu.getAll());
            if (filteredList != null)
                jeuTable.setItems(filteredList);
            else
                jeuTable.setItems(observableList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleAddJeu(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/AjouterJeu.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add New Jeu");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadJeux();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
