package Controllers.event.Back;

import Entities.User.User;
import Entities.event.Review;
import Services.event.ReviewService;
import Utils.SessionManager;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ReviewManagementController implements Initializable {

    @FXML
    private TableView<Review> reviewTable;
    @FXML
    private TableColumn<Review, Integer> colId;
    @FXML
    private TableColumn<Review, String> colUser;
    @FXML
    private TableColumn<Review, String> colEvent;
    @FXML
    private TableColumn<Review, Integer> colRating;
    @FXML
    private TableColumn<Review, String> colComment;
    @FXML
    private TableColumn<Review, String> colCreatedAt;
    @FXML
    private TableColumn<Review, String> colActions;
    @FXML
    private TextField tfSearch;
    @FXML
    private ComboBox<String> cbSort;
    @FXML
    private Label lbPageInfo;

    private final ReviewService reviewService = new ReviewService();
    private int currentPage = 1;
    private final int pageSize = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!isAdmin()) {
            showAlert(Alert.AlertType.ERROR, "Security", "Only admins can manage reviews.");
            return;
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUser.setCellValueFactory(cell -> new ReadOnlyStringWrapper(valueOrDash(cell.getValue().getUsername())));
        colEvent.setCellValueFactory(cell -> new ReadOnlyStringWrapper(valueOrDash(cell.getValue().getEventTitle())));
        colRating.setCellValueFactory(new PropertyValueFactory<>("rating"));
        colComment.setCellValueFactory(cell -> new ReadOnlyStringWrapper(valueOrDash(cell.getValue().getComment())));
        colCreatedAt.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue().getCreatedAt())));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("Delete");

            {
                btnDelete.getStyleClass().add("delete-button");
                btnDelete.setOnAction(event -> deleteReview(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });

        cbSort.setItems(FXCollections.observableArrayList("Newest", "Oldest", "User", "Event", "Rating"));
        cbSort.getSelectionModel().selectFirst();

        tfSearch.textProperty().addListener((obs, oldValue, newValue) -> {
            currentPage = 1;
            loadRows();
        });
        cbSort.valueProperty().addListener((obs, oldValue, newValue) -> loadRows());

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
        int total = reviewService.countBackOfficeReviews(tfSearch.getText(), actor);
        int maxPage = (int) Math.ceil((double) total / pageSize);
        if (currentPage < maxPage) {
            currentPage++;
            loadRows();
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
        } else if ("Rating".equals(selectedSort)) {
            sortBy = "rating";
            asc = false;
        } else {
            sortBy = "id";
            asc = false;
        }

        List<Review> rows = reviewService.findBackOfficeReviews(tfSearch.getText(), sortBy, asc, currentPage, pageSize, actor);
        reviewTable.setItems(FXCollections.observableArrayList(rows));

        int total = reviewService.countBackOfficeReviews(tfSearch.getText(), actor);
        int maxPage = Math.max(1, (int) Math.ceil((double) total / pageSize));
        lbPageInfo.setText("Page " + currentPage + " / " + maxPage + " (" + total + " rows)");
    }

    private void deleteReview(Review review) {
        User actor = SessionManager.getCurrentUser();
        try {
            reviewService.deleteReview(review.getId(), actor);
            loadRows();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Delete", e.getMessage());
        }
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
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
