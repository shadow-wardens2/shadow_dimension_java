package Controllers.Marketplace.Back;

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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import Services.Marketplace.AiDescriptionService;
import javafx.application.Platform;
import java.io.File;

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
    @FXML private javafx.scene.control.Label errorLabel;

    private ServiceProduit sp = new ServiceProduit();
    private ServiceCategorie sc = new ServiceCategorie();
    private ServiceType st = new ServiceType();
    private AiDescriptionService ads = new AiDescriptionService();

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

        // Clear previous error
        errorLabel.setVisible(false);
        errorLabel.setText("");

        // Validation - Required fields
        if (nom.isEmpty() || desc.isEmpty() || prixStr.isEmpty() || stockStr.isEmpty() || cat == null || type == null) {
            showError("Tous les champs doivent être remplis !");
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
        if (prix < 0) {
            showError("Le prix ne peut pas être négatif !");
            return;
        }

        // Validation - Numeric Stock
        if (!Utils.ValidationUtils.isInteger(stockStr)) {
            showError("Le stock doit être un nombre entier !");
            return;
        }
        int stock = Integer.parseInt(stockStr);
        if (stock < 0) {
            showError("Le stock ne peut pas être négatif !");
            return;
        }

        try {
            // Duplicate Check
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
            showError("Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }


    @FXML
    private void handleAnnuler() {
        closeWindow();
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(tfNom.getScene().getWindow());
        if (selectedFile != null) {
            tfImage.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void handleGenerateAiDescription() {
        String nom = tfNom.getText().trim();
        Categorie cat = cbCategorie.getValue();
        String catName = (cat != null) ? cat.getNom() : "Inconnu";

        if (nom.isEmpty()) {
            showError("Veuillez d'abord saisir le nom du produit !");
            return;
        }

        tfDescription.setText("L'ombre s'agite... l'oracle écrit...");
        tfDescription.setDisable(true);

        new Thread(() -> {
            try {
                String aiDesc = ads.generateDescription(nom, catName);
                Platform.runLater(() -> {
                    tfDescription.setText(aiDesc);
                    tfDescription.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("L'oracle est silencieux : " + e.getMessage());
                    tfDescription.setText("");
                    tfDescription.setDisable(false);
                });
            }
        }).start();
    }

    private void closeWindow() {
        Stage stage = (Stage) tfNom.getScene().getWindow();
        stage.close();
    }
}