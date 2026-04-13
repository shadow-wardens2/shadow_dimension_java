package Controllers.Tutorials;

import Entities.Tutorials.Question;
import Services.Tutorials.ServiceQuestion;
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

public class ManagementQuestionController implements Initializable {

    @FXML
    private TableColumn<Question, Integer> colActions;
    @FXML
    private TableColumn<Question, Integer> colId;
    @FXML
    private TableColumn<Question, String> colTexte;
    @FXML
    private TableView<Question> questionTable;

    private ServiceQuestion serviceQuestion = new ServiceQuestion();
    private ObservableList<Question> observableList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTexte.setCellValueFactory(new PropertyValueFactory<>("texte"));

        loadQuestions();

        colActions.setCellFactory(param -> new TableCell<Question, Integer>() {
            private final Button btnUpdate = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(10, btnUpdate, btnDelete);

            {
                btnUpdate.getStyleClass().add("edit-button");
                btnDelete.getStyleClass().add("delete-button");

                btnDelete.setOnAction(event -> {
                    Question item = getTableView().getItems().get(getIndex());
                    try {
                        serviceQuestion.delete(item);
                        loadQuestions();
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Succès");
                        alert.setHeaderText(null);
                        alert.setContentText("Question supprimée avec succès.");
                        alert.showAndWait();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

                btnUpdate.setOnAction(event -> {
                    Question item = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/EditQuestion.fxml"));
                        Parent root = loader.load();
                        EditQuestionController controller = loader.getController();
                        controller.setQuestion(item);
                        Stage stage = new Stage();
                        stage.setTitle("Edit Question");
                        stage.setScene(new Scene(root));
                        stage.showAndWait();
                        loadQuestions();
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

    private void loadQuestions() {
        observableList.clear();
        try {
            observableList.addAll(serviceQuestion.getAll());
            questionTable.setItems(observableList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleAddQuestion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/AjouterQuestion.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add New Question");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadQuestions();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
