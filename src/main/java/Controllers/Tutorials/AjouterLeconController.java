package Controllers.Tutorials;

import Entities.Tutorials.Formation;
import Entities.Tutorials.Lecon;
import Services.Tutorials.ServiceFormation;
import Services.Tutorials.ServiceLecon;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class AjouterLeconController {

    @FXML
    private TextField tfTitre;

    @FXML
    private TextArea taContenu;

    @FXML
    private TextField tfOrdre;

    @FXML
    private ComboBox<Formation> cbFormation;

    @FXML
    private TextField tfImage;

    @FXML
    private TextField tfVideoUrl;

    @FXML
    private TextField tfDocumentUrl;

    @FXML
    private TextField tfVideoDuration;

    @FXML
    private TextField tfVideoThumbnail;

    @FXML
    private Label lbError;

    private ServiceLecon serviceLecon;
    private ServiceFormation serviceFormation;

    public AjouterLeconController() {
        serviceLecon = new ServiceLecon();
        serviceFormation = new ServiceFormation();
    }

    @FXML
    public void initialize() {
        lbError.setText("");
        try {
            List<Formation> formations = serviceFormation.getAll();
            cbFormation.setItems(FXCollections.observableArrayList(formations));
        } catch (SQLException e) {
            e.printStackTrace();
            lbError.setText("Erreur: Impossible de charger les formations.");
        }
    }

    @FXML
    private void ajouterLecon() {
        String titre = tfTitre.getText().trim();
        String contenu = taContenu.getText().trim();
        Formation formation = cbFormation.getSelectionModel().getSelectedItem();

        String ordreStr = tfOrdre.getText().trim();
        int ordre;
        try {
            ordre = Integer.parseInt(ordreStr);
        } catch (NumberFormatException ex) {
            lbError.setText("Erreur: L'ordre doit être un nombre valide.");
            return;
        }

        if (titre.isEmpty() || contenu.isEmpty() || formation == null) {
            lbError.setText("Erreur: Titre, Contenu, Ordre et Formation requis !");
            return;
        }

        try {
            boolean exists = serviceLecon.getAll().stream()
                    .anyMatch(l -> l.getTitre().equalsIgnoreCase(titre));
            if (exists) {
                lbError.setText("Erreur: Une leçon avec ce titre existe déjà !");
                return;
            }

            Lecon lecon = new Lecon(
                    0, titre, contenu, ordre, formation,
                    tfImage.getText().trim(),
                    tfVideoUrl.getText().trim(),
                    tfDocumentUrl.getText().trim(),
                    tfVideoDuration.getText().trim(),
                    tfVideoThumbnail.getText().trim());
            serviceLecon.add(lecon);

            fermerFenetre();
        } catch (Exception e) {
            e.printStackTrace();
            lbError.setText("Erreur: Impossible d'ajouter la leçon.");
        }
    }

    @FXML
    private void annuler() {
        fermerFenetre();
    }

    private void fermerFenetre() {
        Stage stage = (Stage) tfTitre.getScene().getWindow();
        stage.close();
    }
}
