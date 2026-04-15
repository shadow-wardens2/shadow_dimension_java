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
import Controllers.Marketplace.PageHost;

public class ManagementFormationController implements Initializable {

    private PageHost dashboardContext;

    @FXML
    private VBox listContainer;
    @FXML
    private TextField searchField;

    private ServiceFormation serviceFormation = new ServiceFormation();
    private ObservableList<Formation> observableList = FXCollections.observableArrayList();
    private FilteredList<Formation> filteredList;

    public void setDashboardContext(PageHost context) {
        this.dashboardContext = context;
    }

    @FXML
    void handleBack(ActionEvent event) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Tutorials/TutorialsSelector.fxml");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadFormations();

        filteredList = new FilteredList<>(observableList, p -> true);
        searchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredList.setPredicate(f -> lower.isEmpty()
                    || f.getTitre().toLowerCase().contains(lower)
                    || f.getNiveau().toLowerCase().contains(lower));
            renderList();
        });

        renderList();
    }

    private void loadFormations() {
        try {
            observableList.setAll(serviceFormation.getAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderList() {
        listContainer.getChildren().clear();
        for (Formation f : filteredList) {
            listContainer.getChildren().add(createCard(f));
        }
    }

    private HBox createCard(Formation f) {
        HBox card = new HBox(15);
        card.getStyleClass().add("panel-card");
        card.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 15;");

        VBox details = new VBox(5);
        Label lblTitre = new Label(f.getTitre());
        lblTitre.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label lblNiveau = new Label("Niveau: " + f.getNiveau());
        lblNiveau.setStyle("-fx-text-fill: #aaa;");
        details.getChildren().addAll(lblTitre, lblNiveau);
        HBox.setHgrow(details, Priority.ALWAYS);

        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("edit-button");
        btnEdit.setOnAction(e -> handleEdit(f));

        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("delete-button");
        btnDelete.setOnAction(e -> handleDelete(f));

        card.getChildren().addAll(details, btnEdit, btnDelete);
        return card;
    }

    private void handleEdit(Formation f) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/EditFormation.fxml"));
            Parent root = loader.load();
            EditFormationController controller = loader.getController();
            controller.setFormation(f);
            Stage stage = new Stage();
            stage.setTitle("Edit Formation");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadFormations();
            renderList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Formation f) {
        try {
            serviceFormation.delete(f);
            loadFormations();
            renderList();
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
            renderList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
