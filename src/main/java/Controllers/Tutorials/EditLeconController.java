package Controllers.Tutorials;

import Entities.Tutorials.Lecon;
import Services.Tutorials.ServiceLecon;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EditLeconController {

    @FXML
    private TextField tfTitre;
    @FXML
    private TextArea taContenu;
    @FXML
    private TextField tfOrdre;
    @FXML
    private TextField tfImage;
    @FXML
    private TextField tfVideoUrl;
    @FXML
    private TextField tfVideoDuration;
    @FXML
    private TextField tfDocumentUrl;
    @FXML
    private TextField tfVideoThumbnail;

    private ServiceLecon serviceLecon = new ServiceLecon();
    private Lecon lecon;

    public void setLecon(Lecon lecon) {
        this.lecon = lecon;
        tfTitre.setText(lecon.getTitre());
        taContenu.setText(lecon.getContenu());
        tfOrdre.setText(String.valueOf(lecon.getOrdre()));
        tfImage.setText(lecon.getImage());
        tfVideoUrl.setText(lecon.getVideoUrl());
        tfVideoDuration.setText(lecon.getVideoDuration());
        tfDocumentUrl.setText(lecon.getDocumentUrl());
        tfVideoThumbnail.setText(lecon.getVideoThumbnail());
    }

    @FXML
    private void sauvegarder() {
        String titre = tfTitre.getText().trim();
        String contenu = taContenu.getText().trim();
        String ordreStr = tfOrdre.getText().trim();

        if (titre.isEmpty() || contenu.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le titre et le contenu ne peuvent pas être vides.");
            return;
        }

        int ordre;
        try {
            ordre = Integer.parseInt(ordreStr);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "L'ordre doit être un nombre valide.");
            return;
        }

        try {
            boolean exists = serviceLecon.getAll().stream()
                    .anyMatch(l -> l.getTitre().equalsIgnoreCase(titre) && l.getId() != lecon.getId());
            if (exists) {
                showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Une leçon avec ce titre existe déjà !");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        lecon.setOrdre(ordre);
        lecon.setTitre(titre);
        lecon.setContenu(contenu);
        lecon.setImage(tfImage.getText().trim());
        lecon.setVideoUrl(tfVideoUrl.getText().trim());
        lecon.setVideoDuration(tfVideoDuration.getText().trim());
        lecon.setDocumentUrl(tfDocumentUrl.getText().trim());
        lecon.setVideoThumbnail(tfVideoThumbnail.getText().trim());

        try {
            serviceLecon.update(lecon);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Leçon mise à jour avec succès !");
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour la leçon.");
        }
    }

    @FXML
    private void annuler() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) tfTitre.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
