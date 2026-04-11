package Controllers.User;

import Entities.User.User;
import Services.User.ServiceUser;
import Utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public class ManagementUsersController {

    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, Integer> colId;

    @FXML
    private TableColumn<User, String> colUserIdentity;

    @FXML
    private TableColumn<User, String> colRank;

    @FXML
    private TableColumn<User, String> colStatus;

    @FXML
    private TableColumn<User, String> colLastPresence;

    @FXML
    private TableColumn<User, Void> colActions;

    @FXML
    private TextField tfSearch;

    @FXML
    private Button btnSortId;

    private final ServiceUser serviceUser = new ServiceUser();
    private final ObservableList<User> masterUsers = FXCollections.observableArrayList();
    private final ObservableList<User> displayedUsers = FXCollections.observableArrayList();
    private boolean idAscending = false;

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || !currentUser.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Access denied", "Only admins can manage users.");
            return;
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUserIdentity.setCellValueFactory(new PropertyValueFactory<>("userIdentity"));
        colRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colLastPresence.setCellValueFactory(new PropertyValueFactory<>("lastPresence"));

        colUserIdentity.setCellFactory(param -> new TableCell<>() {
                private final Label identityLabel = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                        identityLabel.setText(item);
                        identityLabel.setStyle("-fx-text-fill: #e8e3f5; -fx-font-size: 15px; -fx-font-weight: 600;");
                        setGraphic(identityLabel);
                }
            }
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditRank = new Button("Edit Rank");
            private final Button btnLock = new Button("Lock/Unlock");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(8, btnEditRank, btnLock, btnDelete);

            {
                btnEditRank.getStyleClass().add("secondary-button");
                btnLock.getStyleClass().add("secondary-button");
                btnDelete.getStyleClass().add("delete-button");

                btnEditRank.setOnAction(event -> {
                    User selected = getTableView().getItems().get(getIndex());
                    handleEditRank(selected);
                });

                btnLock.setOnAction(event -> {
                    User selected = getTableView().getItems().get(getIndex());
                    handleToggleLock(selected);
                });

                btnDelete.setOnAction(event -> {
                    User selected = getTableView().getItems().get(getIndex());
                    handleDeleteUser(selected);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });

        tfSearch.textProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndSort());

        loadUsers();
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
    }

    @FXML
    private void handleToggleSortById() {
        idAscending = !idAscending;
        btnSortId.setText(idAscending ? "ID ASC" : "ID DESC");
        applyFiltersAndSort();
    }

    private void handleEditRank(User selected) {
        if (selected == null) {
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(selected.getRank(), Arrays.asList("USER", "ADMIN", "CREATOR"));
        dialog.setTitle("Edit Rank");
        dialog.setHeaderText(null);
        dialog.setContentText("Choose rank:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String roleToken = "ROLE_" + result.get();
        try {
            selected.setRoles(normalizeRoles(roleToken));
            serviceUser.updateUserByAdmin(selected);
            applyFiltersAndSort();

            User current = SessionManager.getCurrentUser();
            if (current != null && current.getId() == selected.getId()) {
                current.setRoles(selected.getRoles());
                SessionManager.setCurrentUser(current);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "SQL Error", e.getMessage());
        }
    }

    private void handleToggleLock(User selected) {
        if (selected == null) {
            return;
        }

        try {
            selected.setIsLocked(selected.getIsLocked() == 1 ? 0 : 1);
            serviceUser.updateUserByAdmin(selected);
            applyFiltersAndSort();

            User current = SessionManager.getCurrentUser();
            if (current != null && current.getId() == selected.getId()) {
                current.setIsLocked(selected.getIsLocked());
                SessionManager.setCurrentUser(current);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "SQL Error", e.getMessage());
        }
    }

    private void handleDeleteUser(User selected) {
        if (selected == null) {
            return;
        }

        User current = SessionManager.getCurrentUser();
        if (current != null && current.getId() == selected.getId()) {
            showAlert(Alert.AlertType.WARNING, "Blocked", "You cannot delete your own admin account.");
            return;
        }

        try {
            serviceUser.deleteUserById(selected.getId());
            loadUsers();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "SQL Error", e.getMessage());
        }
    }

    private void loadUsers() {
        masterUsers.clear();
        try {
            masterUsers.addAll(serviceUser.getAllUsers());
            applyFiltersAndSort();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "SQL Error", e.getMessage());
        }
    }

    private void applyFiltersAndSort() {
        String search = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase();

        displayedUsers.clear();
        for (User user : masterUsers) {
            String name = safe(user.getUsername()).toLowerCase();
            String email = safe(user.getEmail()).toLowerCase();
            String rank = safe(user.getRank()).toLowerCase();
            String id = String.valueOf(user.getId());

            if (search.isBlank() || name.contains(search) || email.contains(search) || rank.contains(search) || id.contains(search)) {
                displayedUsers.add(user);
            }
        }

        displayedUsers.sort(idAscending
                ? Comparator.comparingInt(User::getId)
                : Comparator.comparingInt(User::getId).reversed());

        usersTable.setItems(displayedUsers);
    }

    private String normalizeRoles(String rawRoles) {
        if (rawRoles == null || rawRoles.isBlank()) {
            return "[\"ROLE_USER\"]";
        }

        if (rawRoles.startsWith("[")) {
            return rawRoles;
        }

        // Allows quick input like ROLE_ADMIN while still storing valid JSON.
        return "[\"" + rawRoles.replace("\"", "") + "\"]";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
