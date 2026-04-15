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
    @FXML private javafx.scene.control.Label lblError;

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
        lblError.setVisible(false);
        String nom = tfNom.getText().trim();
        String desc = tfDescription.getText().trim();
        String prixStr = tfPrix.getText().trim();
        String stockStr = tfStock.getText().trim();
        Categorie cat = cbCategorie.getValue();
        Type type = cbType.getValue();
        String image = tfImage.getText().trim();

        if (nom.isEmpty() || desc.isEmpty() || prixStr.isEmpty() || stockStr.isEmpty()
                || image.isEmpty() || cat == null || type == null) {
            showError("Tous les champs sont obligatoires !");
            return;
        }

        if (nom.length() <= 3) {
            showError("Le nom du produit doit avoir plus de 3 caractères !");
            return;
        }

        if (!Utils.ValidationUtils.isNumeric(prixStr)) {
            showError("Le prix doit être un nombre valide (ex: 10.5) !");
            return;
        }
        double prix = Double.parseDouble(prixStr);
        if (prix <= 0) {
            showError("Le prix doit être supérieur à 0 !");
            return;
        }

        if (!Utils.ValidationUtils.isInteger(stockStr)) {
            showError("Le stock doit être un nombre entier !");
            return;
        }
        int stock = Integer.parseInt(stockStr);
        if (stock <= 0) {
            showError("Le stock doit être supérieur à 0 !");
            return;
        }

        try {
            List<Produit> existingProduits = sp.getAll();
            for (Produit existing : existingProduits) {
                if (existing.getNom().equalsIgnoreCase(nom)) {
                    showError("Un produit avec ce nom existe déjà !");
                    return;
                }
            }

            Produit p = new Produit(0, nom, desc, prix, stock, cat.getId(), type.getId(), image);
            sp.add(p);
            Utils.ValidationUtils.showSuccess("Succès", "Produit ajouté avec succès !");
            closeWindow();
        } catch (SQLException e) {
            showError("Une erreur est survenue lors de l'ajout : " + e.getMessage());
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
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