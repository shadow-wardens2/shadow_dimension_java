package Controllers.Tutorials;

import Entities.Tutorials.Formation;
import Entities.Tutorials.Jeu;
import Services.Tutorials.ServiceFormation;
import Services.Tutorials.ServiceJeu;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
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

    private ServiceFormation serviceFormation;
    private ServiceJeu serviceJeu;

    public AjouterFormationController() {
        serviceFormation = new ServiceFormation();
        serviceJeu = new ServiceJeu();
    }

    @FXML
    public void initialize() {
        cbNiveau.setItems(FXCollections.observableArrayList("debutant", "intermediaire", "avance"));

        try {
            List<Jeu> jeux = serviceJeu.getAll();
            cbJeu.setItems(FXCollections.observableArrayList(jeux));
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la liste des jeux.");
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
            showAlert(Alert.AlertType.ERROR, "Erreur de validation",
                    "Le titre, le niveau et le jeu sont obligatoires !");
            return;
        }

        try {
            boolean exists = serviceFormation.getAll().stream()
                    .anyMatch(f -> f.getTitre().equalsIgnoreCase(titre));
            if (exists) {
                showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Une formation avec ce titre existe déjà !");
                return;
            }

            Formation formation = new Formation(0, titre, description, niveau, jeu, image);
            serviceFormation.add(formation);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "La formation a été ajoutée avec succès !");
            fermerFenetre();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter la formation.");
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
