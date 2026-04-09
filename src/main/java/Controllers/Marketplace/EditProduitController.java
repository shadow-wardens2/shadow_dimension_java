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
        try {
            String nom = tfNom.getText();
            String desc = tfDescription.getText();
            double prix = Double.parseDouble(tfPrix.getText());
            int stock = Integer.parseInt(tfStock.getText());
            Categorie cat = cbCategorie.getValue();
            Type type = cbType.getValue();
            String image = tfImage.getText();

            if (nom.isEmpty() || desc.isEmpty() || cat == null || type == null) {
                showAlert("Erreur", "Tous les champs doivent être remplis !");
                return;
            }

            produit.setNom(nom);
            produit.setDescription(desc);
            produit.setPrix(prix);
            produit.setStock(stock);
            produit.setCategorieId(cat.getId());
            produit.setTypeId(type.getId());
            produit.setImage(image);

            serviceProduit.update(produit);
            showAlert("Succès", "Produit mis à jour avec succès !");
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de mettre à jour : " + e.getMessage());
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Prix et Stock doivent être des nombres valide !");
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
