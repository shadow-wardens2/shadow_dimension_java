package Controllers.Forum;

import Entities.Forum.Commentaire;
import Services.Forum.CommentaireService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EditCommentController {

    @FXML private TextArea taContent;
    @FXML private Label lblError;

    private final CommentaireService commentaireService = new CommentaireService();
    private Commentaire commentaire;

    @FXML
    public void initialize() {
        clearError();
    }

    private void clearError() {
        if (lblError != null) {
            lblError.setText("");
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }

    public void setCommentaire(Commentaire commentaire) {
        this.commentaire = commentaire;
        if (taContent != null) taContent.setText(commentaire.getContent());
    }

    @FXML
    void handleSave() {
        clearError();
        String text = taContent.getText() == null ? "" : taContent.getText().trim();
        if (text.isEmpty()) { showError("Reply cannot be empty."); return; }
        if (text.length() < 5) { showError("Reply must be at least 5 characters."); return; }
        if (text.length() > 2000) { showError("Reply is too long (max 2000 characters)."); return; }

        commentaire.setContent(text);
        try {
            commentaireService.update(commentaire);
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
        Stage stage = (Stage) taContent.getScene().getWindow();
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
