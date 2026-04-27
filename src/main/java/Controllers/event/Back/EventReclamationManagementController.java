package Controllers.event.Back;

import Entities.User.User;
import Entities.event.EventReclamation;
import Entities.event.EventReclamationStatus;
import Services.event.EventReclamationService;
import Utils.SessionManager;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class EventReclamationManagementController implements Initializable {

    @FXML
    private TableView<EventReclamation> reclamationTable;
    @FXML
    private TableColumn<EventReclamation, Integer> colId;
    @FXML
    private TableColumn<EventReclamation, String> colUser;
    @FXML
    private TableColumn<EventReclamation, String> colEvent;
    @FXML
    private TableColumn<EventReclamation, String> colStatus;
    @FXML
    private TableColumn<EventReclamation, String> colCreatedAt;
    @FXML
    private TableColumn<EventReclamation, String> colClaim;
    @FXML
    private TableColumn<EventReclamation, String> colAiSummary;
    @FXML
    private TextField tfSearch;
    @FXML
    private ComboBox<String> cbSort;
    @FXML
    private ComboBox<String> cbStatus;
    @FXML
    private ComboBox<String> cbAction;
    @FXML
    private TextArea taResponse;
    @FXML
    private Label lbPageInfo;

    private final EventReclamationService reclamationService = new EventReclamationService();
    private int currentPage = 1;
    private final int pageSize = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!isAdmin()) {
            showAlert(Alert.AlertType.ERROR, "Security", "Only admins can access reclamation moderation.");
            return;
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUser.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUsername()));
        colEvent.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getEventTitle()));
        colStatus.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getStatusLabel()));
        colCreatedAt.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue().getCreatedAt())));
        colClaim.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getMessage()));
        colAiSummary.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getAiResponse()));

        cbSort.setItems(FXCollections.observableArrayList("Newest", "Oldest", "User", "Event", "Status"));
        cbSort.getSelectionModel().selectFirst();

        cbStatus.setItems(FXCollections.observableArrayList("ALL", "OPEN", "AI_RESPONDED", "IN_PROGRESS", "ESCALATED", "RESOLVED", "REJECTED"));
        cbStatus.getSelectionModel().selectFirst();

        cbAction.setItems(FXCollections.observableArrayList("IN_PROGRESS", "RESOLVED", "REJECTED"));
        cbAction.getSelectionModel().selectFirst();

        tfSearch.textProperty().addListener((obs, oldValue, newValue) -> {
            currentPage = 1;
            loadRows();
        });

        cbSort.valueProperty().addListener((obs, oldValue, newValue) -> loadRows());
        cbStatus.valueProperty().addListener((obs, oldValue, newValue) -> {
            currentPage = 1;
            loadRows();
        });

        reclamationTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                taResponse.clear();
                return;
            }
            String existing = selected.getAdminResponse();
            taResponse.setText(existing == null ? "" : existing);
        });

        loadRows();
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadRows();
        }
    }

    @FXML
    private void handleNextPage() {
        User actor = SessionManager.getCurrentUser();
        int total = reclamationService.countBackOfficeRows(tfSearch.getText(), cbStatus.getValue(), actor);
        int maxPage = (int) Math.ceil((double) total / pageSize);
        if (currentPage < maxPage) {
            currentPage++;
            loadRows();
        }
    }

    @FXML
    private void handleSuggestAiResponse() {
        EventReclamation selected = reclamationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection", "Select a reclamation first.");
            return;
        }
        taResponse.setText(reclamationService.buildAiAdminReplySuggestion(selected));
    }

    @FXML
    private void handleApplyDecision() {
        EventReclamation selected = reclamationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection", "Select a reclamation first.");
            return;
        }

        User actor = SessionManager.getCurrentUser();
        try {
            EventReclamationStatus status = EventReclamationStatus.valueOf(cbAction.getValue());
            reclamationService.adminRespond(selected.getId(), status, taResponse.getText(), actor);
            loadRows();
            showAlert(Alert.AlertType.INFORMATION, "Moderation", "Reclamation updated.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Moderation", e.getMessage());
        }
    }

    private void loadRows() {
        User actor = SessionManager.getCurrentUser();

        String sortBy;
        boolean asc;
        String selectedSort = cbSort.getValue();
        if ("Oldest".equals(selectedSort)) {
            sortBy = "id";
            asc = true;
        } else if ("User".equals(selectedSort)) {
            sortBy = "username";
            asc = true;
        } else if ("Event".equals(selectedSort)) {
            sortBy = "eventTitle";
            asc = true;
        } else if ("Status".equals(selectedSort)) {
            sortBy = "status";
            asc = true;
        } else {
            sortBy = "id";
            asc = false;
        }

        List<EventReclamation> rows = reclamationService.findBackOfficeRows(
                tfSearch.getText(),
                cbStatus.getValue(),
                sortBy,
                asc,
                currentPage,
                pageSize,
                actor
        );
        reclamationTable.setItems(FXCollections.observableArrayList(rows));

        int total = reclamationService.countBackOfficeRows(tfSearch.getText(), cbStatus.getValue(), actor);
        int maxPage = Math.max(1, (int) Math.ceil((double) total / pageSize));
        lbPageInfo.setText("Page " + currentPage + " / " + maxPage + " (" + total + " rows)");
    }

    private boolean isAdmin() {
        User user = SessionManager.getCurrentUser();
        return user != null && user.isAdmin();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
