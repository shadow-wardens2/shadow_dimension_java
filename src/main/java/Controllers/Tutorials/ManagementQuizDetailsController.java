package Controllers.Tutorials;

import Entities.Tutorials.Option;
import Entities.Tutorials.Question;
import Entities.Tutorials.Quiz;
import Services.Tutorials.ServiceOption;
import Services.Tutorials.ServiceQuestion;
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
import javafx.scene.control.Label;
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
import java.util.stream.Collectors;
import Controllers.Marketplace.PageHost;

public class ManagementQuizDetailsController implements Initializable {

    public static Quiz currentContextQuiz;
    private PageHost dashboardContext;

    public void setDashboardContext(PageHost context) {
        this.dashboardContext = context;
    }

    @FXML
    void handleBack(ActionEvent event) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Tutorials/ManagementQuiz.fxml");
        }
    }

    private Quiz selectedQuiz;

    @FXML
    private Label lbQuizTitle;

    // Question components
    @FXML
    private TableView<Question> questionTable;
    @FXML
    private TableColumn<Question, Integer> colQuestionId;
    @FXML
    private TableColumn<Question, String> colQuestionTexte;
    @FXML
    private TableColumn<Question, Integer> colQuestionActions;
    @FXML
    private Button btnAddQuestion;

    // Option components
    @FXML
    private TableView<Option> optionTable;
    @FXML
    private TableColumn<Option, Integer> colOptionId;
    @FXML
    private TableColumn<Option, String> colOptionTexte;
    @FXML
    private TableColumn<Option, Boolean> colOptionEstCorrecte;
    @FXML
    private TableColumn<Option, Integer> colOptionActions;
    @FXML
    private Button btnAddOption;

    private ServiceQuestion serviceQuestion = new ServiceQuestion();
    private ServiceOption serviceOption = new ServiceOption();

    private ObservableList<Question> obsQuestions = FXCollections.observableArrayList();
    private ObservableList<Option> obsOptions = FXCollections.observableArrayList();
    private FilteredList<Question> filteredQuestions;
    private FilteredList<Option> filteredOptions;

    @FXML
    private TextField questionSearchField;
    @FXML
    private TextField optionSearchField;

    public void setQuiz(Quiz quiz) {
        this.selectedQuiz = quiz;
        lbQuizTitle.setText("Configuring details for Quiz: " + quiz.getTitre());
        loadQuestionsForQuiz(quiz.getId());
        btnAddQuestion.setDisable(false);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initQuestionColumns();
        initOptionColumns();

        btnAddQuestion.setDisable(true);
        btnAddOption.setDisable(true);

        filteredQuestions = new FilteredList<>(obsQuestions, p -> true);
        questionTable.setItems(filteredQuestions);
        filteredOptions = new FilteredList<>(obsOptions, p -> true);
        optionTable.setItems(filteredOptions);

        questionSearchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredQuestions.setPredicate(q -> lower.isEmpty() || q.getTexte().toLowerCase().contains(lower));
        });

        optionSearchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredOptions.setPredicate(o -> lower.isEmpty() || o.getTexte().toLowerCase().contains(lower));
        });

        if (currentContextQuiz != null) {
            setQuiz(currentContextQuiz);
        }

        questionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                btnAddOption.setDisable(false);
                loadOptionsForQuestion(newSel.getId());
            }
        });
    }

    private void initQuestionColumns() {
        colQuestionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colQuestionTexte.setCellValueFactory(new PropertyValueFactory<>("texte"));
        colQuestionActions.setCellFactory(p -> new ActionCell<>(this::editQuestion, this::deleteQuestion));
    }

    private void initOptionColumns() {
        colOptionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOptionTexte.setCellValueFactory(new PropertyValueFactory<>("texte"));
        colOptionEstCorrecte.setCellValueFactory(new PropertyValueFactory<>("estCorrecte"));
        colOptionActions.setCellFactory(p -> new ActionCell<>(this::editOption, this::deleteOption));
    }

    private void loadQuestionsForQuiz(int quizId) {
        try {
            obsQuestions.setAll(serviceQuestion.getAll().stream()
                    .filter(q -> q.getQuiz().getId() == quizId).collect(Collectors.toList()));
            questionTable.setItems(obsQuestions);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadOptionsForQuestion(int questionId) {
        try {
            obsOptions.setAll(serviceOption.getAll().stream()
                    .filter(o -> o.getQuestion().getId() == questionId).collect(Collectors.toList()));
            optionTable.setItems(obsOptions);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleAddQuestion(ActionEvent event) {
        openPopup("/Tutorials/AjouterQuestion.fxml", "Add Question", c -> {
            if (selectedQuiz != null) {
                ((AjouterQuestionController) c).setPreselectedQuiz(selectedQuiz);
            }
        });
        refreshQuestions();
    }

    @FXML
    void handleAddOption(ActionEvent event) {
        Question q = questionTable.getSelectionModel().getSelectedItem();
        openPopup("/Tutorials/AjouterOption.fxml", "Add Option", c -> {
            if (q != null) {
                ((AjouterOptionController) c).setPreselectedQuestion(q);
            }
        });
        refreshOptions();
    }

    private void editQuestion(Question item) {
        openPopup("/Tutorials/EditQuestion.fxml", "Edit Question", c -> ((EditQuestionController) c).setQuestion(item));
        refreshQuestions();
    }

    private void editOption(Option item) {
        openPopup("/Tutorials/EditOption.fxml", "Edit Option", c -> ((EditOptionController) c).setOption(item));
        refreshOptions();
    }

    private void deleteQuestion(Question item) {
        try {
            serviceQuestion.delete(item);
            refreshQuestions();
        } catch (Exception e) {
        }
    }

    private void deleteOption(Option item) {
        try {
            serviceOption.delete(item);
            refreshOptions();
        } catch (Exception e) {
        }
    }

    private void refreshQuestions() {
        if (selectedQuiz != null)
            loadQuestionsForQuiz(selectedQuiz.getId());
    }

    private void refreshOptions() {
        Question q = questionTable.getSelectionModel().getSelectedItem();
        if (q != null)
            loadOptionsForQuestion(q.getId());
        else
            obsOptions.clear();
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
