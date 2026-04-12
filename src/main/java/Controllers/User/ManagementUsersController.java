package Controllers.User;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.GrayColor;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import Entities.User.User;
import Services.User.ServiceUser;
import Utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

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
        colId.setStyle("-fx-alignment: CENTER;");
        colRank.setStyle("-fx-alignment: CENTER;");
        colStatus.setStyle("-fx-alignment: CENTER;");
        colLastPresence.setStyle("-fx-alignment: CENTER;");
        colActions.setStyle("-fx-alignment: CENTER;");
        usersTable.setFixedCellSize(74);
        usersTable.setEditable(true);
        colRank.setEditable(true);

        colRank.setCellFactory(ComboBoxTableCell.forTableColumn("USER", "ADMIN", "CREATOR"));
        colRank.setOnEditCommit(event -> {
            User selected = event.getRowValue();
            String newRank = event.getNewValue();

            if (selected == null || newRank == null || newRank.isBlank()) {
                return;
            }

            try {
                String roleToken = "ROLE_" + newRank;
                selected.setRoles(normalizeRoles(roleToken));
                serviceUser.updateUserByAdmin(selected);
                usersTable.refresh();

                User current = SessionManager.getCurrentUser();
                if (current != null && current.getId() == selected.getId()) {
                    current.setRoles(selected.getRoles());
                    SessionManager.setCurrentUser(current);
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "SQL Error", e.getMessage());
                loadUsers();
            }
        });

        colUserIdentity.setCellFactory(param -> new TableCell<>() {
            private final Label usernameLabel = new Label();
            private final Label emailLabel = new Label();
            private final VBox identityBox = new VBox(2, usernameLabel, emailLabel);

            {
                usernameLabel.setStyle("-fx-text-fill: #f3eefc; -fx-font-size: 14px; -fx-font-weight: 700;");
                emailLabel.setStyle("-fx-text-fill: #9ea3b0; -fx-font-size: 12px;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    String[] lines = item.split("\\n", 2);
                    usernameLabel.setText(lines.length > 0 ? lines[0] : "-");
                    emailLabel.setText(lines.length > 1 ? lines[1] : "-");
                    setGraphic(identityBox);
                }
            }
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnLock = new Button("Lock/Unlock");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(8, btnLock, btnDelete);

            {
                pane.setAlignment(Pos.CENTER);
                btnLock.getStyleClass().add("secondary-button");
                btnDelete.getStyleClass().add("delete-button");

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

    @FXML
    private void handleExportPdf() {
        if (displayedUsers.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Export", "No users to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Users to PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("users_export_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");

        Stage stage = (Stage) usersTable.getScene().getWindow();
        java.io.File file = fileChooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try {
            exportUsersToPdf(file.getAbsolutePath());
            showAlert(Alert.AlertType.INFORMATION, "Export", "PDF exported successfully.");
        } catch (IOException | DocumentException e) {
            showAlert(Alert.AlertType.ERROR, "Export Error", e.getMessage());
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

    private void exportUsersToPdf(String path) throws IOException, DocumentException {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();

        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 11, new GrayColor(0.45f));
        Paragraph generated = new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), metaFont);
        generated.setAlignment(Element.ALIGN_RIGHT);
        generated.setSpacingAfter(12f);
        document.add(generated);
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.9f, 2.7f, 3.8f, 2.0f, 1.5f, 2.1f});

        addHeaderCell(table, "ID");
        addHeaderCell(table, "IDENTITY");
        addHeaderCell(table, "EMAIL");
        addHeaderCell(table, "RANK");
        addHeaderCell(table, "STATUS");
        addHeaderCell(table, "JOINED");

        for (User user : displayedUsers) {
            addDataCell(table, String.valueOf(user.getId()), Element.ALIGN_LEFT);
            addDataCell(table, safe(user.getUsername()), Element.ALIGN_LEFT);
            addDataCell(table, safe(user.getEmail()), Element.ALIGN_LEFT);
            addBadgeCell(table, safe(user.getRank()), rankColor(safe(user.getRank())), new GrayColor(1f));
            addBadgeCell(table, safe(user.getStatus()), statusColor(safe(user.getStatus())), new GrayColor(0f));
            addDataCell(table, safe(user.getLastPresence()), Element.ALIGN_LEFT);
        }

        document.add(table);
        document.close();
    }

    private void addHeaderCell(PdfPTable table, String value) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new GrayColor(0.15f));
        PdfPCell cell = new PdfPCell(new Phrase(value, headerFont));
        cell.setBackgroundColor(new GrayColor(0.88f));
        cell.setBorderColor(new GrayColor(0.78f));
        cell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
        cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        cell.setPadding(8f);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String value, int align) {
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12, new GrayColor(0.15f));
        PdfPCell cell = new PdfPCell(new Phrase(value, bodyFont));
        cell.setBackgroundColor(new GrayColor(0.92f));
        cell.setBorderColor(new GrayColor(0.78f));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        cell.setPadding(9f);
        table.addCell(cell);
    }

    private void addBadgeCell(PdfPTable table, String value, GrayColor bg, GrayColor fg) {
        Font badgeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, fg);
        PdfPCell cell = new PdfPCell(new Phrase(value.toUpperCase(), badgeFont));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(new GrayColor(0.78f));
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        cell.setPadding(9f);
        table.addCell(cell);
    }

    private GrayColor rankColor(String rank) {
        String r = rank == null ? "" : rank.toUpperCase();
        if (r.equals("ADMIN")) {
            return new GrayColor(0.55f);
        }
        if (r.equals("CREATOR")) {
            return new GrayColor(0.48f);
        }
        return new GrayColor(0.63f);
    }

    private GrayColor statusColor(String status) {
        String s = status == null ? "" : status.toUpperCase();
        if (s.equals("ACTIVE")) {
            return new GrayColor(0.70f);
        }
        if (s.equals("LOCKED")) {
            return new GrayColor(0.80f);
        }
        return new GrayColor(0.75f);
    }
}
