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
    @FXML
    private javafx.scene.control.Label lblError;

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
        lblError.setVisible(false);
        String nom = tfNom.getText().trim();
        String desc = tfDescription.getText().trim();
        String prixStr = tfPrix.getText().trim();
        String stockStr = tfStock.getText().trim();
        Categorie cat = cbCategorie.getValue();
        Type type = cbType.getValue();
        String image = tfImage.getText().trim();

        // Validation - Required fields
        if (nom.isEmpty() || desc.isEmpty() || prixStr.isEmpty() || stockStr.isEmpty()
                || image.isEmpty() || cat == null || type == null) {
            showError("Tous les champs sont obligatoires !");
            return;
        }

        // Validation - Name length
        if (nom.length() <= 3) {
            showError("Le nom du produit doit avoir plus de 3 caractères !");
            return;
        }

        // Validation - Numeric Price
        if (!Utils.ValidationUtils.isNumeric(prixStr)) {
            showError("Le prix doit être un nombre valide (ex: 10.5) !");
            return;
        }
        double prix = Double.parseDouble(prixStr);
        if (prix <= 0) {
            showError("Le prix doit être supérieur à 0 !");
            return;
        }

        // Validation - Numeric Stock
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
            // Duplicate Check (excluding current product)
            List<Produit> existingProduits = serviceProduit.getAll();
            for (Produit existing : existingProduits) {
                if (existing.getNom().equalsIgnoreCase(nom) && existing.getId() != produit.getId()) {
                    showError("Un autre produit porte déjà ce nom !");
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
            showError("Impossible de mettre à jour : " + e.getMessage());
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
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
