package Controllers.Forum;

import Entities.Forum.Post;
import Services.Forum.PostService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class UserProfileController {

    @FXML private ImageView imgAvatar;
    @FXML private Label lblUsername;
    @FXML private Label lblPostCount;
    @FXML private VBox postsContainer;
    @FXML private Button btnBack;

    private final PostService postService = new PostService();
    private int profileUserId;
    private String profileUsername;
    private Runnable backCallback;

    public void setProfileUser(int userId, String username) {
        this.profileUserId = userId;
        this.profileUsername = username;
        lblUsername.setText(username != null ? username : "Unknown Warden");
        
        // Load Avatar
        String avatarUrl = "https://ui-avatars.com/api/?name=" + (username != null ? username : "U") + "&background=8b5cf6&color=fff&size=256";
        try {
            imgAvatar.setImage(new Image(avatarUrl, true));
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadUserPosts();
    }

    public void setBackCallback(Runnable callback) {
        this.backCallback = callback;
    }

    private void loadUserPosts() {
        if (postsContainer == null) return;
        postsContainer.getChildren().clear();

        try {
            List<Post> posts = postService.getPostsByUserId(profileUserId);
            lblPostCount.setText(posts.size() + (posts.size() == 1 ? " Scroll Inscribed" : " Scrolls Inscribed"));

            if (posts.isEmpty()) {
                Label emptyLbl = new Label("This warden has yet to inscribe any scrolls.");
                emptyLbl.setStyle("-fx-text-fill: #5a5070; -fx-font-style: italic; -fx-padding: 20;");
                postsContainer.getChildren().add(emptyLbl);
                return;
            }

            for (Post p : posts) {
                postsContainer.getChildren().add(buildProfilePostCard(p));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label err = new Label("Failed to load scrolls from the void.");
            err.setStyle("-fx-text-fill: #ff4500;");
            postsContainer.getChildren().add(err);
        }
    }

    private HBox buildProfilePostCard(Post post) {
        VBox contentBox = new VBox(8);
        contentBox.setStyle("-fx-padding: 20 24;");

        // Meta (Category & Date)
        HBox metaRow = new HBox(12);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        
        // Add Shared Indicator if author is different from profile user
        if (post.getAuthorId() != profileUserId) {
            Label sharedLabel = new Label("↗ Shared");
            sharedLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: rgba(245,158,11,0.15); -fx-padding: 3 8; -fx-border-radius: 8; -fx-background-radius: 8;");
            metaRow.getChildren().add(sharedLabel);
        }
        
        Label catLabel = new Label(post.getCategoryName());
        catLabel.setStyle("-fx-background-color: rgba(139,92,246,0.15); -fx-text-fill: #8b5cf6;" +
            " -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 3 10; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label dateLabel = new Label("· " + formatRelativeTime(post.getCreatedAt()));
        dateLabel.setStyle("-fx-text-fill: #5a5070; -fx-font-size: 12px;");
        metaRow.getChildren().addAll(catLabel, dateLabel);

        // Title
        Label titleLabel = new Label(post.getTitle());
        titleLabel.setStyle("-fx-font-family: 'Cinzel', serif; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e8e0eb; -fx-wrap-text: true;");
        titleLabel.setWrapText(true);

        // Excerpt
        String excerptText = post.getContent();
        if (excerptText != null && excerptText.length() > 150) {
            excerptText = excerptText.substring(0, 150) + "...";
        }
        Label excerptLabel = new Label(excerptText);
        excerptLabel.setStyle("-fx-text-fill: #adaaae; -fx-font-size: 13px; -fx-wrap-text: true; -fx-line-spacing: 4;");
        excerptLabel.setWrapText(true);

        // Stats Box
        HBox statsBox = new HBox(16);
        statsBox.setAlignment(Pos.CENTER_LEFT);
        statsBox.setStyle("-fx-padding: 10 0 0 0;");
        Label votesLbl = new Label("⇧ " + post.getVotes());
        votesLbl.setStyle("-fx-text-fill: #7193ff; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label commentsLbl = new Label("💬 " + post.getCommentCount());
        commentsLbl.setStyle("-fx-text-fill: #adaaae; -fx-font-weight: bold; -fx-font-size: 13px;");
        statsBox.getChildren().addAll(votesLbl, commentsLbl);

        contentBox.getChildren().addAll(metaRow, titleLabel, excerptLabel, statsBox);

        HBox card = new HBox(contentBox);
        HBox.setHgrow(contentBox, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color: #0f111a; -fx-border-color: rgba(255,255,255,0.05);" +
            " -fx-border-radius: 14; -fx-background-radius: 14; -fx-cursor: hand;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #13151f; -fx-border-color: rgba(139,92,246,0.25); -fx-border-radius: 14; -fx-background-radius: 14; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #0f111a; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 14; -fx-background-radius: 14; -fx-cursor: hand;"));
        
        card.setOnMouseClicked(e -> openPostDetail(post));

        return card;
    }

    private void openPostDetail(Post post) {
        try {
            Stage currentStage = (Stage) btnBack.getScene().getWindow();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Forum/ForumPostDetail.fxml"));
            javafx.scene.Parent root = loader.load();
            ForumPostDetailController controller = loader.getController();
            controller.setPost(post);
            controller.setBackCallback(() -> {
                try {
                    FXMLLoader pLoader = new FXMLLoader(getClass().getResource("/Forum/UserProfile.fxml"));
                    javafx.scene.Parent pRoot = pLoader.load();
                    UserProfileController pController = pLoader.getController();
                    pController.setProfileUser(profileUserId, profileUsername);
                    currentStage.getScene().setRoot(pRoot);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            currentStage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Forum/ForumFront.fxml"));
            javafx.scene.Parent root = loader.load();
            if (btnBack != null && btnBack.getScene() != null) {
                btnBack.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
}
