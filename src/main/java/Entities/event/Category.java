package Entities.event;

import java.sql.Timestamp;

public class Category {
    private int id;
    private String nom;
    private String description;
    private String typeTarification;
    private Double prix;
    private String creatorType;
    private Timestamp createdAt;

    public Category() {
    }

    public Category(int id, String nom, String description, String typeTarification, Double prix,
                    String creatorType, Timestamp createdAt) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.typeTarification = typeTarification;
        this.prix = prix;
        this.creatorType = creatorType;
        this.createdAt = createdAt;
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

    public String getTypeTarification() {
        return typeTarification;
    }

    public void setTypeTarification(String typeTarification) {
        this.typeTarification = typeTarification;
    }

    public Double getPrix() {
        return prix;
    }

    public void setPrix(Double prix) {
        this.prix = prix;
    }

    public String getCreatorType() {
        return creatorType;
    }

    public void setCreatorType(String creatorType) {
        this.creatorType = creatorType;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return nom;
    }
}
