package Controllers.Tutorials;

import Entities.Tutorials.Lecon;
import Services.Tutorials.ServiceLecon;
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

public class ManagementLeconController implements Initializable {
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
    private TableColumn<Lecon, Integer> colActions;
    @FXML
    private TableColumn<Lecon, Integer> colId;
    @FXML
    private TableColumn<Lecon, String> colTitre;
    @FXML
    private TableColumn<Lecon, Integer> colOrdre;
    @FXML
    private TableView<Lecon> leconTable;

    @FXML
    private TextField searchField;

    private ServiceLecon serviceLecon = new ServiceLecon();
    private ObservableList<Lecon> observableList = FXCollections.observableArrayList();
    private FilteredList<Lecon> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colOrdre.setCellValueFactory(new PropertyValueFactory<>("ordre"));

        loadLecons();

        filteredList = new FilteredList<>(observableList, p -> true);
        leconTable.setItems(filteredList);
        searchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredList.setPredicate(l -> lower.isEmpty()
                    || l.getTitre().toLowerCase().contains(lower));
        });

        colActions.setCellFactory(param -> new TableCell<Lecon, Integer>() {
            private final Button btnUpdate = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(10, btnUpdate, btnDelete);

            {
                btnUpdate.getStyleClass().add("edit-button");
                btnDelete.getStyleClass().add("delete-button");

                btnDelete.setOnAction(event -> {
                    Lecon item = getTableView().getItems().get(getIndex());
                    try {
                        serviceLecon.delete(item);
                        loadLecons();
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Succès");
                        alert.setHeaderText(null);
                        alert.setContentText("Leçon supprimée avec succès.");
                        alert.showAndWait();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

                btnUpdate.setOnAction(event -> {
                    Lecon item = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/EditLecon.fxml"));
                        Parent root = loader.load();
                        EditLeconController controller = loader.getController();
                        controller.setLecon(item);
                        Stage stage = new Stage();
                        stage.setTitle("Edit Leçon");
                        stage.setScene(new Scene(root));
                        stage.showAndWait();
                        loadLecons();
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

    private void loadLecons() {
        observableList.clear();
        try {
            observableList.addAll(serviceLecon.getAll());
            if (filteredList != null)
                leconTable.setItems(filteredList);
            else
                leconTable.setItems(observableList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleAddLecon(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/AjouterLecon.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add New Leçon");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadLecons();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
