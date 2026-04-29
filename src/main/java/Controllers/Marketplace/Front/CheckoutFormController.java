package Controllers.Marketplace.Front;

import Entities.Marketplace.Commande;
import Services.Marketplace.ServiceCommande;
import Utils.CartManager;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;

public class CheckoutFormController {

    @FXML private AnchorPane rootNode;
    @FXML private TextField tfFullName;
    @FXML private TextField tfEmail;
    @FXML private TextField tfAdresse;
    @FXML private TextField tfVille;
    @FXML private TextField tfCodePostal;
    @FXML private TextField tfPays;
    @FXML private TextField tfTelephone;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        if (SessionManager.isLoggedIn()) {
            Entities.User.User u = SessionManager.getCurrentUser();
            tfEmail.setText(u.getEmail() != null ? u.getEmail() : "");
        }
    }

    @FXML
    void handleConfirm() {
        String fullName = tfFullName.getText().trim();
        String email = tfEmail.getText().trim();
        String adresse = tfAdresse.getText().trim();
        String ville = tfVille.getText().trim();
        String codePostal = tfCodePostal.getText().trim();
        String pays = tfPays.getText().trim();
        String telephone = tfTelephone.getText().trim();

        if (fullName.isEmpty() || email.isEmpty() || adresse.isEmpty() || ville.isEmpty() || codePostal.isEmpty() || pays.isEmpty() || telephone.isEmpty()) {
            showError("Please fill in all the required coordinates.");
            return;
        }

        String nom = fullName;
        String prenom = "";
        int spaceIndex = fullName.indexOf(' ');
        if (spaceIndex != -1) {
            nom = fullName.substring(0, spaceIndex);
            prenom = fullName.substring(spaceIndex + 1);
        }

        Commande commande = new Commande();
        commande.setTotalAmount(CartManager.getTotal());
        commande.setStatus("PENDING");
        commande.setDateCommande(new Timestamp(System.currentTimeMillis()));
        commande.setProduits(CartManager.getCartItems());
        
        if (SessionManager.isLoggedIn()) {
            commande.setUserId(SessionManager.getCurrentUser().getId());
        } else {
            commande.setUserId(1); 
        }
        
        commande.setNom(nom);
        commande.setPrenom(prenom);
        commande.setEmail(email);
        commande.setAdresse(adresse);
        commande.setVille(ville);
        commande.setCodePostal(codePostal);
        commande.setPays(pays);
        commande.setTelephone(telephone);

        ServiceCommande sc = new ServiceCommande();
        try {
            sc.add(commande);
            CartManager.clearCart();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Order Confirmed");
            alert.setHeaderText(null);
            alert.setContentText("Your order has been successfully placed! ID: " + commande.getId());
            alert.showAndWait();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/Front/MarketplaceFront.fxml"));
            rootNode.getScene().setRoot(loader.load());
            
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            showError("Failed to place order: " + e.getMessage());
        }
    }

    @FXML
    void handleCancel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Marketplace/Front/CartView.fxml"));
            rootNode.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
