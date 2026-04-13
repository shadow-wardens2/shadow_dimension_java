package Entities.Tutorials;

public class Option {
    private int id;
    private String texte;
    private boolean estCorrecte;
    private Question question;

    public Option() {
    }

    public Option(int id, String texte, boolean estCorrecte, Question question) {
        this.id = id;
        this.texte = texte;
        this.estCorrecte = estCorrecte;
        this.question = question;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTexte() {
        return texte;
    }

    public void setTexte(String texte) {
        this.texte = texte;
    }

    public boolean isEstCorrecte() {
        return estCorrecte;
    }

    public void setEstCorrecte(boolean estCorrecte) {
        this.estCorrecte = estCorrecte;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    @Override
    public String toString() {
        return this.texte != null ? this.texte : "Select Option...";
    }
}
