package Repositories.event;

import Entities.event.EventRatingSummary;
import Entities.event.Review;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository {
    Optional<Review> findByUserAndEvent(int userId, int eventId) throws SQLException;

    Review create(Review review) throws SQLException;

    void update(Review review) throws SQLException;

    List<Review> findByEventId(int eventId) throws SQLException;

    List<Review> findForBackOffice(String search, String sortBy, boolean ascending, int offset, int limit) throws SQLException;

    int countForBackOffice(String search) throws SQLException;

    void deleteById(int reviewId) throws SQLException;

    EventRatingSummary getEventRatingSummary(int eventId) throws SQLException;
}
