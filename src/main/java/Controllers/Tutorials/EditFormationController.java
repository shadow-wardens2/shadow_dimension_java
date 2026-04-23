package Controllers.Tutorials;

import Entities.Tutorials.Formation;
import Entities.Tutorials.Jeu;
import Services.Tutorials.ServiceFormation;
import Services.Tutorials.ServiceJeu;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class EditFormationController {

    @FXML
    private TextField tfTitre;

    @FXML
    private TextField tfDescription;

    @FXML
    private ComboBox<String> cbNiveau;

    @FXML
    private ComboBox<Jeu> cbJeu;

    @FXML
    private TextField tfImage;

    @FXML
    private Label lbError;

    private ServiceFormation serviceFormation;
    private ServiceJeu serviceJeu;
    private Formation targetFormation;

    public EditFormationController() {
        serviceFormation = new ServiceFormation();
        serviceJeu = new ServiceJeu();
    }

    @FXML
    public void initialize() {
        cbNiveau.setItems(FXCollections.observableArrayList("debutant", "intermediaire", "avance"));
        lbError.setText("");

        try {
            List<Jeu> jeux = serviceJeu.getAll();
            cbJeu.setItems(FXCollections.observableArrayList(jeux));
        } catch (SQLException e) {
            e.printStackTrace();
            lbError.setText("Erreur: Impossible de charger les jeux.");
        }
    }

    public void setFormation(Formation f) {
        this.targetFormation = f;
        if (f != null) {
            tfTitre.setText(f.getTitre());
            tfDescription.setText(f.getDescription());
            cbNiveau.getSelectionModel().select(f.getNiveau());
            cbJeu.getSelectionModel().select(f.getJeu());
            tfImage.setText(f.getImage());
        }
    }

    @FXML
    private void sauvegarder() {
        String titre = tfTitre.getText().trim();
        String description = tfDescription.getText().trim();
        String niveau = cbNiveau.getValue();

        if (titre.isEmpty() || niveau == null || jeu == null) {
            lbError.setText("Erreur: Titre, Niveau et Jeu sont obligatoires !");
            return;
        }

        try {
            boolean exists = serviceFormation.getAll().stream()
                    .anyMatch(f -> f.getTitre().equalsIgnoreCase(titre) && f.getId() != formation.getId());
            if (exists) {
                showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Une formation avec ce titre existe déjà !");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        formation.setTitre(titre);
        formation.setDescription(description);
        formation.setNiveau(niveau);
        formation.setImage(tfImage.getText().trim());

        try {
            serviceFormation.update(formation);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Formation mise à jour avec succès !");
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            lbError.setText("Erreur: Impossible de mettre à jour la formation.");
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
