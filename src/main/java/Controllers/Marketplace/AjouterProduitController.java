package Controllers.Marketplace;

import Entities.Marketplace.Categorie;
import Entities.Marketplace.Produit;
import Entities.Marketplace.Type;
import Services.Marketplace.ServiceCategorie;
import Services.Marketplace.ServiceProduit;
import Services.Marketplace.ServiceType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class AjouterProduitController {

    @FXML private TextField tfNom;
    @FXML private TextField tfDescription;
    @FXML private TextField tfPrix;
    @FXML private TextField tfStock;
    @FXML private ComboBox<Categorie> cbCategorie;
    @FXML private ComboBox<Type> cbType;
    @FXML private TextField tfImage;

    private ServiceProduit sp = new ServiceProduit();
    private ServiceCategorie sc = new ServiceCategorie();
    private ServiceType st = new ServiceType();

    @FXML
    public void initialize() {
        try {
            List<Categorie> categories = sc.getAll();
            cbCategorie.setItems(FXCollections.observableArrayList(categories));

            List<Type> types = st.getAll();
            cbType.setItems(FXCollections.observableArrayList(types));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAjouter() {
        String nom = tfNom.getText().trim();
        String desc = tfDescription.getText().trim();
        String prixStr = tfPrix.getText().trim();
        String stockStr = tfStock.getText().trim();
        Categorie cat = cbCategorie.getValue();
        Type type = cbType.getValue();
        String image = tfImage.getText().trim();

        // Validation - Required fields
        if (nom.isEmpty() || desc.isEmpty() || prixStr.isEmpty() || stockStr.isEmpty() || cat == null || type == null) {
            Utils.ValidationUtils.showAlert("Erreur de saisie", "Tous les champs doivent être remplis !");
            return;
        }

        // Validation - Name length
        if (nom.length() <= 3) {
            Utils.ValidationUtils.showAlert("Erreur de saisie", "Le nom du produit doit avoir plus de 3 caractères !");
            return;
        }

        // Validation - Numeric Price
        if (!Utils.ValidationUtils.isNumeric(prixStr)) {
            Utils.ValidationUtils.showAlert("Erreur de saisie", "Le prix doit être un nombre valide (ex: 10.5) !");
            return;
        }
        double prix = Double.parseDouble(prixStr);
        if (prix < 0) {
            Utils.ValidationUtils.showAlert("Erreur de saisie", "Le prix ne peut pas être négatif !");
            return;
        }

        // Validation - Numeric Stock
        if (!Utils.ValidationUtils.isInteger(stockStr)) {
            Utils.ValidationUtils.showAlert("Erreur de saisie", "Le stock doit être un nombre entier !");
            return;
        }
        int stock = Integer.parseInt(stockStr);
        if (stock < 0) {
            Utils.ValidationUtils.showAlert("Erreur de saisie", "Le stock ne peut pas être négatif !");
            return;
        }

        try {
            // Duplicate Check
            List<Produit> existingProduits = sp.getAll();
            for (Produit existing : existingProduits) {
                if (existing.getNom().equalsIgnoreCase(nom)) {
                    Utils.ValidationUtils.showAlert("Doublon", "Un produit avec ce nom existe déjà !");
                    return;
                }
            }

            Produit p = new Produit(0, nom, desc, prix, stock, cat.getId(), type.getId(), image);
            sp.add(p);
            Utils.ValidationUtils.showSuccess("Succès", "Produit ajouté avec succès !");
            closeWindow();
        } catch (SQLException e) {
            Utils.ValidationUtils.showAlert("Erreur SQL", "Une erreur est survenue lors de l'ajout : " + e.getMessage());
        }
    }

    @FXML
    private void handleAnnuler() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) tfNom.getScene().getWindow();
        stage.close();
    }
}