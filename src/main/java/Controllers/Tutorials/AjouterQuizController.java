package Controllers.Tutorials;

import Entities.Tutorials.Formation;
import Entities.Tutorials.Quiz;
import Services.Tutorials.ServiceFormation;
import Services.Tutorials.ServiceQuiz;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class AjouterQuizController {

    @FXML
    private TextField tfTitre;

    @FXML
    private TextField tfOrdre;

    @FXML
    private ComboBox<Formation> cbFormation;

    @FXML
    private Label lbError;

    private ServiceQuiz serviceQuiz;
    private ServiceFormation serviceFormation;

    public AjouterQuizController() {
        serviceQuiz = new ServiceQuiz();
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
    private void ajouterQuiz() {
        String titre = tfTitre.getText().trim();
        String ordreStr = tfOrdre.getText().trim();
        Formation formation = cbFormation.getSelectionModel().getSelectedItem();

        if (titre.isEmpty() || ordreStr.isEmpty() || formation == null) {
            lbError.setText("Erreur: Tous les champs sont obligatoires !");
            return;
        }

        int ordre;
        try {
            ordre = Integer.parseInt(ordreStr);
        } catch (NumberFormatException e) {
            lbError.setText("Erreur: L'ordre doit être un nombre.");
            return;
        }

        try {
            Quiz quiz = new Quiz(0, titre, ordre, formation);
            serviceQuiz.add(quiz);
            fermerFenetre();
        } catch (Exception e) {
            e.printStackTrace();
            lbError.setText("Erreur: Impossible d'ajouter le quiz.");
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
