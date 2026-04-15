package Controllers.Tutorials;

import Entities.Tutorials.Jeu;
import Services.Tutorials.ServiceJeu;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EditJeuController {

    @FXML
    private TextField tfNom;
    @FXML
    private TextField tfGenre;

    private ServiceJeu serviceJeu = new ServiceJeu();
    private Jeu jeu;

    public void setJeu(Jeu jeu) {
        this.jeu = jeu;
        tfNom.setText(jeu.getNom());
        tfGenre.setText(jeu.getGenre());
    }

    @FXML
    private void sauvegarder() {
        String nom = tfNom.getText().trim();
        String genre = tfGenre.getText().trim();

        if (nom.isEmpty() || genre.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Tous les champs sont obligatoires.");
            return;
        }

        try {
            boolean exists = serviceJeu.getAll().stream()
                    .anyMatch(j -> j.getNom().equalsIgnoreCase(nom) && j.getId() != jeu.getId());
            if (exists) {
                showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Un jeu avec ce nom existe déjà !");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        jeu.setNom(nom);
        jeu.setGenre(genre);

        try {
            serviceJeu.update(jeu);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Jeu mis à jour avec succès !");
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour le jeu.");
        }
    }

    @FXML
    private void annuler() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) tfNom.getScene().getWindow();
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
