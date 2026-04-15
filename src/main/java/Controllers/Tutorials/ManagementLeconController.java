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

public class ManagementLeconController implements Initializable {
    private PageHost dashboardContext;

    @FXML
    private VBox listContainer;
    @FXML
    private TextField searchField;

    private ServiceLecon serviceLecon = new ServiceLecon();
    private ObservableList<Lecon> observableList = FXCollections.observableArrayList();
    private FilteredList<Lecon> filteredList;

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
        loadLecons();

        filteredList = new FilteredList<>(observableList, p -> true);
        searchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredList.setPredicate(l -> lower.isEmpty()
                    || l.getTitre().toLowerCase().contains(lower));
            renderList();
        });

        renderList();
    }

    private void loadLecons() {
        try {
            observableList.setAll(serviceLecon.getAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderList() {
        listContainer.getChildren().clear();
        for (Lecon l : filteredList) {
            listContainer.getChildren().add(createCard(l));
        }
    }

    private HBox createCard(Lecon l) {
        HBox card = new HBox(15);
        card.getStyleClass().add("panel-card");
        card.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 15;");

        VBox details = new VBox(5);
        Label lblTitre = new Label(l.getTitre());
        lblTitre.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label lblOrdre = new Label("Ordre: " + l.getOrdre());
        lblOrdre.setStyle("-fx-text-fill: #aaa;");
        details.getChildren().addAll(lblTitre, lblOrdre);
        HBox.setHgrow(details, Priority.ALWAYS);

        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("edit-button");
        btnEdit.setOnAction(e -> handleEdit(l));

        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("delete-button");
        btnDelete.setOnAction(e -> handleDelete(l));

        card.getChildren().addAll(details, btnEdit, btnDelete);
        return card;
    }

    private void handleEdit(Lecon l) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/EditLecon.fxml"));
            Parent root = loader.load();
            EditLeconController controller = loader.getController();
            controller.setLecon(l);
            Stage stage = new Stage();
            stage.setTitle("Edit Leçon");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadLecons();
            renderList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Lecon l) {
        try {
            serviceLecon.delete(l);
            loadLecons();
            renderList();
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
            renderList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
