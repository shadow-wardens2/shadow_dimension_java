package Controllers.Forum;

import Entities.Forum.BannedUser;
import Entities.Forum.Post;
import Services.Forum.PostService;
import Utils.ShadowDimensionsDB;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Locale;
import java.util.ResourceBundle;

public class ForumBackController implements Initializable {

    // ── Posts tab ──
    @FXML private TableView<Post>         postsTable;
    @FXML private TableColumn<Post, Integer>   colId;
    @FXML private TableColumn<Post, String>    colTitle;
    @FXML private TableColumn<Post, String>    colCategory;
    @FXML private TableColumn<Post, String>    colAuthor;
    @FXML private TableColumn<Post, Integer>   colVotes;
    @FXML private TableColumn<Post, Integer>   colComments;
    @FXML private TableColumn<Post, Boolean>   colHidden;
    @FXML private TableColumn<Post, Boolean>   colLocked;
    @FXML private TableColumn<Post, Timestamp> colDate;
    @FXML private TableColumn<Post, Integer>   colActions;
    @FXML private TextField  tfSearch;
    @FXML private ComboBox<String> cbSort;

    // ── Banned tab ──
    @FXML private TableView<BannedUser>             bannedTable;
    @FXML private TableColumn<BannedUser, Integer>  colBanId;
    @FXML private TableColumn<BannedUser, String>   colBanUsername;
    @FXML private TableColumn<BannedUser, String>   colBanEmail;
    @FXML private TableColumn<BannedUser, Integer>  colBanStrikes;
    @FXML private TableColumn<BannedUser, Integer>  colBanAction;

    private final PostService postService = new PostService();
    private final ObservableList<Post> observablePosts   = FXCollections.observableArrayList();
    private final ObservableList<BannedUser> bannedUsers = FXCollections.observableArrayList();
    private FilteredList<Post> filteredPosts;
    private SortedList<Post>   sortedPosts;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupPostsTab();
        setupBannedTab();
        loadPosts();
        loadBannedUsers();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Posts Tab
    // ─────────────────────────────────────────────────────────────────

