package Entities.Marketplace;

import java.util.Objects;

public class Produit {

    private int id;
    private String nom;
    private String description;
    private double prix;
    private int stock;
    private int categorieId;
    private int typeId;
    private String image;

    public Produit() {}

    public Produit(int id, String nom, String description, double prix, int stock,
                   int categorieId, int typeId, String image) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.stock = stock;
        this.categorieId = categorieId;
        this.typeId = typeId;
        this.image = image;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getNom() {
        return nom;
    }
    public void setNom(String nom) {
        this.nom = nom;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public double getPrix() {
        return prix;
    }
    public void setPrix(double prix) {
        this.prix = prix;
    }
    public int getStock() {
        return stock;
    }
    public void setStock(int stock) {
        this.stock = stock;
    }
    public int getCategorieId() {
        return categorieId;
    }
    public void setCategorieId(int categorieId) {
        this.categorieId = categorieId;
    }
    public int getTypeId() {
        return typeId;
    }
    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }
    public String getImage() {
        return image;
    }
    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return "Produit{" + "id=" + id + ", nom=" + nom + ", description=" + description + ", prix=" + prix + ", stock=" + stock + ", categorieId=" + categorieId + ", image=" + image + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Produit)) return false;

        Produit p = (Produit) o;
        return id == p.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }



}
