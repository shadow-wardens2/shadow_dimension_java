package Entities.Artworks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EvaluationTest {

    @Test
    void testEvaluationGettersAndSetters() {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(1);
        evaluation.setArtworkId(10);
        evaluation.setUserId(100);
        evaluation.setRating(5);
        evaluation.setComment("Amazing!");
        evaluation.setDate("2024-01-01");
        evaluation.setArtworkTitle("Starry Night");

        assertEquals(1, evaluation.getId());
        assertEquals(10, evaluation.getArtworkId());
        assertEquals(100, evaluation.getUserId());
        assertEquals(5, evaluation.getRating());
        assertEquals("Amazing!", evaluation.getComment());
        assertEquals("2024-01-01", evaluation.getDate());
        assertEquals("Starry Night", evaluation.getArtworkTitle());
    }
}
