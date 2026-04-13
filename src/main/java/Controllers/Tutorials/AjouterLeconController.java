package Controllers.Tutorials;

import Entities.Tutorials.Formation;
import Entities.Tutorials.Lecon;
import Services.Tutorials.ServiceFormation;
import Services.Tutorials.ServiceLecon;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
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

    private ServiceLecon serviceLecon;
    private ServiceFormation serviceFormation;

    public AjouterLeconController() {
        serviceLecon = new ServiceLecon();
        serviceFormation = new ServiceFormation();
    }

    @FXML
    public void initialize() {
        try {
            List<Formation> formations = serviceFormation.getAll();
            cbFormation.setItems(FXCollections.observableArrayList(formations));
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la liste des formations.");
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
            showAlert(Alert.AlertType.ERROR, "Erreur", "L'ordre doit être un nombre valide.");
            return;
        }

        if (titre.isEmpty() || contenu.isEmpty() || formation == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation",
                    "Le titre, le contenu, l'ordre et la formation sont obligatoires !");
            return;
        }

        try {
            boolean exists = serviceLecon.getAll().stream()
                    .anyMatch(l -> l.getTitre().equalsIgnoreCase(titre));
            if (exists) {
                showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Une leçon avec ce titre existe déjà !");
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

            showAlert(Alert.AlertType.INFORMATION, "Succès", "La leçon a été ajoutée avec succès !");
            fermerFenetre();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter la leçon.");
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

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
