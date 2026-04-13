package Controllers.Tutorials;

import Entities.Tutorials.Option;
import Services.Tutorials.ServiceOption;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EditOptionController {

    @FXML
    private TextField tfTexte;
    @FXML
    private ComboBox<String> cbEstCorrecte;

    private ServiceOption serviceOption = new ServiceOption();
    private Option option;

    @FXML
    public void initialize() {
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
            showAlert(Alert.AlertType.ERROR, "Erreur", "Tous les champs sont obligatoires.");
            return;
        }

        option.setTexte(texte);
        option.setEstCorrecte(estCorrecteStr.equals("Vrai"));

        try {
            serviceOption.update(option);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Option mise à jour avec succès !");
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour l'option.");
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
