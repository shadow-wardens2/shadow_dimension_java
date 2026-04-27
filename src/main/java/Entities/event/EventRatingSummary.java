package Entities.event;

public class EventRatingSummary {
    private int eventId;
    private double averageRating;
    private int totalReviews;

    public EventRatingSummary(int eventId, double averageRating, int totalReviews) {
        this.eventId = eventId;
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
    }

    public int getEventId() {
        return eventId;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getTotalReviews() {
        return totalReviews;
    }
}
