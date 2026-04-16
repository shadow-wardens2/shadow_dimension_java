package Controllers.Tutorials;

import Controllers.Marketplace.PageHost;

import Entities.Tutorials.Quiz;
import Services.Tutorials.ServiceQuiz;
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

public class ManagementQuizController implements Initializable {
    @FXML
    void handleBack(javafx.event.ActionEvent event) {
        if (dashboardContext != null)
            dashboardContext.loadPage("/Tutorials/TutorialsSelector.fxml");
    }

    // Quiz components
    @FXML
    private TableView<Quiz> quizTable;
    @FXML
    private TableColumn<Quiz, Integer> colQuizId;
    @FXML
    private TableColumn<Quiz, String> colQuizTitre;
    @FXML
    private TableColumn<Quiz, Integer> colQuizOrdre;
    @FXML
    private TableColumn<Quiz, Integer> colQuizActions;

    @FXML
    private Button btnManageDetails;

    @FXML
    private TextField searchField;

    // Services
    private ServiceQuiz serviceQuiz = new ServiceQuiz();

    private ObservableList<Quiz> obsQuizzes = FXCollections.observableArrayList();
    private FilteredList<Quiz> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initQuizColumns();
        btnManageDetails.setDisable(true);

        quizTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            btnManageDetails.setDisable(newSel == null);
        });

        loadQuizzes();

        filteredList = new FilteredList<>(obsQuizzes, p -> true);
        quizTable.setItems(filteredList);
        searchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredList.setPredicate(q -> lower.isEmpty()
                    || q.getTitre().toLowerCase().contains(lower));
        });
    }

    private void initQuizColumns() {
        colQuizId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colQuizTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colQuizOrdre.setCellValueFactory(new PropertyValueFactory<>("ordre"));
        colQuizActions.setCellFactory(p -> new ActionCell<>(this::editQuiz, this::deleteQuiz));
    }

    private void loadQuizzes() {
        try {
            obsQuizzes.setAll(serviceQuiz.getAll());
            if (filteredList != null)
                quizTable.setItems(filteredList);
            else
                quizTable.setItems(obsQuizzes);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleAddQuiz(ActionEvent event) {
        openPopup("/Tutorials/AjouterQuiz.fxml", "Add Quiz", null);
        loadQuizzes();
    }

    private PageHost dashboardContext;

    public void setDashboardContext(PageHost context) {
        this.dashboardContext = context;
    }

    @FXML
    void handleConfigQuiz(ActionEvent event) {
        Quiz selected = quizTable.getSelectionModel().getSelectedItem();
        if (selected != null && dashboardContext != null) {
            ManagementQuizDetailsController.currentContextQuiz = selected;
            dashboardContext.loadPage("/Tutorials/ManagementQuizDetails.fxml");
        }
    }

    private void editQuiz(Quiz item) {
        openPopup("/Tutorials/EditQuiz.fxml", "Edit Quiz", c -> ((EditQuizController) c).setQuiz(item));
        loadQuizzes();
    }

    private void deleteQuiz(Quiz item) {
        try {
            serviceQuiz.delete(item);
            loadQuizzes();
        } catch (Exception e) {
        }
    }

    private void openPopup(String fxml, String title, java.util.function.Consumer<Object> controllerSetup) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            if (controllerSetup != null)
                controllerSetup.accept(loader.getController());
            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            } catch (Exception e) {
            }
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ActionCell<T> extends TableCell<T, Integer> {
        private final Button btnEdit = new Button("Edit");
        private final Button btnDel = new Button("Del");
        private final HBox pane = new HBox(5, btnEdit, btnDel);

        public ActionCell(java.util.function.Consumer<T> onEdit, java.util.function.Consumer<T> onDel) {
            btnEdit.getStyleClass().add("edit-button");
            btnDel.getStyleClass().add("delete-button");
            btnEdit.setOnAction(e -> onEdit.accept(getTableView().getItems().get(getIndex())));
            btnDel.setOnAction(e -> onDel.accept(getTableView().getItems().get(getIndex())));
        }

        @Override
        protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : pane);
        }
    }
}
