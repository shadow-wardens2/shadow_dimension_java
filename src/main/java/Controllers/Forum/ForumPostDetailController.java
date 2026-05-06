package Controllers.Forum;

import Entities.Forum.Commentaire;
import Entities.Forum.Post;
import Services.Forum.CommentaireService;
import Services.Forum.PostService;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import Services.Forum.ReportService;
import Services.Forum.OpenRouterService;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class ForumPostDetailController {

    @FXML private Label lblTitle;
    @FXML private Label lblCategory;
    @FXML private Label lblAuthor;
    @FXML private Label lblVotes;
    @FXML private Label lblDate;
    @FXML private Label lblContent;
    @FXML private javafx.scene.image.ImageView postImageView;
    @FXML private Label lblCommentCount;
    @FXML private VBox commentsContainer;
    @FXML private TextArea taNewComment;
    @FXML private Button btnSubmitComment;
    @FXML private Label lblLockedMessage;
    @FXML private VBox replyBox;
    @FXML private Button btnUp;
    @FXML private Button btnDown;
    @FXML private Button btnBack;
    @FXML private Button btnReport;
    @FXML private Button btnShare;
    @FXML private Button btnAiCorrectReply;
    @FXML private Button btnGiphy;

    private final PostService postService = new PostService();
    private final CommentaireService commentaireService = new CommentaireService();
    private final ReportService reportService = new ReportService();
    private final OpenRouterService openRouterService = new OpenRouterService();
    private Post post;
    private Runnable backCallback;

    public void setPost(Post post) {
        this.post = post;
        populateHeader();
        loadComments();
    }

    public void setBackCallback(Runnable callback) {
        this.backCallback = callback;
    }

    @FXML
    public void initialize() {
        boolean loggedIn = SessionManager.isLoggedIn();
        if (replyBox != null) {
            replyBox.setVisible(loggedIn);
            replyBox.setManaged(loggedIn);
        }
        if (btnReport != null) {
            btnReport.setVisible(loggedIn);
            btnReport.setManaged(loggedIn);
        }
    }

    private void populateHeader() {
        if (post == null) return;
        
        if (post.isLocked()) {
            if (replyBox != null) {
                replyBox.setVisible(false);
                replyBox.setManaged(false);
            }
            if (lblLockedMessage != null) {
                lblLockedMessage.setVisible(true);
                lblLockedMessage.setManaged(true);
            }
        } else {
            boolean loggedIn = SessionManager.isLoggedIn();
            if (replyBox != null) {
                replyBox.setVisible(loggedIn);
                replyBox.setManaged(loggedIn);
            }
            if (lblLockedMessage != null) {
                lblLockedMessage.setVisible(false);
                lblLockedMessage.setManaged(false);
            }
        }
        
        if (lblTitle != null)    lblTitle.setText((post.isLocked() ? "🔒 " : "") + post.getTitle());
        if (lblCategory != null) {
            lblCategory.setText("✦ " + post.getCategoryName());
            lblCategory.setStyle("-fx-text-fill: " + getCategoryColor(post.getCategoryName()) + "; " +
                "-fx-background-color: " + getCategoryColor(post.getCategoryName()) + "22; " +
                "-fx-border-color: " + getCategoryColor(post.getCategoryName()) + "44; " +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 3 10;" +
                " -fx-font-weight: bold; -fx-font-size: 11px;");
        }
        if (lblAuthor != null)   lblAuthor.setText("⬡ " + (post.getAuthorName() != null ? post.getAuthorName() : "Unknown"));
        if (lblVotes != null)    lblVotes.setText(String.valueOf(post.getVotes()));
        if (lblDate != null)     lblDate.setText(post.getCreatedAt() != null ? post.getCreatedAt().toString().substring(0, 16) : "");
        if (lblContent != null)  lblContent.setText(post.getContent());
        if (lblCommentCount != null) lblCommentCount.setText(post.getCommentCount() + " Replies");

        if (postImageView != null) {
            if (post.getImageUrl() != null && !post.getImageUrl().trim().isEmpty()) {
                try {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(post.getImageUrl(), true);
                    postImageView.setImage(img);
                    postImageView.setManaged(true);
                    postImageView.setVisible(true);
                } catch (Exception e) {
                    postImageView.setManaged(false);
                    postImageView.setVisible(false);
                }
            } else {
                postImageView.setManaged(false);
                postImageView.setVisible(false);
            }
        }

        if (btnUp != null && btnDown != null) {
            styleVoteButtons(btnUp, btnDown, post.getCurrentUserVote());
        }
    }

    private void loadComments() {
        if (commentsContainer == null || post == null) return;
        commentsContainer.getChildren().clear();
        try {
            List<Commentaire> comments = commentaireService.getByPostId(post.getId());
            if (lblCommentCount != null)
                lblCommentCount.setText(comments.size() + " " + (comments.size() == 1 ? "Reply" : "Replies"));

            if (comments.isEmpty()) {
                Label none = new Label("Be the first soul to reply…");
                none.setStyle("-fx-text-fill: #5a5070; -fx-font-size: 13px; -fx-padding: 12 0;");
                commentsContainer.getChildren().add(none);
                return;
            }

            for (Commentaire c : comments) {
                commentsContainer.getChildren().add(buildCommentCard(c));
            }
        } catch (SQLException e) {
            showError("Failed to load comments: " + e.getMessage());
        }
    }

    private VBox buildCommentCard(Commentaire c) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: #1a1724; -fx-border-color: rgba(139,92,246,0.12);" +
            " -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 16;");

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label authorLbl = new Label("⬡ " + (c.getAuthorName() != null ? c.getAuthorName() : "Unknown"));
        authorLbl.setStyle("-fx-text-fill: #ba9eff; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label timeLbl = new Label("· " + (c.getCreatedAt() != null ? c.getCreatedAt().toString().substring(0, 16) : ""));
        timeLbl.setStyle("-fx-text-fill: #5a5070; -fx-font-size: 11px;");

        metaRow.getChildren().addAll(authorLbl, timeLbl);

        String rawContent = c.getContent();
        String gifUrl = null;
        if (rawContent != null && rawContent.contains("[GIF]")) {
            int start = rawContent.indexOf("[GIF]");
            int end = rawContent.indexOf("[/GIF]");
            if (start != -1 && end != -1 && end > start) {
                gifUrl = rawContent.substring(start + 5, end);
                rawContent = rawContent.substring(0, start) + rawContent.substring(end + 6);
            }
        }

        Label contentLbl = new Label(rawContent.trim());
        contentLbl.setStyle("-fx-text-fill: #e8e0eb; -fx-font-size: 13px; -fx-wrap-text: true;");
        contentLbl.setWrapText(true);

        card.getChildren().addAll(metaRow, contentLbl);

        if (gifUrl != null && !gifUrl.trim().isEmpty()) {
            try {
                javafx.scene.image.ImageView gifView = new javafx.scene.image.ImageView(new javafx.scene.image.Image(gifUrl, true));
                gifView.setFitWidth(250);
                gifView.setPreserveRatio(true);
                javafx.scene.layout.VBox gifBox = new javafx.scene.layout.VBox(gifView);
                gifBox.setStyle("-fx-padding: 8 0 0 0;");
                card.getChildren().add(gifBox);
            } catch (Exception ignored) {}
        }

        boolean isOwner = SessionManager.isLoggedIn() && SessionManager.getCurrentUser().getId() == c.getAuthorId();
        boolean isAdmin  = SessionManager.isLoggedIn() && SessionManager.getCurrentUser().isAdmin();
        if (isOwner || isAdmin) {
            HBox actionRow = new HBox(8);
            actionRow.setAlignment(Pos.CENTER_LEFT);

            Button btnEdit = new Button("Edit");
            btnEdit.getStyleClass().add("edit-button");
            btnEdit.setStyle("-fx-padding: 4 12; -fx-font-size: 11px;");
            btnEdit.setOnAction(e -> openEditComment(c));

            Button btnDel = new Button("Delete");
            btnDel.getStyleClass().add("delete-button");
            btnDel.setStyle("-fx-padding: 4 12; -fx-font-size: 11px;");
            btnDel.setOnAction(e -> deleteComment(c));

            actionRow.getChildren().addAll(btnEdit, btnDel);
            card.getChildren().add(actionRow);
        }

        return card;
    }

    @FXML
    void handleSubmitComment() {
        if (post == null || !SessionManager.isLoggedIn()) return;
        String text = taNewComment.getText() == null ? "" : taNewComment.getText().trim();
        if (text.isEmpty()) {
            showError("Comment cannot be empty.");
            return;
        }
        Commentaire c = new Commentaire();
        c.setContent(text);
        c.setPostId(post.getId());
        c.setAuthorId(SessionManager.getCurrentUser().getId());
        c.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        try {
            commentaireService.add(c);
            taNewComment.clear();
            loadComments();
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("COMMENT_BAN:")) {
                showBanAlert();
            } else if (msg != null && msg.startsWith("COMMENT_WARN:")) {
                // Comment was still posted — refresh, then warn
                taNewComment.clear();
                loadComments();
                // Parse remaining strikes from message prefix "COMMENT_WARN:N - ..."
                String warningText = msg.substring(msg.indexOf(" - ") + 3);
                showWarningAlert(warningText);
            } else {
                showError("Failed to submit: " + msg);
            }
        }
    }

    private void showBanAlert() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Commenting Revoked");
        alert.setHeaderText("🚫 You have been banned from commenting");
        alert.setContentText("Your account has been flagged for repeatedly using prohibited language.\n"
                + "Your commenting rights have been permanently revoked.\n"
                + "A warning email has been sent to your registered address.");
        alert.showAndWait();
    }

    private void showWarningAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle("Prohibited Language Warning");
        alert.setHeaderText("⚠️ Prohibited language detected");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    void handleAiCorrectReply() {
        String text = taNewComment.getText();
        if (text == null || text.trim().isEmpty()) return;

        if (btnAiCorrectReply != null) {
            btnAiCorrectReply.setDisable(true);
            btnAiCorrectReply.setText("✨ Processing...");
        }

        new Thread(() -> {
            try {
                String corrected = openRouterService.correctSpellingAndGrammar(text);
                Platform.runLater(() -> {
                    taNewComment.setText(corrected);
                    if (btnAiCorrectReply != null) {
                        btnAiCorrectReply.setDisable(false);
                        btnAiCorrectReply.setText("✨ Auto-Correct");
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (btnAiCorrectReply != null) {
                        btnAiCorrectReply.setDisable(false);
                        btnAiCorrectReply.setText("✨ Auto-Correct");
                    }
                    showError("AI Service Error: " + ex.getMessage());
                });
            }
        }).start();
    }

    @FXML
    void handleOpenGiphy() {
        if (!Utils.SessionManager.isLoggedIn()) {
            showError("You must be logged in to use Giphy.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Forum/GiphyPicker.fxml"));
            javafx.scene.Parent root = loader.load();
            GiphyPickerController controller = loader.getController();
            controller.setOnGifSelected(url -> {
                String currentText = taNewComment.getText() != null ? taNewComment.getText() : "";
                taNewComment.setText(currentText + (currentText.isEmpty() ? "" : "\n") + "[GIF]" + url + "[/GIF]");
            });

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Select a GIF");
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open Giphy picker: " + e.getMessage());
        }
    }

    @FXML
    void handleShare() {
        if (post == null || btnShare == null) return;
        
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
                Platform.runLater(() -> {
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
    void handleUpvote() {
        if (post == null) return;
        try {
            postService.upvote(post.getId());
            Post updated = postService.getById(post.getId());
            if (updated != null) {
                post.setVotes(updated.getVotes());
                post.setCurrentUserVote(updated.getCurrentUserVote());
                lblVotes.setText(String.valueOf(updated.getVotes()));
                styleVoteButtons(btnUp, btnDown, updated.getCurrentUserVote());
            }
        } catch (SQLException e) {
            showError("Upvote failed: " + e.getMessage());
        }
    }

    @FXML
    void handleDownvote() {
        if (post == null) return;
        try {
            postService.downvote(post.getId());
            Post updated = postService.getById(post.getId());
            if (updated != null) {
                post.setVotes(updated.getVotes());
                post.setCurrentUserVote(updated.getCurrentUserVote());
                lblVotes.setText(String.valueOf(updated.getVotes()));
                styleVoteButtons(btnUp, btnDown, updated.getCurrentUserVote());
            }
        } catch (SQLException e) {
            showError("Downvote failed: " + e.getMessage());
        }
    }

    private void styleVoteButtons(Button upBtn, Button downBtn, int vote) {
        if (vote == 1) {
            upBtn.setStyle("-fx-text-fill: #ff4500; -fx-background-color: rgba(255, 69, 0, 0.1); -fx-background-radius: 4;");
            downBtn.setStyle("");
        } else if (vote == -1) {
            upBtn.setStyle("");
            downBtn.setStyle("-fx-text-fill: #7193ff; -fx-background-color: rgba(113, 147, 255, 0.1); -fx-background-radius: 4;");
        } else {
            upBtn.setStyle("");
            downBtn.setStyle("");
        }
    }

    @FXML
    void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Forum/ForumFront.fxml"));
            javafx.scene.Parent root = loader.load();
            btnBack.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleReport() {
        if (post == null || !SessionManager.isLoggedIn()) return;
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
                
                // If it hits 2 reports, it's auto-hidden. Return back to feed.
                handleBack();
            } catch (SQLException ex) {
                showError("Failed to submit report: " + ex.getMessage());
            }
        });

        layout.getChildren().addAll(title, cbReason, taDescription, btnSubmit);
        Scene scene = new Scene(layout, 400, 350);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void openEditComment(Commentaire c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Forum/EditComment.fxml"));
            javafx.scene.Parent root = loader.load();
            EditCommentController controller = loader.getController();
            controller.setCommentaire(c);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Edit Reply");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadComments();
        } catch (IOException e) {
            showError("Cannot open edit form: " + e.getMessage());
        }
    }

    private void deleteComment(Commentaire c) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Reply");
        confirm.setHeaderText(null);
        confirm.setContentText("Banish this reply from the void?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    commentaireService.delete(c);
                    loadComments();
                } catch (SQLException e) {
                    showError("Delete failed: " + e.getMessage());
                }
            }
        });
    }

    private String getCategoryColor(String cat) {
        if (cat == null) return "#8b5cf6";
        switch (cat) {
            case "Gaming":      return "#4ade80";
            case "Tutorials":   return "#60a5fa";
            case "Events":      return "#f472b6";
            case "Marketplace": return "#fb923c";
            default:            return "#8b5cf6";
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
