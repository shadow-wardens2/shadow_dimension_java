package Controllers.Tutorials;

import Entities.Tutorials.Option;
import Services.Tutorials.ServiceOption;
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

public class ManagementOptionController implements Initializable {

    @FXML
    private TableColumn<Option, Integer> colActions;
    @FXML
    private TableColumn<Option, Integer> colId;
    @FXML
    private TableColumn<Option, String> colTexte;
    @FXML
    private TableColumn<Option, Boolean> colEstCorrecte;
    @FXML
    private TableView<Option> optionTable;

    private ServiceOption serviceOption = new ServiceOption();
    private ObservableList<Option> observableList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTexte.setCellValueFactory(new PropertyValueFactory<>("texte"));
        colEstCorrecte.setCellValueFactory(new PropertyValueFactory<>("estCorrecte"));

        loadOptions();

        colActions.setCellFactory(param -> new TableCell<Option, Integer>() {
            private final Button btnUpdate = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(10, btnUpdate, btnDelete);

            {
                btnUpdate.getStyleClass().add("edit-button");
                btnDelete.getStyleClass().add("delete-button");

                btnDelete.setOnAction(event -> {
                    Option item = getTableView().getItems().get(getIndex());
                    try {
                        serviceOption.delete(item);
                        loadOptions();
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Succès");
                        alert.setHeaderText(null);
                        alert.setContentText("Option supprimée avec succès.");
                        alert.showAndWait();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

                btnUpdate.setOnAction(event -> {
                    Option item = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/EditOption.fxml"));
                        Parent root = loader.load();
                        EditOptionController controller = loader.getController();
                        controller.setOption(item);
                        Stage stage = new Stage();
                        stage.setTitle("Edit Option");
                        stage.setScene(new Scene(root));
                        stage.showAndWait();
                        loadOptions();
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

    private void loadOptions() {
        observableList.clear();
        try {
            observableList.addAll(serviceOption.getAll());
            optionTable.setItems(observableList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleAddOption(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/AjouterOption.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add New Option");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadOptions();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
