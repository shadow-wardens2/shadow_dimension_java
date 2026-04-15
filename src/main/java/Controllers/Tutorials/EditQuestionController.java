package Controllers.Tutorials;

import Entities.Tutorials.Question;
import Services.Tutorials.ServiceQuestion;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class EditQuestionController {

    @FXML
    private TextArea taTexte;

    @FXML
    private Label lbError;

    private ServiceQuestion serviceQuestion;
    private Question question;

    public EditQuestionController() {
        serviceQuestion = new ServiceQuestion();
    }

    public void setQuestion(Question q) {
        this.question = q;
        if (q != null) {
            taTexte.setText(q.getTexte());
        }
    }

    @FXML
    private void sauvegarder() {
        String texte = taTexte.getText().trim();

        if (texte.isEmpty()) {
            lbError.setText("Erreur: Le texte de la question est obligatoire !");
            return;
        }

        try {
            question.setTexte(texte);
            serviceQuestion.update(question);
            fermerFenetre();
        } catch (Exception e) {
            e.printStackTrace();
            lbError.setText("Erreur: Impossible de mettre à jour la question.");
        }
    }

    @FXML
    private void annuler() {
        fermerFenetre();
    }

    private void fermerFenetre() {
        Stage stage = (Stage) taTexte.getScene().getWindow();
        stage.close();
    }
}
