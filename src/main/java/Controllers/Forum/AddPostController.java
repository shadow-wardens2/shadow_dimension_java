package Controllers.Forum;

import Entities.Forum.Post;
import Services.Forum.PostService;
import Utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.application.Platform;
import Services.Forum.OpenRouterService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class AddPostController {

    @FXML private TextField tfTitle;
    @FXML private TextArea taContent;
    @FXML private ComboBox<String> cbCategory;
    @FXML private TextField tfImageUrl;
    @FXML private Label lblError;
    @FXML private Button btnAiCorrect;

    private final PostService postService = new PostService();
    private final OpenRouterService openRouterService = new OpenRouterService();
    private final Map<String, Integer> categoryMap = new HashMap<>();

    @FXML
    public void initialize() {
        loadCategories();
        clearError();
    }

    private void clearError() {
        if (lblError != null) {
            lblError.setText("");
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }

    private void loadCategories() {
        try {
             Connection cnx = Utils.ShadowDimensionsDB.getInstance().getConnection();
             Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name FROM forum_category");
            while (rs.next()) {
                String name = rs.getString("name");
                int id = rs.getInt("id");
                categoryMap.put(name, id);
                cbCategory.getItems().add(name);
            }
            if (!cbCategory.getItems().isEmpty()) {
                cbCategory.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleSave() {
        clearError();
        String title   = tfTitle.getText() == null ? "" : tfTitle.getText().trim();
        String content = taContent.getText() == null ? "" : taContent.getText().trim();
        String cat     = cbCategory.getValue();
        String imgUrl  = tfImageUrl.getText() == null ? "" : tfImageUrl.getText().trim();

        if (title.isEmpty()) { showError("Title is required."); return; }
        if (title.length() < 3) { showError("Title must be at least 3 characters."); return; }
        if (title.length() > 100) { showError("Title is too long (max 100 characters)."); return; }
        
        if (content.isEmpty()) { showError("Content is required."); return; }
        if (content.length() < 10) { showError("Content must be at least 10 characters."); return; }
        if (content.length() > 5000) { showError("Content is too long (max 5000 characters)."); return; }
        
        if (cat == null) { showError("Please select a category."); return; }
        if (!SessionManager.isLoggedIn()) { showError("You must be logged in."); return; }

        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setCategoryId(categoryMap.get(cat));
        post.setCategoryName(cat);
        post.setImageUrl(imgUrl.isEmpty() ? null : imgUrl);
        post.setAuthorId(SessionManager.getCurrentUser().getId());
        post.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        try {
            postService.add(post);
            closeWindow();
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    void handleAiCorrect() {
        clearError();
        String text = taContent.getText();
        if (text == null || text.trim().isEmpty()) return;

        if (btnAiCorrect != null) {
            btnAiCorrect.setDisable(true);
            btnAiCorrect.setText("✨ Processing...");
        }

        // Run network call on background thread to prevent UI freezing
        new Thread(() -> {
            try {
                String corrected = openRouterService.correctSpellingAndGrammar(text);
                Platform.runLater(() -> {
                    taContent.setText(corrected);
                    if (btnAiCorrect != null) {
                        btnAiCorrect.setDisable(false);
                        btnAiCorrect.setText("✨ Auto-Correct");
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (btnAiCorrect != null) {
                        btnAiCorrect.setDisable(false);
                        btnAiCorrect.setText("✨ Auto-Correct");
                    }
                    showError("AI Service Error: " + ex.getMessage());
                });
            }
        }).start();
    }

    @FXML
    void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) tfTitle.getScene().getWindow();
        stage.close();
    }

    private void showError(String msg) {
        if (lblError != null) {
            lblError.setText(msg);
            lblError.setVisible(true);
            lblError.setManaged(true);
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        }
    }
}
