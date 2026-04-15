package Tutorials;

import Entities.Tutorials.Question;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QuestionTest {
    @Test
    public void testQuestionSettersAndGetters() {
        Question question = new Question();
        question.setTexte("Quelle est la capitale de la France?");

        Assertions.assertEquals("Quelle est la capitale de la France?", question.getTexte());
    }
}
