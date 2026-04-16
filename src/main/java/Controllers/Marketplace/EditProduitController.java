package Controllers.Marketplace;

import Entities.Marketplace.Categorie;
import Entities.Marketplace.Produit;
import Entities.Marketplace.Type;
import Services.Marketplace.ServiceCategorie;
import Services.Marketplace.ServiceProduit;
import Services.Marketplace.ServiceType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class EditProduitController {

    @FXML
    private TextField tfNom;
    @FXML
    private TextField tfDescription;
    @FXML
    private TextField tfPrix;
    @FXML
    private TextField tfStock;
    @FXML
    private ComboBox<Categorie> cbCategorie;
    @FXML
    private ComboBox<Type> cbType;
    @FXML
    private TextField tfImage;

    private ServiceProduit serviceProduit = new ServiceProduit();
    private ServiceCategorie serviceCategorie = new ServiceCategorie();
    private ServiceType serviceType = new ServiceType();
    private Produit produit;

    @FXML
    public void initialize() {
        try {
            List<Categorie> categories = serviceCategorie.getAll();
            cbCategorie.setItems(FXCollections.observableArrayList(categories));

            List<Type> types = serviceType.getAll();
            cbType.setItems(FXCollections.observableArrayList(types));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setProduit(Produit p) {
        this.produit = p;
        tfNom.setText(p.getNom());
        tfDescription.setText(p.getDescription());
        tfPrix.setText(String.valueOf(p.getPrix()));
        tfStock.setText(String.valueOf(p.getStock()));
        tfImage.setText(p.getImage());

        for (Categorie c : cbCategorie.getItems()) {
            if (c.getId() == p.getCategorieId()) {
                cbCategorie.getSelectionModel().select(c);
                break;
            }
        }

        for (Type t : cbType.getItems()) {
            if (t.getId() == p.getTypeId()) {
                cbType.getSelectionModel().select(t);
                break;
            }
        }
    }

    @FXML
    private void sauvegarder() {
        String nom = tfNom.getText().trim();
        String desc = tfDescription.getText().trim();
        String prixStr = tfPrix.getText().trim();
        String stockStr = tfStock.getText().trim();
        Categorie cat = cbCategorie.getValue();
        Type type = cbType.getValue();
        String image = tfImage.getText().trim();

        // Validation - Required fields
        if (nom.isEmpty() || desc.isEmpty() || prixStr.isEmpty() || stockStr.isEmpty() || cat == null || type == null) {
            Utils.ValidationUtils.showAlert("Erreur de saisie", "Tous les champs obligatoires (*) doivent être remplis !");
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
            // Duplicate Check (excluding current product)
            List<Produit> existingProduits = serviceProduit.getAll();
            for (Produit existing : existingProduits) {
                if (existing.getNom().equalsIgnoreCase(nom) && existing.getId() != produit.getId()) {
                    Utils.ValidationUtils.showAlert("Doublon", "Un autre produit porte déjà ce nom !");
                    return;
                }
            }

            produit.setNom(nom);
            produit.setDescription(desc);
            produit.setPrix(prix);
            produit.setStock(stock);
            produit.setCategorieId(cat.getId());
            produit.setTypeId(type.getId());
            produit.setImage(image);

            serviceProduit.update(produit);
            Utils.ValidationUtils.showSuccess("Succès", "Produit mis à jour avec succès !");
            closeWindow();
        } catch (SQLException e) {
            Utils.ValidationUtils.showAlert("Erreur", "Impossible de mettre à jour : " + e.getMessage());
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


}
