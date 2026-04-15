package Controllers.Tutorials;

import Entities.Tutorials.Option;
import Entities.Tutorials.Question;
import Services.Tutorials.ServiceOption;
import Services.Tutorials.ServiceQuestion;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class AjouterOptionController {

    @FXML
    private TextField tfTexte;

    @FXML
    private ComboBox<String> cbEstCorrecte;

    @FXML
    private ComboBox<Question> cbQuestion;

    @FXML
    private Label lbError;

    private ServiceOption serviceOption;
    private ServiceQuestion serviceQuestion;

    public AjouterOptionController() {
        serviceOption = new ServiceOption();
        serviceQuestion = new ServiceQuestion();
    }

    @FXML
    public void initialize() {
        lbError.setText("");
        cbEstCorrecte.setItems(FXCollections.observableArrayList("Vrai", "Faux"));

        try {
            List<Question> questions = serviceQuestion.getAll();
            cbQuestion.setItems(FXCollections.observableArrayList(questions));
        } catch (SQLException e) {
            e.printStackTrace();
            lbError.setText("Erreur: Impossible de charger les questions.");
        }
    }

    public void setPreselectedQuestion(Question question) {
        cbQuestion.setValue(question);
    }

    @FXML
    private void ajouterOption() {
        String texte = tfTexte.getText().trim();
        String estCorrecteStr = cbEstCorrecte.getSelectionModel().getSelectedItem();
        Question question = cbQuestion.getSelectionModel().getSelectedItem();

        if (texte.isEmpty() || estCorrecteStr == null || question == null) {
            lbError.setText("Erreur: Tous les champs sont obligatoires !");
            return;
        }

        try {
            boolean estCorrecte = estCorrecteStr.equals("Vrai");
            Option option = new Option(0, texte, estCorrecte, question);
            serviceOption.add(option);

            fermerFenetre();
        } catch (Exception e) {
            e.printStackTrace();
            lbError.setText("Erreur: Impossible d'ajouter l'option.");
        }
    }

    @FXML
    private void annuler() {
        fermerFenetre();
    }

    private void fermerFenetre() {
        Stage stage = (Stage) tfTexte.getScene().getWindow();
        stage.close();
    }
}
