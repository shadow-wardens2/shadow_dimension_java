package Tutorials;

import Entities.Tutorials.Quiz;
import Services.Tutorials.ServiceQuiz;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

public class ServiceQuizTest {

    @Test
    public void testGetAllQuizzes() {
        ServiceQuiz service = new ServiceQuiz();
        try {
            List<Quiz> quizzes = service.getAll();
            // We just ensure it doesn't crash and returns a non-null list
            Assertions.assertNotNull(quizzes, "The list should not be null, even if empty");
        } catch (SQLException e) {
            // If the database is not running, we skip but print it
            System.err.println("Skipping DB test: JDBC Connection failed.");
        }
    }
}
