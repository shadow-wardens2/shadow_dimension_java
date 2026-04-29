package Controllers.Tutorials;

import Entities.Tutorials.Lecon;
import Services.Tutorials.ServiceLecon;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
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
    @FXML
    private Label lbError;

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
    private void fetchYoutubeData() {
        String url = tfVideoUrl.getText().trim();
        if (url.isEmpty()) {
            lbError.setText("Erreur: Entrez une URL YouTube d'abord.");
            return;
        }

        lbError.setText("Fetching metadata...");
        new Thread(() -> {
            java.util.Map<String, String> details = Utils.YoutubeApiManager.getVideoDetails(url);
            javafx.application.Platform.runLater(() -> {
                if (!details.isEmpty()) {
                    if (tfTitre.getText().isEmpty())
                        tfTitre.setText(details.get("title"));
                    tfVideoDuration.setText(details.get("duration"));
                    tfVideoThumbnail.setText(details.get("thumbnail"));
                    lbError.setText("Metadata fetched successfully!");
                    lbError.setStyle("-fx-text-fill: #22c55e;");
                } else {
                    lbError.setText("Erreur: Impossible de trouver la vidéo ou clé API invalide.");
                    lbError.setStyle("-fx-text-fill: #ff5555;");
                }
            });
        }).start();
    }

    @FXML
    private void sauvegarder() {
        String titre = tfTitre.getText().trim();
        String contenu = taContenu.getText().trim();
        String ordreStr = tfOrdre.getText().trim();

        if (titre.isEmpty() || contenu.isEmpty()) {
            lbError.setText("Le titre et le contenu ne peuvent pas être vides.");
            return;
        }

        int ordre;
        try {
            ordre = Integer.parseInt(ordreStr);
        } catch (NumberFormatException e) {
            lbError.setText("L'ordre doit être un nombre valide.");
            return;
        }

        try {
            boolean exists = serviceLecon.getAll().stream()
                    .anyMatch(l -> l.getTitre().equalsIgnoreCase(titre) && l.getId() != lecon.getId());
            if (exists) {
                lbError.setText("Une leçon avec ce titre existe déjà !");
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
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            lbError.setText("Impossible de mettre à jour la leçon.");
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
}
