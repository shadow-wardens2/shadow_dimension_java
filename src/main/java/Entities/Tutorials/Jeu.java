package Entities.Tutorials;

import java.util.ArrayList;
import java.util.List;

public class Jeu {
    private int id;
    private String nom;
    private String genre;
    private List<Formation> formations;

    public Jeu() {
        this.formations = new ArrayList<>();
    }

    public Jeu(int id, String nom, String genre) {
        this();
        this.id = id;
        this.nom = nom;
        this.genre = genre;
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

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public List<Formation> getFormations() {
        return formations;
    }

    public void setFormations(List<Formation> formations) {
        this.formations = formations;
    }

    public void addFormation(Formation formation) {
        if (!this.formations.contains(formation)) {
            this.formations.add(formation);
            formation.setJeu(this);
        }
    }

    public void removeFormation(Formation formation) {
        this.formations.remove(formation);
    }

    @Override
    public String toString() {
        return this.nom != null ? this.nom : "New Jeu";
    }
}
