package Controllers.Forum;

import Entities.Forum.Post;
import Services.Forum.PostService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class EditPostController {

    @FXML private TextField tfTitle;
    @FXML private TextArea taContent;
    @FXML private ComboBox<String> cbCategory;
    @FXML private TextField tfImageUrl;
    @FXML private Label lblError;

    private final PostService postService = new PostService();
    private final Map<String, Integer> categoryMap = new HashMap<>();
    private Post post;

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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setPost(Post post) {
        this.post = post;
        tfTitle.setText(post.getTitle());
        taContent.setText(post.getContent());
        cbCategory.setValue(post.getCategoryName());
        tfImageUrl.setText(post.getImageUrl() != null ? post.getImageUrl() : "");
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

        post.setTitle(title);
        post.setContent(content);
        post.setCategoryId(categoryMap.get(cat));
        post.setCategoryName(cat);
        post.setImageUrl(imgUrl.isEmpty() ? null : imgUrl);

        try {
            postService.update(post);
            closeWindow();
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
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
