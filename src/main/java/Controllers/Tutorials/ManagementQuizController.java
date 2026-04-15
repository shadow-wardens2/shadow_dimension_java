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

public class ManagementQuizController implements Initializable {
    private PageHost dashboardContext;

    @FXML
    private VBox listContainer;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnManageDetails;

    private ServiceQuiz serviceQuiz = new ServiceQuiz();
    private ObservableList<Quiz> obsQuizzes = FXCollections.observableArrayList();
    private FilteredList<Quiz> filteredList;
    private Quiz selectedQuiz;

    public void setDashboardContext(PageHost context) {
        this.dashboardContext = context;
    }

    @FXML
    void handleBack(ActionEvent event) {
        if (dashboardContext != null)
            dashboardContext.loadPage("/Tutorials/TutorialsSelector.fxml");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        btnManageDetails.setDisable(true);
        loadQuizzes();

        filteredList = new FilteredList<>(obsQuizzes, p -> true);
        searchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredList.setPredicate(q -> lower.isEmpty()
                    || q.getTitre().toLowerCase().contains(lower));
            renderList();
        });

        renderList();
    }

    private void loadQuizzes() {
        try {
            obsQuizzes.setAll(serviceQuiz.getAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderList() {
        listContainer.getChildren().clear();
        for (Quiz q : filteredList) {
            listContainer.getChildren().add(createCard(q));
        }
    }

    private HBox createCard(Quiz q) {
        HBox card = new HBox(15);
        card.getStyleClass().add("panel-card");
        card.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 15; -fx-cursor: hand;");

        if (selectedQuiz != null && selectedQuiz.getId() == q.getId()) {
            card.setStyle(
                    "-fx-alignment: CENTER_LEFT; -fx-padding: 15; -fx-cursor: hand; -fx-border-color: #ba9eff; -fx-border-width: 2;");
        }

        card.setOnMouseClicked(e -> {
            selectedQuiz = q;
            btnManageDetails.setDisable(false);
            renderList();
        });

        VBox details = new VBox(5);
        Label lblTitre = new Label(q.getTitre());
        lblTitre.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label lblOrdre = new Label("Ordre: " + q.getOrdre());
        lblOrdre.setStyle("-fx-text-fill: #aaa;");
        details.getChildren().addAll(lblTitre, lblOrdre);
        HBox.setHgrow(details, Priority.ALWAYS);

        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("edit-button");
        btnEdit.setOnAction(e -> editQuiz(q));

        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("delete-button");
        btnDelete.setOnAction(e -> deleteQuiz(q));

        card.getChildren().addAll(details, btnEdit, btnDelete);
        return card;
    }

    @FXML
    void handleAddQuiz(ActionEvent event) {
        openPopup("/Tutorials/AjouterQuiz.fxml", "Add Quiz", null);
        loadQuizzes();
        renderList();
    }

    @FXML
    void handleConfigQuiz(ActionEvent event) {
        if (selectedQuiz != null && dashboardContext != null) {
            ManagementQuizDetailsController.currentContextQuiz = selectedQuiz;
            dashboardContext.loadPage("/Tutorials/ManagementQuizDetails.fxml");
        }
    }

    private void editQuiz(Quiz item) {
        openPopup("/Tutorials/EditQuiz.fxml", "Edit Quiz", c -> ((EditQuizController) c).setQuiz(item));
        loadQuizzes();
        renderList();
    }

    private void deleteQuiz(Quiz item) {
        try {
            serviceQuiz.delete(item);
            if (selectedQuiz != null && selectedQuiz.getId() == item.getId()) {
                selectedQuiz = null;
                btnManageDetails.setDisable(true);
            }
            loadQuizzes();
            renderList();
        } catch (Exception e) {
            e.printStackTrace();
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