    private void setupPostsTab() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colAuthor.setCellValueFactory(new PropertyValueFactory<>("authorName"));
        colVotes.setCellValueFactory(new PropertyValueFactory<>("votes"));
        colComments.setCellValueFactory(new PropertyValueFactory<>("commentCount"));
        colHidden.setCellValueFactory(new PropertyValueFactory<>("hidden"));
        colLocked.setCellValueFactory(new PropertyValueFactory<>("locked"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        filteredPosts = new FilteredList<>(observablePosts, p -> true);
        sortedPosts   = new SortedList<>(filteredPosts);
        postsTable.setItems(sortedPosts);

        cbSort.setItems(FXCollections.observableArrayList(
            "Newest first", "Oldest first", "Most votes", "Least votes",
            "Most comments", "Title A-Z"
        ));
        cbSort.getSelectionModel().selectFirst();

        tfSearch.textProperty().addListener((obs, o, n) -> applyFilter());
        cbSort.valueProperty().addListener((obs, o, n) -> applyFilter());
        applyFilter();

        colActions.setCellFactory(param -> new TableCell<Post, Integer>() {
            private final Button btnEdit   = new Button("✎");
            private final Button btnDelete = new Button("🗑");
            private final Button btnHide   = new Button();
            private final Button btnLock   = new Button();
            private final HBox box = new HBox(4, btnEdit, btnHide, btnLock, btnDelete);

            {
                btnEdit.getStyleClass().add("edit-button");
                btnDelete.getStyleClass().add("delete-button");
                btnHide.getStyleClass().add("ghost-button");
                btnHide.setStyle("-fx-font-size: 10px; -fx-padding: 3 6;");
                btnLock.getStyleClass().add("ghost-button");
                btnLock.setStyle("-fx-font-size: 10px; -fx-padding: 3 6;");

                btnEdit.setOnAction(e -> openEditDialog(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
                btnHide.setOnAction(e -> {
                    Post p = getTableView().getItems().get(getIndex());
                    try { postService.toggleHidden(p.getId(), !p.isHidden()); loadPosts(); }
                    catch (SQLException ex) { showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage()); }
                });
                btnLock.setOnAction(e -> {
                    Post p = getTableView().getItems().get(getIndex());
                    try { postService.toggleLocked(p.getId(), !p.isLocked()); loadPosts(); }
                    catch (SQLException ex) { showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage()); }
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Post p = getTableView().getItems().get(getIndex());
                if (p != null) {
                    btnHide.setText(p.isHidden() ? "Unhide" : "Hide");
                    btnLock.setText(p.isLocked() ? "Unlock" : "Lock");
                }
                setGraphic(box);
            }
        });
    }

    private void applyFilter() {
        String search = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase(Locale.ROOT);

        filteredPosts.setPredicate(p -> {
            if (search.isEmpty()) return true;
            return safe(p.getTitle()).contains(search)
                || safe(p.getCategoryName()).contains(search)
                || safe(p.getAuthorName()).contains(search)
                || safe(p.getContent()).contains(search);
        });

        Comparator<Post> cmp;
        switch (cbSort.getValue() == null ? "" : cbSort.getValue()) {
            case "Oldest first":   cmp = Comparator.comparingInt(Post::getId); break;
            case "Most votes":     cmp = Comparator.comparingInt(Post::getVotes).reversed(); break;
            case "Least votes":    cmp = Comparator.comparingInt(Post::getVotes); break;
            case "Most comments":  cmp = Comparator.comparingInt(Post::getCommentCount).reversed(); break;
            case "Title A-Z":      cmp = Comparator.comparing(p -> safe(p.getTitle())); break;
            default:               cmp = Comparator.comparingInt(Post::getId).reversed();
        }
        sortedPosts.setComparator(cmp);
    }

    private void loadPosts() {
        observablePosts.clear();
        try { observablePosts.addAll(postService.getAllWithCommentCount()); applyFilter(); }
        catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Banned Users Tab
    // ─────────────────────────────────────────────────────────────────

    private void setupBannedTab() {
        colBanId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colBanUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colBanEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colBanStrikes.setCellValueFactory(new PropertyValueFactory<>("strikes"));

        bannedTable.setItems(bannedUsers);

        // Unban action column
        colBanAction.setCellFactory(param -> new TableCell<BannedUser, Integer>() {
            private final Button btnUnban = new Button("✔ Unban");

            {
                btnUnban.setStyle(
                    "-fx-background-color: rgba(74,222,128,0.15); " +
                    "-fx-text-fill: #4ade80; " +
                    "-fx-border-color: #4ade80; " +
                    "-fx-border-radius: 8; -fx-background-radius: 8; " +
                    "-fx-padding: 4 12; -fx-cursor: hand; -fx-font-weight: bold;"
                );
                btnUnban.setOnAction(e -> {
                    BannedUser user = getTableView().getItems().get(getIndex());
                    handleUnban(user);
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnUnban);
            }
        });
    }

    private void loadBannedUsers() {
        bannedUsers.clear();
        Connection cnx = ShadowDimensionsDB.getInstance().getConnection();
        String sql = "SELECT id, username, email, bad_comment_count " +
                     "FROM user WHERE is_locked = 1 AND bad_comment_count >= 3 " +
                     "ORDER BY bad_comment_count DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                bannedUsers.add(new BannedUser(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getInt("bad_comment_count")
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error loading banned users", e.getMessage());
        }
    }

    private void handleUnban(BannedUser user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Unban User");
        confirm.setHeaderText(null);
        confirm.setContentText("Restore commenting rights for \"" + user.getUsername() + "\"?\n"
                + "Their strike count will also be reset to 0.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                Connection cnx = ShadowDimensionsDB.getInstance().getConnection();
                try (PreparedStatement ps = cnx.prepareStatement(
                        "UPDATE user SET is_locked = 0, bad_comment_count = 0 WHERE id = ?")) {
                    ps.setInt(1, user.getId());
                    ps.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "User Unbanned",
                            user.getUsername() + " can now post comments again.");
                    loadBannedUsers();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
                }
            }
        });
    }

    @FXML
    void handleRefreshBanned() {
        loadBannedUsers();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Shared Helpers
    // ─────────────────────────────────────────────────────────────────

    @FXML
    void handleAddPost() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Forum/AddPost.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("New Scroll");
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadPosts();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    private void openEditDialog(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Forum/EditPost.fxml"));
            Parent root = loader.load();
            EditPostController ctrl = loader.getController();
            ctrl.setPost(post);
            Stage stage = new Stage();
            stage.setTitle("Edit Scroll");
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadPosts();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    private void handleDelete(Post post) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Scroll");
        confirm.setHeaderText(null);
        confirm.setContentText("Permanently delete \"" + post.getTitle() + "\" and all its replies?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try { postService.delete(post); loadPosts(); }
                catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
            }
        });
    }

    private String safe(String v) { return v == null ? "" : v.toLowerCase(Locale.ROOT); }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
