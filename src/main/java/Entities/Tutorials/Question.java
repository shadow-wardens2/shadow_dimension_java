package Entities.Tutorials;

import java.util.ArrayList;
import java.util.List;

public class Question {
    private int id;
    private String texte;
    private Quiz quiz;
    private List<Option> options;

    public Question() {
        this.options = new ArrayList<>();
    }

    public Question(int id, String texte, Quiz quiz) {
        this();
        this.id = id;
        this.texte = texte;
        this.quiz = quiz;
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

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }

    public void addOption(Option option) {
        if (!this.options.contains(option)) {
            this.options.add(option);
            option.setQuestion(this);
        }
    }

    public void removeOption(Option option) {
        this.options.remove(option);
    }

    @Override
    public String toString() {
        return this.texte != null ? this.texte : "Select Question...";
    }
}
