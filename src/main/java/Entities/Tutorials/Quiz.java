package Entities.Tutorials;

import java.util.ArrayList;
import java.util.List;

public class Quiz {
    private int id;
    private String titre;
    private int ordre;
    private Formation formation;
    private List<Question> questions;

    public Quiz() {
        this.questions = new ArrayList<>();
    }

    public Quiz(int id, String titre, int ordre, Formation formation) {
        this();
        this.id = id;
        this.titre = titre;
        this.ordre = ordre;
        this.formation = formation;
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

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public Formation getFormation() {
        return formation;
    }

    public void setFormation(Formation formation) {
        this.formation = formation;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public void addQuestion(Question question) {
        if (!this.questions.contains(question)) {
            this.questions.add(question);
            question.setQuiz(this);
        }
    }

    public void removeQuestion(Question question) {
        this.questions.remove(question);
    }

    @Override
    public String toString() {
        return this.titre != null ? this.titre : "New Quiz";
    }
}
