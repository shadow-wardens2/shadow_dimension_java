package Controllers.Tutorials;

import Entities.Tutorials.Option;
import Services.Tutorials.ServiceOption;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class EditOptionController {

    @FXML
    private TextField tfTexte;
    @FXML
    private ComboBox<String> cbEstCorrecte;
    @FXML
    private Label lbError;

    private ServiceOption serviceOption = new ServiceOption();
    private Option option;

    @FXML
    public void initialize() {
        lbError.setText("");
        cbEstCorrecte.setItems(FXCollections.observableArrayList("Vrai", "Faux"));
    }

    public void setOption(Option option) {
        this.option = option;
        tfTexte.setText(option.getTexte());
        cbEstCorrecte.setValue(option.isEstCorrecte() ? "Vrai" : "Faux");
    }

    @FXML
    private void sauvegarder() {
        String texte = tfTexte.getText().trim();
        String estCorrecteStr = cbEstCorrecte.getValue();

        if (texte.isEmpty() || estCorrecteStr == null) {
            lbError.setText("Erreur: Tous les champs sont obligatoires.");
            return;
        }

        option.setTexte(texte);
        option.setEstCorrecte(estCorrecteStr.equals("Vrai"));

        try {
            serviceOption.update(option);
            closeWindow();
        } catch (Exception e) {
            e.printStackTrace();
            lbError.setText("Erreur: Impossible de mettre à jour l'option.");
        }
    }

    @FXML
    private void annuler() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) tfTexte.getScene().getWindow();
        stage.close();
    }
}
