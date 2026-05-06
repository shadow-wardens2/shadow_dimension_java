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
        if (lblError != null) lblError.setVisible(false);
    }

    public void setCommentaire(Commentaire commentaire) {
        this.commentaire = commentaire;
        if (taContent != null) taContent.setText(commentaire.getContent());
    }

    @FXML
    void handleSave() {
        String text = taContent.getText() == null ? "" : taContent.getText().trim();
        if (text.isEmpty()) { showError("Reply cannot be empty."); return; }

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
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        }
    }
}
