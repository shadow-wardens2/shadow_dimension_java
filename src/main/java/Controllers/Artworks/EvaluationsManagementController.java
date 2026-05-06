package Controllers.Artworks;

import Entities.Artworks.Evaluation;
import Services.Artworks.ServiceEvaluations;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import java.sql.SQLException;
import java.util.List;
import Controllers.Marketplace.Back.PageHost;

public class EvaluationsManagementController {

    @FXML private TableView<Evaluation> evalTable;
    @FXML private TableColumn<Evaluation, String> colArtwork;
    @FXML private TableColumn<Evaluation, String> colComment;
    @FXML private TableColumn<Evaluation, String> colDate;
    @FXML private TableColumn<Evaluation, Void> colActions;
    @FXML private TextField searchField;
    @FXML private Label lbTotalEvaluations;

    private ServiceEvaluations serviceEvaluations = new ServiceEvaluations();
    private ObservableList<Evaluation> evaluationList = FXCollections.observableArrayList();
    private PageHost dashboardContext;

    public void setDashboardContext(PageHost context) {
        this.dashboardContext = context;
    }

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
        setupSearch();
    }

    private void setupColumns() {
        colArtwork.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getArtworkTitle()));
        colComment.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getComment()));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDate()));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");
            {
                deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
                deleteBtn.setOnAction(event -> {
                    Evaluation ev = getTableView().getItems().get(getIndex());
                    handleDelete(ev);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(deleteBtn);
            }
        });
    }

    private void loadData() {
        try {
            List<Evaluation> data = serviceEvaluations.getAllWithArtworks();
            evaluationList.setAll(data);
            evalTable.setItems(evaluationList);
            
            updateStats(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateStats(List<Evaluation> data) {
        lbTotalEvaluations.setText(String.valueOf(data.size()));
    }

    private void setupSearch() {
        FilteredList<Evaluation> filteredData = new FilteredList<>(evaluationList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(eval -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return eval.getArtworkTitle().toLowerCase().contains(lowerCaseFilter) ||
                       eval.getComment().toLowerCase().contains(lowerCaseFilter);
            });
        });
        evalTable.setItems(filteredData);
    }

    private void handleDelete(Evaluation ev) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete this evaluation?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    serviceEvaluations.delete(ev.getId());
                    loadData();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
