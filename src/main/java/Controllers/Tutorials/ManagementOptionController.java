package Controllers.Tutorials;

import Entities.Tutorials.Option;
import Services.Tutorials.ServiceOption;
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

public class ManagementOptionController implements Initializable {

    @FXML
    private VBox listContainer;
    @FXML
    private TextField searchField;

    private ServiceOption serviceOption = new ServiceOption();
    private ObservableList<Option> observableList = FXCollections.observableArrayList();
    private FilteredList<Option> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadOptions();

        filteredList = new FilteredList<>(observableList, p -> true);
        searchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredList.setPredicate(o -> lower.isEmpty() || o.getTexte().toLowerCase().contains(lower));
            renderList();
        });

        renderList();
    }

    private void loadOptions() {
        try {
            observableList.setAll(serviceOption.getAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderList() {
        listContainer.getChildren().clear();
        for (Option o : filteredList) {
            listContainer.getChildren().add(createCard(o));
        }
    }

    private HBox createCard(Option o) {
        HBox card = new HBox(15);
        card.getStyleClass().add("panel-card");
        card.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 15;");

        VBox details = new VBox(5);
        Label lblTexte = new Label(o.getTexte());
        lblTexte.setStyle("-fx-text-fill: " + (o.isEstCorrecte() ? "#4CAF50" : "white")
                + "; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label lblStatus = new Label(o.isEstCorrecte() ? "Correcte" : "Incorrecte");
        lblStatus.setStyle("-fx-text-fill: #aaa;");
        details.getChildren().addAll(lblTexte, lblStatus);
        HBox.setHgrow(details, Priority.ALWAYS);

        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("edit-button");
        btnEdit.setOnAction(e -> handleEdit(o));

        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("delete-button");
        btnDelete.setOnAction(e -> handleDelete(o));

        card.getChildren().addAll(details, btnEdit, btnDelete);
        return card;
    }

    private void handleEdit(Option o) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/EditOption.fxml"));
            Parent root = loader.load();
            EditOptionController controller = loader.getController();
            controller.setOption(o);
            Stage stage = new Stage();
            stage.setTitle("Edit Option");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadOptions();
            renderList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Option o) {
        try {
            serviceOption.delete(o);
            loadOptions();
            renderList();
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
            renderList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
