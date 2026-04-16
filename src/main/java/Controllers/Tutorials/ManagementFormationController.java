package Controllers.Tutorials;

import Entities.Tutorials.Formation;
import Services.Tutorials.ServiceFormation;
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

public class ManagementFormationController implements Initializable {

    private PageHost dashboardContext;

    public void setDashboardContext(PageHost context) {
        this.dashboardContext = context;
    }

    @FXML
    void handleBack(ActionEvent event) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Tutorials/TutorialsSelector.fxml");
        }
    }

    @FXML
    private TableColumn<Formation, Integer> colActions;
    @FXML
    private TableColumn<Formation, Integer> colId;
    @FXML
    private TableColumn<Formation, String> colTitre;
    @FXML
    private TableColumn<Formation, String> colNiveau;
    @FXML
    private TableView<Formation> formationTable;

    @FXML
    private TextField searchField;

    private ServiceFormation serviceFormation = new ServiceFormation();
    private ObservableList<Formation> observableList = FXCollections.observableArrayList();
    private FilteredList<Formation> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveau"));

        loadFormations();

        filteredList = new FilteredList<>(observableList, p -> true);
        formationTable.setItems(filteredList);
        searchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredList.setPredicate(f -> lower.isEmpty()
                    || f.getTitre().toLowerCase().contains(lower)
                    || f.getNiveau().toLowerCase().contains(lower));
        });

        colActions.setCellFactory(param -> new TableCell<Formation, Integer>() {
            private final Button btnUpdate = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(10, btnUpdate, btnDelete);

            {
                btnUpdate.getStyleClass().add("edit-button");
                btnDelete.getStyleClass().add("delete-button");

                btnDelete.setOnAction(event -> {
                    Formation item = getTableView().getItems().get(getIndex());
                    try {
                        serviceFormation.delete(item);
                        loadFormations();
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Succès");
                        alert.setHeaderText(null);
                        alert.setContentText("Formation supprimée avec succès.");
                        alert.showAndWait();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

                btnUpdate.setOnAction(event -> {
                    Formation item = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/EditFormation.fxml"));
                        Parent root = loader.load();
                        EditFormationController controller = loader.getController();
                        controller.setFormation(item);
                        Stage stage = new Stage();
                        stage.setTitle("Edit Formation");
                        stage.setScene(new Scene(root));
                        stage.showAndWait();
                        loadFormations();
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

    private void loadFormations() {
        observableList.clear();
        try {
            observableList.addAll(serviceFormation.getAll());
            if (filteredList != null)
                formationTable.setItems(filteredList);
            else
                formationTable.setItems(observableList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleAddFormation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/AjouterFormation.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add New Formation");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadFormations();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
