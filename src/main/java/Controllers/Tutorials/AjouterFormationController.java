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

public class AjouterFormationController {

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

    public AjouterFormationController() {
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

    @FXML
    private void ajouterFormation() {
        String titre = tfTitre.getText().trim();
        String description = tfDescription.getText() != null ? tfDescription.getText().trim() : "";
        String niveau = cbNiveau.getSelectionModel().getSelectedItem();
        Jeu jeu = cbJeu.getSelectionModel().getSelectedItem();
        String image = tfImage.getText() != null ? tfImage.getText().trim() : "";

        if (titre.isEmpty() || niveau == null || jeu == null) {
            lbError.setText("Erreur: Le titre, le niveau et le jeu sont obligatoires !");
            return;
        }

        try {
            boolean exists = serviceFormation.getAll().stream()
                    .anyMatch(f -> f.getTitre().equalsIgnoreCase(titre));
            if (exists) {
                lbError.setText("Erreur: Une formation avec ce titre existe déjà !");
                return;
            }

            Formation formation = new Formation(0, titre, description, niveau, jeu, image);
            serviceFormation.add(formation);
            fermerFenetre();
        } catch (Exception e) {
            e.printStackTrace();
            lbError.setText("Erreur: Impossible d'ajouter la formation.");
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
