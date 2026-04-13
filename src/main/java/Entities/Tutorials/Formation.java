package Entities.Tutorials;

import java.util.ArrayList;
import java.util.List;

public class Formation {
    private int id;
    private String titre;
    private String description;
    private String niveau;
    private Jeu jeu;
    private List<Lecon> lecons;
    private List<Quiz> quizzes;
    private String image;

    public Formation() {
        this.lecons = new ArrayList<>();
        this.quizzes = new ArrayList<>();
    }

    public Formation(int id, String titre, String description, String niveau, Jeu jeu, String image) {
        this();
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.niveau = niveau;
        this.jeu = jeu;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public Jeu getJeu() {
        return jeu;
    }

    public void setJeu(Jeu jeu) {
        this.jeu = jeu;
    }

    public List<Lecon> getLecons() {
        return lecons;
    }

    public void setLecons(List<Lecon> lecons) {
        this.lecons = lecons;
    }

    public void addLecon(Lecon lecon) {
        if (!this.lecons.contains(lecon)) {
            this.lecons.add(lecon);
            lecon.setFormation(this);
        }
    }

    public void removeLecon(Lecon lecon) {
        this.lecons.remove(lecon);
    }

    public List<Quiz> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<Quiz> quizzes) {
        this.quizzes = quizzes;
    }

    public void addQuiz(Quiz quiz) {
        if (!this.quizzes.contains(quiz)) {
            this.quizzes.add(quiz);
            quiz.setFormation(this);
        }
    }

    public void removeQuiz(Quiz quiz) {
        this.quizzes.remove(quiz);
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return this.titre != null ? this.titre : "New Formation";
    }
}
