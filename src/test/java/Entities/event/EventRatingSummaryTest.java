package Entities.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventRatingSummaryTest {

    @Test
    void testConstructorAndGetters() {
        EventRatingSummary summary = new EventRatingSummary(5, 4.5, 12);

        assertEquals(5, summary.getEventId());
        assertEquals(4.5, summary.getAverageRating());
        assertEquals(12, summary.getTotalReviews());
    }
}
