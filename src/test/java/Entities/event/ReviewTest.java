package Entities.event;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class ReviewTest {

    @Test
    void testReviewGettersAndSetters() {
        Review review = new Review();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        review.setId(1);
        review.setUserId(2);
        review.setEventId(3);
        review.setRating(5);
        review.setComment("Great event!");
        review.setCreatedAt(now);

        // Virtual properties
        review.setUsername("johndoe");
        review.setEventTitle("Epic Party");

        assertEquals(1, review.getId());
        assertEquals(2, review.getUserId());
        assertEquals(3, review.getEventId());
        assertEquals(5, review.getRating());
        assertEquals("Great event!", review.getComment());
        assertEquals(now, review.getCreatedAt());

        assertEquals("johndoe", review.getUsername());
        assertEquals("Epic Party", review.getEventTitle());
    }

    @Test
    void testReviewConstructor() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Review review = new Review(10, 20, 30, 4, "Nice!", now);

        assertEquals(10, review.getId());
        assertEquals(20, review.getUserId());
        assertEquals(30, review.getEventId());
        assertEquals(4, review.getRating());
        assertEquals("Nice!", review.getComment());
        assertEquals(now, review.getCreatedAt());
    }
}
