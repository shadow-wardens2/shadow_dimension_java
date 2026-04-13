package Controllers.Tutorials;

import Entities.Tutorials.Jeu;
import Services.Tutorials.ServiceJeu;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AjouterJeuController {

    @FXML
    private TextField tfNom;

    @FXML
    private TextField tfGenre;

    private ServiceJeu serviceJeu;

    public AjouterJeuController() {
        serviceJeu = new ServiceJeu();
    }

    @FXML
    private void ajouterJeu() {
        String nom = tfNom.getText().trim();
        String genre = tfGenre.getText() != null ? tfGenre.getText().trim() : "";

        if (nom.isEmpty() || genre.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Tous les champs sont obligatoires !");
            return;
        }

        try {
            boolean exists = serviceJeu.getAll().stream()
                    .anyMatch(j -> j.getNom().equalsIgnoreCase(nom));
            if (exists) {
                showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Un jeu avec ce nom existe déjà !");
                return;
            }

            Jeu jeu = new Jeu(0, nom, genre);
            serviceJeu.add(jeu);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Le jeu a été ajouté avec succès !");
            fermerFenetre();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter le jeu.\n" + e.getMessage());
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

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
