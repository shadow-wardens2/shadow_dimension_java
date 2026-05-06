package Controllers.Forum;

import Entities.Forum.Post;
import Services.Forum.PostService;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.Stage;
import Services.Forum.ReportService;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ForumFrontController {

    @FXML private VBox postsContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private Button btnNewPost;
    @FXML private HBox categoryBar;
    @FXML private AnchorPane rootPane;

    private final PostService postService = new PostService();
    private final ReportService reportService = new ReportService();
    private List<Post> allPosts;
    private String currentCategory = "All";
    
    private final List<String> CATEGORIES = new ArrayList<>();
    private static final String[] CHIP_COLORS = {
        "#8b5cf6", "#4ade80", "#f59e0b", "#60a5fa", "#f472b6", "#fb923c"
    };

    @FXML
    public void initialize() {
        loadCategoriesFromDb();
        buildCategoryBar();

        // Show "New Post" only when logged in
        if (btnNewPost != null) {
            boolean loggedIn = SessionManager.isLoggedIn();
            btnNewPost.setVisible(loggedIn);
            btnNewPost.setManaged(loggedIn);
        }

        loadPosts();
    }

    private void loadCategoriesFromDb() {
        CATEGORIES.clear();
        CATEGORIES.add("All");
        try {
             Connection cnx = Utils.ShadowDimensionsDB.getInstance().getConnection();
             Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM forum_category");
            while (rs.next()) {
                CATEGORIES.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void buildCategoryBar() {
        if (categoryBar == null) return;
        categoryBar.getChildren().clear();
        int i = 0;
        for (String cat : CATEGORIES) {
            final String catName = cat;
            final String color = CHIP_COLORS[i % CHIP_COLORS.length];
            Button chip = new Button(cat);
            chip.setStyle(
                "-fx-background-color: " + (catName.equals(currentCategory) ? color : "rgba(139,92,246,0.12)") + ";" +
                "-fx-text-fill: " + (catName.equals(currentCategory) ? "#050507" : "#ba9eff") + ";" +
                "-fx-background-radius: 20; -fx-padding: 6 18; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 12px;"
            );
            chip.setOnAction(e -> {
                currentCategory = catName;
                buildCategoryBar();
                filterPosts();
            });
            categoryBar.getChildren().add(chip);
            i++;
        }
    }

    private void loadPosts() {
        try {
            allPosts = postService.getAllWithCommentCount();
            filterPosts();
        } catch (SQLException e) {
            showError("Failed to load posts: " + e.getMessage());
        }
    }

    private void filterPosts() {
        if (postsContainer == null) return;
        postsContainer.getChildren().clear();

        List<Post> filtered = "All".equals(currentCategory)
            ? allPosts
            : allPosts.stream()
                .filter(p -> currentCategory.equals(p.getCategoryName()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label empty = new Label("No scrolls found in the void. Be the first to speak.");
            empty.setStyle("-fx-text-fill: #5a5070; -fx-font-size: 16px; -fx-padding: 40 0 0 0;");
            empty.setAlignment(Pos.CENTER);
            postsContainer.getChildren().add(empty);
            return;
        }

        for (Post post : filtered) {
            postsContainer.getChildren().add(buildPostCard(post));
        }
    }

    private HBox buildPostCard(Post post) {
        // (Vote box moved to the action bar below)

        // --- Content Column ---
        VBox contentBox = new VBox(8);
        contentBox.setStyle("-fx-padding: 16 16 12 12;");
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        // Category chip + meta row
        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label catChip = new Label("✦ " + post.getCategoryName());
        catChip.getStyleClass().add("category-chip");
        catChip.setStyle("-fx-background-color: " + getCategoryColor(post.getCategoryName()) +
            "22; -fx-border-color: " + getCategoryColor(post.getCategoryName()) +
            "55; -fx-border-radius: 10; -fx-background-radius: 10;" +
            " -fx-text-fill: " + getCategoryColor(post.getCategoryName()) +
            "; -fx-padding: 3 10; -fx-font-size: 11px; -fx-font-weight: bold;");

        String authorNameStr = post.getAuthorName() != null ? post.getAuthorName() : "Unknown";
        
        HBox authorBox = new HBox(6);
        authorBox.setAlignment(Pos.CENTER_LEFT);
        authorBox.setStyle("-fx-cursor: hand;");
        
        ImageView avatar = new ImageView();
        avatar.setFitWidth(24);
        avatar.setFitHeight(24);
        Circle clip = new Circle(12, 12, 12);
        avatar.setClip(clip);
        try {
            avatar.setImage(new Image("https://ui-avatars.com/api/?name=" + authorNameStr + "&background=8b5cf6&color=fff&size=64"));
        } catch (Exception e) {}
        
        Label authorLabel = new Label(authorNameStr);
        authorLabel.setStyle("-fx-text-fill: #ba9eff; -fx-font-size: 13px; -fx-font-weight: bold;");
        
        authorBox.getChildren().addAll(avatar, authorLabel);
        authorBox.setOnMouseClicked(e -> openUserProfile(post.getAuthorId(), authorNameStr));

        Label timeLabel = new Label("· " + formatRelativeTime(post.getCreatedAt()));
        timeLabel.setStyle("-fx-text-fill: #5a5070; -fx-font-size: 11px;");

        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);

        metaRow.getChildren().addAll(catChip, authorBox, timeLabel, metaSpacer);

        // Report Button
        if (SessionManager.isLoggedIn()) {
            Button btnReport = new Button("🚩 Report");
            btnReport.getStyleClass().add("ghost-button");
            btnReport.setStyle("-fx-background-color: rgba(255,69,0,0.1); -fx-text-fill: #ff4500;" +
                " -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 3 10; -fx-cursor: hand;" +
                " -fx-font-size: 11px; -fx-font-weight: bold;");
            btnReport.setOnAction(e -> openReportDialog(post));
            metaRow.getChildren().add(btnReport);
        }

        // Edit/Delete Buttons
        boolean isOwner = SessionManager.isLoggedIn() && SessionManager.getCurrentUser().getId() == post.getAuthorId();
        boolean isAdmin = SessionManager.isLoggedIn() && SessionManager.getCurrentUser().isAdmin();
        if (isOwner || isAdmin) {
            Button btnEdit = new Button("Edit");
            btnEdit.getStyleClass().add("edit-button");
            btnEdit.setStyle("-fx-padding: 3 10; -fx-font-size: 11px;");
            btnEdit.setOnAction(e -> openEditPost(post));

            Button btnDelete = new Button("Delete");
            btnDelete.getStyleClass().add("delete-button");
            btnDelete.setStyle("-fx-padding: 3 10; -fx-font-size: 11px;");
            btnDelete.setOnAction(e -> deletePost(post));

            metaRow.getChildren().addAll(btnEdit, btnDelete);
        }

        // Title
        String titleText = (post.isLocked() ? "🔒 " : "") + post.getTitle();
        Label titleLabel = new Label(titleText);
        titleLabel.setStyle("-fx-font-family: 'Cinzel', serif; -fx-font-size: 20px;" +
            " -fx-font-weight: bold; -fx-text-fill: #e8e0eb; -fx-wrap-text: true;");
        titleLabel.setWrapText(true);

        // Excerpt
        String excerpt = post.getContent().length() > 250
            ? post.getContent().substring(0, 250) + "…"
            : post.getContent();
        Label excerptLabel = new Label(excerpt);
        excerptLabel.setStyle("-fx-text-fill: #adaaae; -fx-font-size: 14px; -fx-wrap-text: true; -fx-line-spacing: 4;");
        excerptLabel.setMaxWidth(700);
        excerptLabel.setWrapText(true);

        // Image
        javafx.scene.image.ImageView imageView = null;
        VBox imageWrapper = null;
        if (post.getImageUrl() != null && !post.getImageUrl().trim().isEmpty()) {
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(post.getImageUrl(), true);
                imageView = new javafx.scene.image.ImageView(img);
                imageView.setFitWidth(500); // Fit well within the card
                imageView.setPreserveRatio(true);
                
                // Add a rounded clip or a styled wrapper for the image
                imageWrapper = new VBox(imageView);
                imageWrapper.setStyle("-fx-padding: 8 0 8 0;");
            } catch (Exception e) {
                // Invalid URL, ignore gracefully
            }
        }

        // Action bar
        HBox actionBar = new HBox(12);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.setStyle("-fx-padding: 8 0 0 0;");

        // Horizontal Vote Box (Reddit style)
        HBox voteBox = new HBox(4);
        voteBox.setAlignment(Pos.CENTER);
        voteBox.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 20; -fx-padding: 2 8;");

        Button btnUp = new Button("⇧");
        btnUp.getStyleClass().add("vote-button-up");
        Label lblVotes = new Label(String.valueOf(post.getVotes()));
        lblVotes.setStyle("-fx-text-fill: #e8e0eb; -fx-font-size: 13px; -fx-font-weight: bold;");
        Button btnDown = new Button("⇩");
        btnDown.getStyleClass().add("vote-button-down");

        styleVoteButtons(btnUp, btnDown, post.getCurrentUserVote());

        btnUp.setOnAction(e -> handleVote(post, true, btnUp, btnDown, lblVotes));
        btnDown.setOnAction(e -> handleVote(post, false, btnUp, btnDown, lblVotes));

        voteBox.getChildren().addAll(btnUp, lblVotes, btnDown);

        Button btnComment = new Button("💬 " + post.getCommentCount());
        btnComment.getStyleClass().add("ghost-button");
        btnComment.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #e8e0eb;" +
            " -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 5 14; -fx-cursor: hand;" +
            " -fx-font-size: 13px; -fx-font-weight: bold;");
        btnComment.setOnAction(e -> openPostDetail(post));

        Button btnShare = new Button("↗ Share");
        btnShare.getStyleClass().add("ghost-button");
        btnShare.setStyle("-fx-background-color: transparent; -fx-text-fill: #adaaae; -fx-cursor: hand; -fx-font-size: 13px; -fx-font-weight: bold;");
        btnShare.setOnAction(e -> handleShare(post, btnShare));

        actionBar.getChildren().addAll(voteBox, btnComment, btnShare);

        contentBox.getChildren().addAll(metaRow, titleLabel, excerptLabel);
        if (imageWrapper != null) {
            contentBox.getChildren().add(imageWrapper);
        }
        contentBox.getChildren().add(actionBar);

        // --- Card wrapper ---
        HBox card = new HBox(contentBox);
        HBox.setHgrow(contentBox, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("forum-post-card");
        card.setStyle("-fx-background-color: #0f111a; -fx-border-color: rgba(255,255,255,0.05);" +
            " -fx-border-radius: 14; -fx-background-radius: 14; -fx-cursor: hand;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #13151f;" +
            " -fx-border-color: rgba(139,92,246,0.25);" +
            " -fx-border-radius: 14; -fx-background-radius: 14; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #0f111a;" +
            " -fx-border-color: rgba(255,255,255,0.05);" +
            " -fx-border-radius: 14; -fx-background-radius: 14; -fx-cursor: hand;"));

        return card;
    }

    private void handleVote(Post post, boolean up, Button btnUp, Button btnDown, Label lblVotes) {
        try {
            if (up) postService.upvote(post.getId());
            else    postService.downvote(post.getId());

            Post updated = postService.getById(post.getId());
            if (updated != null) {
                post.setVotes(updated.getVotes());
                post.setCurrentUserVote(updated.getCurrentUserVote());
                lblVotes.setText(String.valueOf(updated.getVotes()));
                styleVoteButtons(btnUp, btnDown, updated.getCurrentUserVote());
            }
        } catch (SQLException e) {
            showError("Vote failed: " + e.getMessage());
        }
    }

    private void styleVoteButtons(Button btnUp, Button btnDown, int vote) {
        if (vote == 1) {
            btnUp.setStyle("-fx-text-fill: #ff4500; -fx-background-color: rgba(255, 69, 0, 0.1); -fx-background-radius: 4;");
            btnDown.setStyle("");
        } else if (vote == -1) {
            btnUp.setStyle("");
            btnDown.setStyle("-fx-text-fill: #7193ff; -fx-background-color: rgba(113, 147, 255, 0.1); -fx-background-radius: 4;");
        } else {
            btnUp.setStyle("");
            btnDown.setStyle("");
        }
    }

    private void openReportDialog(Post post) {
        if (!SessionManager.isLoggedIn()) return;
        int userId = SessionManager.getCurrentUser().getId();

        try {
            if (reportService.hasUserReportedPost(post.getId(), userId)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Already Reported");
                alert.setHeaderText(null);
                alert.setContentText("You have already reported this scroll. The wardens are reviewing it.");
                alert.showAndWait();
                return;
            }
        } catch (SQLException e) {
            showError("Could not verify report status.");
            return;
        }

        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Report Scroll");

        VBox layout = new VBox(15);
        layout.setStyle("-fx-background-color: #050507; -fx-padding: 24; -fx-border-color: rgba(255,69,0,0.3); -fx-border-width: 2;");
        
        Label title = new Label("Report this Scroll");
        title.setStyle("-fx-font-family: 'Cinzel', serif; -fx-font-size: 20px; -fx-text-fill: #ff4500; -fx-font-weight: bold;");

        ComboBox<String> cbReason = new ComboBox<>();
        cbReason.getItems().addAll("Harassment", "Spam", "Inappropriate Content", "Other");
        cbReason.setPromptText("Select a Reason");
        cbReason.setStyle("-fx-background-color: #0f111a; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 6;");

        TextArea taDescription = new TextArea();
        taDescription.setPromptText("Please explain why you are reporting this...");
        taDescription.setPrefRowCount(4);
        taDescription.setWrapText(true);
        taDescription.setStyle("-fx-control-inner-background: #0f111a; -fx-text-fill: #e8e0eb;");

        Button btnSubmit = new Button("Submit Report");
        btnSubmit.setStyle("-fx-background-color: #ff4500; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        btnSubmit.setOnAction(e -> {
            String reason = cbReason.getValue();
            String desc = taDescription.getText().trim();
            if (reason == null || reason.isEmpty()) {
                showError("Please select a reason.");
                return;
            }
            if (desc.isEmpty()) {
                showError("Please provide a description.");
                return;
            }
            try {
                reportService.reportPost(post.getId(), userId, reason, desc);
                dialog.close();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Report Submitted");
                alert.setHeaderText(null);
                alert.setContentText("Your report has been received. Thank you for keeping the dimension safe.");
                alert.showAndWait();
                loadPosts(); // Refresh feed in case it got auto-hidden!
            } catch (SQLException ex) {
                showError("Failed to submit report: " + ex.getMessage());
            }
        });

        layout.getChildren().addAll(title, cbReason, taDescription, btnSubmit);
        Scene scene = new Scene(layout, 400, 350);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void openPostDetail(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Forum/ForumPostDetail.fxml"));
            javafx.scene.Parent root = loader.load();
            ForumPostDetailController controller = loader.getController();
            controller.setPost(post);
            controller.setBackCallback(this::loadPosts);
            if (postsContainer != null && postsContainer.getScene() != null) {
                postsContainer.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Cannot open post: " + e.getMessage());
        }
    }

    private void openUserProfile(int userId, String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Forum/UserProfile.fxml"));
            javafx.scene.Parent root = loader.load();
            UserProfileController controller = loader.getController();
            controller.setProfileUser(userId, username);
            controller.setBackCallback(() -> {
                if (rootPane != null && rootPane.getScene() != null) {
                    try {
                        FXMLLoader frontLoader = new FXMLLoader(getClass().getResource("/Forum/ForumFront.fxml"));
                        rootPane.getScene().setRoot(frontLoader.load());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            if (rootPane != null && rootPane.getScene() != null) {
                rootPane.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Cannot open user profile: " + e.getMessage());
        }
    }

    @FXML
    void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HomeFront.fxml"));
            javafx.scene.Parent root = loader.load();
            if (rootPane != null && rootPane.getScene() != null) {
                rootPane.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Cannot navigate back: " + e.getMessage());
        }
    }

    private void handleShare(Post post, Button btnShare) {
        if (!Utils.SessionManager.isLoggedIn()) {
            showError("You must be logged in to share a scroll.");
            return;
        }
        
        try {
            postService.sharePost(post.getId(), Utils.SessionManager.getCurrentUser().getId());
            String originalText = btnShare.getText();
            btnShare.setText("✓ Shared to Profile");
            btnShare.setStyle("-fx-background-color: rgba(139,92,246,0.15); -fx-text-fill: #ba9eff; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 5 14; -fx-cursor: hand; -fx-font-size: 13px; -fx-font-weight: bold;");
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {}
                javafx.application.Platform.runLater(() -> {
                    btnShare.setText(originalText);
                    btnShare.setStyle("-fx-background-color: transparent; -fx-text-fill: #adaaae; -fx-cursor: hand; -fx-font-size: 13px; -fx-font-weight: bold;");
                });
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not share post: " + e.getMessage());
        }
    }

    @FXML
    void handleNewPost() {
        if (!SessionManager.isLoggedIn()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Forum/AddPost.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("New Scroll");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadPosts();
        } catch (IOException e) {
            showError("Cannot open form: " + e.getMessage());
        }
    }

    private void openEditPost(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Forum/EditPost.fxml"));
            javafx.scene.Parent root = loader.load();
            EditPostController controller = loader.getController();
            controller.setPost(post);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Edit Scroll");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadPosts();
        } catch (IOException e) {
            showError("Cannot open edit form: " + e.getMessage());
        }
    }

    private void deletePost(Post post) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Scroll");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to banish this scroll to the void?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    postService.delete(post);
                    loadPosts();
                } catch (SQLException e) {
                    showError("Delete failed: " + e.getMessage());
                }
            }
        });
    }

    private String getCategoryColor(String category) {
        if (category == null) return "#8b5cf6";
        switch (category) {
            case "Gaming":      return "#4ade80";
            case "Tutorials":   return "#60a5fa";
            case "Events":      return "#f472b6";
            case "Marketplace": return "#fb923c";
            default:            return "#8b5cf6";
        }
    }

    private String formatRelativeTime(Timestamp ts) {
        if (ts == null) return "";
        long diff = System.currentTimeMillis() - ts.getTime();
        long mins = diff / 60000;
        if (mins < 1) return "just now";
        if (mins < 60) return mins + "m ago";
        long hrs = mins / 60;
        if (hrs < 24) return hrs + "h ago";
        long days = hrs / 24;
        return days + "d ago";
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
