package Controllers;

import Entities.Categorie;
import Entities.Produit;
import Entities.Type;
import Services.ServiceCategorie;
import Services.ServiceProduit;
import Services.ServiceType;
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
        try {
            String nom = tfNom.getText();
            String desc = tfDescription.getText();
            double prix = Double.parseDouble(tfPrix.getText());
            int stock = Integer.parseInt(tfStock.getText());
            Categorie cat = cbCategorie.getValue();
            Type type = cbType.getValue();
            String image = tfImage.getText();

            if (nom.isEmpty() || desc.isEmpty() || cat == null || type == null) {
                System.out.println("Tous les champs doivent être remplis !");
                return;
            }

            Produit p = new Produit(0, nom, desc, prix, stock, cat.getId(), type.getId(), image);
            sp.add(p);
            System.out.println("Produit ajouté avec succès !");
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.println("Prix et Stock doivent être des nombres !");
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