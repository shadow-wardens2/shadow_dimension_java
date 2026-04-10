package Controllers.User;

import Entities.User.User;
import Services.User.ServiceUser;
import Utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;

public class ManagementUsersController {

    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, Integer> colId;

    @FXML
    private TableColumn<User, String> colUsername;

    @FXML
    private TableColumn<User, String> colEmail;

    @FXML
    private TableColumn<User, String> colRoles;

    @FXML
    private TableColumn<User, Integer> colActive;

    @FXML
    private TableColumn<User, Integer> colLocked;

    @FXML
    private TextField tfEmail;

    @FXML
    private TextField tfUsername;

    @FXML
    private TextField tfRoles;

    @FXML
    private TextField tfFullName;

    @FXML
    private TextField tfPhone;

    @FXML
    private TextField tfCountry;

    @FXML
    private TextField tfCity;

    @FXML
    private TextArea taBio;

    @FXML
    private CheckBox cbActive;

    @FXML
    private CheckBox cbLocked;

    private final ServiceUser serviceUser = new ServiceUser();
    private final ObservableList<User> users = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        User current = SessionManager.getCurrentUser();
        if (current == null || !current.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Access denied", "Only admins can manage users.");
            return;
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRoles.setCellValueFactory(new PropertyValueFactory<>("roles"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("isActive"));
        colLocked.setCellValueFactory(new PropertyValueFactory<>("isLocked"));

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                fillForm(selected);
            }
        });

        loadUsers();
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
    }

    @FXML
    private void handleSaveUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection", "Select a user first.");
            return;
        }

        try {
            selected.setEmail(tfEmail.getText().trim());
            selected.setUsername(tfUsername.getText().trim());
            selected.setRoles(normalizeRoles(tfRoles.getText().trim()));
            selected.setFullName(tfFullName.getText().trim());
            selected.setPhone(tfPhone.getText().trim());
            selected.setCountry(tfCountry.getText().trim());
            selected.setCity(tfCity.getText().trim());
            selected.setBio(taBio.getText().trim());
            selected.setIsActive(cbActive.isSelected() ? 1 : 0);
            selected.setIsLocked(cbLocked.isSelected() ? 1 : 0);

            serviceUser.updateUserByAdmin(selected);

            User current = SessionManager.getCurrentUser();
            if (current != null && current.getId() == selected.getId()) {
                SessionManager.setCurrentUser(selected);
            }

            showAlert(Alert.AlertType.INFORMATION, "Success", "User updated.");
            loadUsers();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "SQL Error", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection", "Select a user first.");
            return;
        }

        User current = SessionManager.getCurrentUser();
        if (current != null && current.getId() == selected.getId()) {
            showAlert(Alert.AlertType.WARNING, "Blocked", "You cannot delete your own admin account.");
            return;
        }

        try {
            serviceUser.deleteUserById(selected.getId());
            showAlert(Alert.AlertType.INFORMATION, "Success", "User deleted.");
            clearForm();
            loadUsers();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "SQL Error", e.getMessage());
        }
    }

    private void loadUsers() {
        users.clear();
        try {
            users.addAll(serviceUser.getAllUsers());
            usersTable.setItems(users);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "SQL Error", e.getMessage());
        }
    }

    private void fillForm(User user) {
        tfEmail.setText(valueOrEmpty(user.getEmail()));
        tfUsername.setText(valueOrEmpty(user.getUsername()));
        tfRoles.setText(valueOrEmpty(user.getRoles()));
        tfFullName.setText(valueOrEmpty(user.getFullName()));
        tfPhone.setText(valueOrEmpty(user.getPhone()));
        tfCountry.setText(valueOrEmpty(user.getCountry()));
        tfCity.setText(valueOrEmpty(user.getCity()));
        taBio.setText(valueOrEmpty(user.getBio()));
        cbActive.setSelected(user.getIsActive() == 1);
        cbLocked.setSelected(user.getIsLocked() == 1);
    }

    private void clearForm() {
        tfEmail.clear();
        tfUsername.clear();
        tfRoles.clear();
        tfFullName.clear();
        tfPhone.clear();
        tfCountry.clear();
        tfCity.clear();
        taBio.clear();
        cbActive.setSelected(false);
        cbLocked.setSelected(false);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
