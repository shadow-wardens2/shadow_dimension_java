package Controllers.Tutorials;

import Entities.Tutorials.Option;
import Entities.Tutorials.Question;
import Services.Tutorials.ServiceOption;
import Services.Tutorials.ServiceQuestion;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
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

    private ServiceOption serviceOption;
    private ServiceQuestion serviceQuestion;

    public AjouterOptionController() {
        serviceOption = new ServiceOption();
        serviceQuestion = new ServiceQuestion();
    }

    @FXML
    public void initialize() {
        cbEstCorrecte.setItems(FXCollections.observableArrayList("Vrai", "Faux"));

        try {
            List<Question> questions = serviceQuestion.getAll();
            cbQuestion.setItems(FXCollections.observableArrayList(questions));
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les questions.");
        }
    }

    public void setPreselectedQuestion(Question question) {
        if (question != null && cbQuestion.getItems() != null) {
            for (Question q : cbQuestion.getItems()) {
                if (q.getId() == question.getId()) {
                    cbQuestion.getSelectionModel().select(q);
                    break;
                }
            }
        }
    }

    @FXML
    private void ajouterOption() {
        String texte = tfTexte.getText().trim();
        String estCorrecteStr = cbEstCorrecte.getSelectionModel().getSelectedItem();
        Question question = cbQuestion.getSelectionModel().getSelectedItem();

        if (texte.isEmpty() || estCorrecteStr == null || question == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Tous les champs sont obligatoires !");
            return;
        }

        boolean estCorrecte = estCorrecteStr.equals("Vrai");

        try {
            Option option = new Option(0, texte, estCorrecte, question);
            serviceOption.add(option);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "L'option a été ajoutée avec succès !");
            fermerFenetre();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter l'option.");
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

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
