package Controllers.Tutorials;

import Entities.Tutorials.Jeu;
import Services.Tutorials.ServiceJeu;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AjouterJeuController {

    @FXML
    private TextField tfNom;

    @FXML
    private TextField tfGenre;

    @FXML
    private Label lbError;

    private ServiceJeu serviceJeu;

    public AjouterJeuController() {
        serviceJeu = new ServiceJeu();
    }

    @FXML
    private void ajouterJeu() {
        String nom = tfNom.getText().trim();
        String genre = tfGenre.getText() != null ? tfGenre.getText().trim() : "";

        if (nom.isEmpty() || genre.isEmpty()) {
            lbError.setText("Erreur: Tous les champs sont obligatoires !");
            return;
        }

        try {
            boolean exists = serviceJeu.getAll().stream()
                    .anyMatch(j -> j.getNom().equalsIgnoreCase(nom));
            if (exists) {
                lbError.setText("Erreur: Un jeu avec ce nom existe déjà !");
                return;
            }

            Jeu jeu = new Jeu(0, nom, genre);
            serviceJeu.add(jeu);

            fermerFenetre();
        } catch (Exception e) {
            e.printStackTrace();
            lbError.setText("Erreur: Impossible d'ajouter le jeu.");
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
