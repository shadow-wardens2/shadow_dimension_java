package Services.event;

import Entities.User.User;
import Entities.event.EventRatingSummary;
import Entities.event.Event;
import Entities.event.Reservation;
import Entities.event.ReservationStatus;
import Entities.event.Review;
import Repositories.event.JdbcReviewRepository;
import Repositories.event.ReservationRepository;
import Repositories.event.ReviewRepository;
import Repositories.event.JdbcReservationRepository;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final EventService eventService;

    public ReviewService() {
        this(new JdbcReviewRepository(), new JdbcReservationRepository(), new EventService());
    }

    public ReviewService(ReviewRepository reviewRepository,
                         ReservationRepository reservationRepository,
                         EventService eventService) {
        this.reviewRepository = reviewRepository;
        this.reservationRepository = reservationRepository;
        this.eventService = eventService;
    }

    public Review createOrUpdate(int userId, int eventId, int rating, String comment) {
        validatePayload(userId, eventId, rating);

        try {
            enforceEligibility(userId, eventId);

            Optional<Review> existing = reviewRepository.findByUserAndEvent(userId, eventId);
            if (existing.isPresent()) {
                Review toUpdate = existing.get();
                toUpdate.setRating(rating);
                toUpdate.setComment(comment == null ? "" : comment.trim());
                toUpdate.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                reviewRepository.update(toUpdate);
                return reviewRepository.findByUserAndEvent(userId, eventId)
                        .orElseThrow(() -> new EventModuleException("Review update failed."));
            }

            Review review = new Review();
            review.setUserId(userId);
            review.setEventId(eventId);
            review.setRating(rating);
            review.setComment(comment == null ? "" : comment.trim());
            review.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            return reviewRepository.create(review);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to save review: " + e.getMessage(), e);
        }
    }

    public Optional<Review> findByUserAndEvent(int userId, int eventId) {
        try {
            return reviewRepository.findByUserAndEvent(userId, eventId);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to read review: " + e.getMessage(), e);
        }
    }

    public List<Review> findByEvent(int eventId) {
        try {
            if (eventId <= 0) {
                return Collections.emptyList();
            }
            return reviewRepository.findByEventId(eventId);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to fetch event reviews: " + e.getMessage(), e);
        }
    }

    public EventRatingSummary getRatingSummary(int eventId) {
        try {
            return reviewRepository.getEventRatingSummary(eventId);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to fetch rating summary: " + e.getMessage(), e);
        }
    }

    public List<Review> findBackOfficeReviews(String search, String sortBy, boolean ascending, int page, int pageSize, User actor) {
        enforceAdmin(actor);
        try {
            int safePage = Math.max(1, page);
            int safePageSize = Math.max(1, Math.min(100, pageSize));
            int offset = (safePage - 1) * safePageSize;
            return reviewRepository.findForBackOffice(search, sortBy, ascending, offset, safePageSize);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to load reviews: " + e.getMessage(), e);
        }
    }

    public int countBackOfficeReviews(String search, User actor) {
        enforceAdmin(actor);
        try {
            return reviewRepository.countForBackOffice(search);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to count reviews: " + e.getMessage(), e);
        }
    }

    public void deleteReview(int reviewId, User actor) {
        enforceAdmin(actor);
        if (reviewId <= 0) {
            throw new EventModuleException("Invalid review id.");
        }
        try {
            reviewRepository.deleteById(reviewId);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to delete review: " + e.getMessage(), e);
        }
    }

    private void validatePayload(int userId, int eventId, int rating) {
        if (userId <= 0 || eventId <= 0) {
            throw new EventModuleException("Invalid review payload.");
        }
        if (rating < 1 || rating > 5) {
            throw new EventModuleException("Rating must be between 1 and 5.");
        }
    }

    private void enforceEligibility(int userId, int eventId) throws SQLException {
        Event event = eventService.getById(eventId);
        if (event == null) {
            throw new EventModuleException("Event not found.");
        }

        Optional<Reservation> reservation = reservationRepository.findByUserAndEvent(userId, eventId);
        if (reservation.isEmpty() || reservation.get().getStatus() != ReservationStatus.ACCEPTED) {
            throw new EventModuleException("Only users with ACCEPTED reservation can review this event.");
        }

        if (event.getEndDate() == null || event.getEndDate().after(new Timestamp(System.currentTimeMillis()))) {
            throw new EventModuleException("Review is allowed only after event date has passed.");
        }
    }

    private void enforceAdmin(User actor) {
        if (actor == null || !actor.isAdmin()) {
            throw new EventModuleException("Admin privileges required.");
        }
    }
}
