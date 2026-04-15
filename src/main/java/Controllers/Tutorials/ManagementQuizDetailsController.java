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
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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

    @FXML
    private Label lbQuizTitle;
    @FXML
    private VBox questionContainer;
    @FXML
    private VBox optionContainer;
    @FXML
    private TextField questionSearchField;
    @FXML
    private TextField optionSearchField;
    @FXML
    private Button btnAddQuestion;
    @FXML
    private Button btnAddOption;

    private ServiceQuestion serviceQuestion = new ServiceQuestion();
    private ServiceOption serviceOption = new ServiceOption();

    private ObservableList<Question> obsQuestions = FXCollections.observableArrayList();
    private ObservableList<Option> obsOptions = FXCollections.observableArrayList();
    private FilteredList<Question> filteredQuestions;
    private FilteredList<Option> filteredOptions;

    private Quiz selectedQuiz;
    private Question selectedQuestion;

    public void setDashboardContext(PageHost context) {
        this.dashboardContext = context;
    }

    @FXML
    void handleBack(ActionEvent event) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Tutorials/ManagementQuiz.fxml");
        }
    }

    public void setQuiz(Quiz quiz) {
        this.selectedQuiz = quiz;
        lbQuizTitle.setText("Quiz: " + quiz.getTitre());
        refreshQuestions();
        btnAddQuestion.setDisable(false);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        btnAddQuestion.setDisable(true);
        btnAddOption.setDisable(true);

        filteredQuestions = new FilteredList<>(obsQuestions, p -> true);
        filteredOptions = new FilteredList<>(obsOptions, p -> true);

        questionSearchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredQuestions.setPredicate(q -> lower.isEmpty() || q.getTexte().toLowerCase().contains(lower));
            renderQuestions();
        });

        optionSearchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredOptions.setPredicate(o -> lower.isEmpty() || o.getTexte().toLowerCase().contains(lower));
            renderOptions();
        });

        if (currentContextQuiz != null) {
            setQuiz(currentContextQuiz);
        }
    }

    private void refreshQuestions() {
        if (selectedQuiz == null)
            return;
        try {
            obsQuestions.setAll(serviceQuestion.getAll().stream()
                    .filter(q -> q.getQuiz().getId() == selectedQuiz.getId()).collect(Collectors.toList()));
            renderQuestions();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshOptions() {
        if (selectedQuestion == null) {
            obsOptions.clear();
            renderOptions();
            return;
        }
        try {
            obsOptions.setAll(serviceOption.getAll().stream()
                    .filter(o -> o.getQuestion().getId() == selectedQuestion.getId()).collect(Collectors.toList()));
            renderOptions();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderQuestions() {
        questionContainer.getChildren().clear();
        for (Question q : filteredQuestions) {
            questionContainer.getChildren().add(createQuestionCard(q));
        }
    }

    private void renderOptions() {
        optionContainer.getChildren().clear();
        for (Option o : filteredOptions) {
            optionContainer.getChildren().add(createOptionCard(o));
        }
    }

    private HBox createQuestionCard(Question q) {
        HBox card = new HBox(10);
        card.getStyleClass().add("panel-card");
        card.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 10; -fx-cursor: hand;");

        if (selectedQuestion != null && selectedQuestion.getId() == q.getId()) {
            card.setStyle(
                    "-fx-alignment: CENTER_LEFT; -fx-padding: 10; -fx-cursor: hand; -fx-border-color: #ba9eff; -fx-border-width: 1;");
        }

        card.setOnMouseClicked(e -> {
            selectedQuestion = q;
            btnAddOption.setDisable(false);
            refreshOptions();
            renderQuestions();
        });

        Label lbl = new Label(q.getTexte());
        lbl.setStyle("-fx-text-fill: white; -fx-wrap-text: true;");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("edit-button");
        btnEdit.setOnAction(e -> {
            openPopup("/Tutorials/EditQuestion.fxml", "Edit Question",
                    c -> ((EditQuestionController) c).setQuestion(q));
            refreshQuestions();
        });

        Button btnDel = new Button("Del");
        btnDel.getStyleClass().add("delete-button");
        btnDel.setOnAction(e -> {
            try {
                serviceQuestion.delete(q);
                if (selectedQuestion != null && selectedQuestion.getId() == q.getId()) {
                    selectedQuestion = null;
                    btnAddOption.setDisable(true);
                    refreshOptions();
                }
                refreshQuestions();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(lbl, btnEdit, btnDel);
        return card;
    }

    private HBox createOptionCard(Option o) {
        HBox card = new HBox(10);
        card.getStyleClass().add("panel-card");
        card.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 10;");

        Label lbl = new Label(o.getTexte());
        lbl.setStyle("-fx-text-fill: " + (o.isEstCorrecte() ? "#4CAF50" : "white") + "; -fx-wrap-text: true;");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("edit-button");
        btnEdit.setOnAction(e -> {
            openPopup("/Tutorials/EditOption.fxml", "Edit Option", c -> ((EditOptionController) c).setOption(o));
            refreshOptions();
        });

        Button btnDel = new Button("Del");
        btnDel.getStyleClass().add("delete-button");
        btnDel.setOnAction(e -> {
            try {
                serviceOption.delete(o);
                refreshOptions();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(lbl, btnEdit, btnDel);
        return card;
    }

    @FXML
    void handleAddQuestion(ActionEvent event) {
        openPopup("/Tutorials/AjouterQuestion.fxml", "Add Question", c -> {
            if (selectedQuiz != null)
                ((AjouterQuestionController) c).setPreselectedQuiz(selectedQuiz);
        });
        refreshQuestions();
    }

    @FXML
    void handleAddOption(ActionEvent event) {
        if (selectedQuestion != null) {
            openPopup("/Tutorials/AjouterOption.fxml", "Add Option",
                    c -> ((AjouterOptionController) c).setPreselectedQuestion(selectedQuestion));
            refreshOptions();
        }
    }

    private void openPopup(String fxml, String title, java.util.function.Consumer<Object> controllerSetup) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            if (controllerSetup != null)
                controllerSetup.accept(loader.getController());
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
