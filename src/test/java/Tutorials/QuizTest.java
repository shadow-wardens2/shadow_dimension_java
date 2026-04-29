package Tutorials;

import Entities.Tutorials.Quiz;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QuizTest {
    @Test
    public void testQuizSettersAndGetters() {
        Quiz quiz = new Quiz();
        quiz.setTitre("Quiz Final");
        quiz.setOrdre(5);

        Assertions.assertEquals("Quiz Final", quiz.getTitre());
        Assertions.assertEquals(5, quiz.getOrdre());
    }
}
