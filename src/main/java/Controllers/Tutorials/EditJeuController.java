package Controllers.Tutorials;

import Entities.Tutorials.Jeu;
import Services.Tutorials.ServiceJeu;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class EditJeuController {

    @FXML
    private TextField tfNom;

    @FXML
    private TextField tfGenre;

    @FXML
    private Label lbError;

    private ServiceJeu serviceJeu;
    private Jeu jeu;

    public EditJeuController() {
        serviceJeu = new ServiceJeu();
    }

    public void setJeu(Jeu jeu) {
        this.jeu = jeu;
        if (jeu != null) {
            tfNom.setText(jeu.getNom());
            tfGenre.setText(jeu.getGenre());
        }
    }

    @FXML
    private void sauvegarder() {
        String nom = tfNom.getText().trim();
        String genre = tfGenre.getText() != null ? tfGenre.getText().trim() : "";

        if (nom.isEmpty() || genre.isEmpty()) {
            lbError.setText("Erreur: Tous les champs sont obligatoires !");
            return;
        }

        try {
            jeu.setNom(nom);
            jeu.setGenre(genre);
            serviceJeu.update(jeu);
            fermerFenetre();
        } catch (Exception e) {
            e.printStackTrace();
            lbError.setText("Erreur: Impossible de mettre à jour le jeu.");
        }
    }

    @FXML
    private void annuler() {
        fermerFenetre();
    }

    private void fermerFenetre() {
        Stage stage = (Stage) tfNom.getScene().getWindow();
        stage.close();
    }
}
