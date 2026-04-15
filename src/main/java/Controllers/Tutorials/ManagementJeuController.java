package Controllers.Tutorials;

import Entities.Tutorials.Jeu;
import Services.Tutorials.ServiceJeu;
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

public class ManagementJeuController implements Initializable {
    private PageHost dashboardContext;

    @FXML
    private VBox listContainer;
    @FXML
    private TextField searchField;

    private ServiceJeu serviceJeu = new ServiceJeu();
    private ObservableList<Jeu> observableList = FXCollections.observableArrayList();
    private FilteredList<Jeu> filteredList;

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
        loadJeux();

        filteredList = new FilteredList<>(observableList, p -> true);
        searchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredList.setPredicate(j -> lower.isEmpty()
                    || j.getNom().toLowerCase().contains(lower)
                    || j.getGenre().toLowerCase().contains(lower));
            renderList();
        });

        renderList();
    }

    private void loadJeux() {
        try {
            observableList.setAll(serviceJeu.getAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderList() {
        listContainer.getChildren().clear();
        for (Jeu j : filteredList) {
            listContainer.getChildren().add(createCard(j));
        }
    }

    private HBox createCard(Jeu j) {
        HBox card = new HBox(15);
        card.getStyleClass().add("panel-card");
        card.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 15;");

        VBox details = new VBox(5);
        Label lblNom = new Label(j.getNom());
        lblNom.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label lblGenre = new Label("Genre: " + j.getGenre());
        lblGenre.setStyle("-fx-text-fill: #aaa;");
        details.getChildren().addAll(lblNom, lblGenre);
        HBox.setHgrow(details, Priority.ALWAYS);

        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("edit-button");
        btnEdit.setOnAction(e -> handleEdit(j));

        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("delete-button");
        btnDelete.setOnAction(e -> handleDelete(j));

        card.getChildren().addAll(details, btnEdit, btnDelete);
        return card;
    }

    private void handleEdit(Jeu j) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/EditJeu.fxml"));
            Parent root = loader.load();
            EditJeuController controller = loader.getController();
            controller.setJeu(j);
            Stage stage = new Stage();
            stage.setTitle("Edit Jeu");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadJeux();
            renderList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Jeu j) {
        try {
            serviceJeu.delete(j);
            loadJeux();
            renderList();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleAddJeu(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/AjouterJeu.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add New Jeu");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadJeux();
            renderList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
