package Entities.Marketplace;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Commande {
    private int id;
    private Timestamp dateCommande;
    private double totalAmount;
    private String status;
    private String nom;
    private String prenom;
    private String email;
    private String adresse;
    private String ville;
    private String codePostal;
    private String telephone;
    private String pays;
    private String trackingNumber;
    private String shippingCarrier;
    private String shippingMethod;
    private double shippingCost;
    private double poidsTotal;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private int userId;

    private List<Produit> produits;

    public Commande() {
        this.produits = new ArrayList<>();
    }

    // Getters
    public int getId() { return id; }
    public Timestamp getDateCommande() { return dateCommande; }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getAdresse() { return adresse; }
    public String getVille() { return ville; }
    public String getCodePostal() { return codePostal; }
    public String getTelephone() { return telephone; }
    public String getPays() { return pays; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getShippingCarrier() { return shippingCarrier; }
    public String getShippingMethod() { return shippingMethod; }
    public double getShippingCost() { return shippingCost; }
    public double getPoidsTotal() { return poidsTotal; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public int getUserId() { return userId; }
    public List<Produit> getProduits() { return produits; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setDateCommande(Timestamp dateCommande) { this.dateCommande = dateCommande; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public void setStatus(String status) { this.status = status; }
    public void setNom(String nom) { this.nom = nom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public void setEmail(String email) { this.email = email; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public void setVille(String ville) { this.ville = ville; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public void setPays(String pays) { this.pays = pays; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public void setShippingCarrier(String shippingCarrier) { this.shippingCarrier = shippingCarrier; }
    public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }
    public void setShippingCost(double shippingCost) { this.shippingCost = shippingCost; }
    public void setPoidsTotal(double poidsTotal) { this.poidsTotal = poidsTotal; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setProduits(List<Produit> produits) { this.produits = produits; }
    public void addProduit(Produit p) { this.produits.add(p); }
}
