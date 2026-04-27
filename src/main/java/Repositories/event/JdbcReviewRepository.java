package Repositories.event;

import Entities.event.EventRatingSummary;
import Entities.event.Review;
import Utils.ShadowDimensionsDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class JdbcReviewRepository implements ReviewRepository {

    private final Connection cnx;

    public JdbcReviewRepository() {
        this(ShadowDimensionsDB.getInstance().getConnection());
    }

    public JdbcReviewRepository(Connection cnx) {
        this.cnx = cnx;
    }

    @Override
    public Optional<Review> findByUserAndEvent(int userId, int eventId) throws SQLException {
        String sql = "SELECT r.*, u.username, e.title AS event_title FROM evt_review r "
                + "JOIN `user` u ON u.id = r.user_id "
                + "JOIN evt_event e ON e.id = r.event_id "
                + "WHERE r.user_id = ? AND r.event_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReview(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Review create(Review review) throws SQLException {
        String sql = "INSERT INTO evt_review(user_id, event_id, rating, comment, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, review.getUserId());
            ps.setInt(2, review.getEventId());
            ps.setInt(3, review.getRating());
            ps.setString(4, review.getComment());
            ps.setTimestamp(5, review.getCreatedAt() == null ? new Timestamp(System.currentTimeMillis()) : review.getCreatedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    review.setId(keys.getInt(1));
                }
            }
        }
        return findByUserAndEvent(review.getUserId(), review.getEventId())
                .orElseThrow(() -> new SQLException("Review creation failed."));
    }

    @Override
    public void update(Review review) throws SQLException {
        String sql = "UPDATE evt_review SET rating = ?, comment = ?, created_at = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, review.getRating());
            ps.setString(2, review.getComment());
            ps.setTimestamp(3, review.getCreatedAt() == null ? new Timestamp(System.currentTimeMillis()) : review.getCreatedAt());
            ps.setInt(4, review.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Review> findByEventId(int eventId) throws SQLException {
        String sql = "SELECT r.*, u.username, e.title AS event_title FROM evt_review r "
                + "JOIN `user` u ON u.id = r.user_id "
                + "JOIN evt_event e ON e.id = r.event_id "
                + "WHERE r.event_id = ? ORDER BY r.created_at DESC";
        List<Review> rows = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapReview(rs));
                }
            }
        }
        return rows;
    }

    @Override
    public List<Review> findForBackOffice(String search, String sortBy, boolean ascending, int offset, int limit) throws SQLException {
        String token = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String like = "%" + token + "%";

        String sql = "SELECT r.*, u.username, e.title AS event_title FROM evt_review r "
                + "JOIN `user` u ON u.id = r.user_id "
                + "JOIN evt_event e ON e.id = r.event_id "
                + "WHERE (? = '' OR LOWER(u.username) LIKE ? OR LOWER(e.title) LIKE ? OR LOWER(r.comment) LIKE ?) "
                + "ORDER BY " + normalizeSortBy(sortBy) + " " + (ascending ? "ASC" : "DESC") + " LIMIT ? OFFSET ?";

        List<Review> rows = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setInt(5, limit);
            ps.setInt(6, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapReview(rs));
                }
            }
        }
        return rows;
    }

    @Override
    public int countForBackOffice(String search) throws SQLException {
        String token = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String like = "%" + token + "%";
        String sql = "SELECT COUNT(*) FROM evt_review r "
                + "JOIN `user` u ON u.id = r.user_id "
                + "JOIN evt_event e ON e.id = r.event_id "
                + "WHERE (? = '' OR LOWER(u.username) LIKE ? OR LOWER(e.title) LIKE ? OR LOWER(r.comment) LIKE ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public void deleteById(int reviewId) throws SQLException {
        String sql = "DELETE FROM evt_review WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.executeUpdate();
        }
    }

    @Override
    public EventRatingSummary getEventRatingSummary(int eventId) throws SQLException {
        String sql = "SELECT COALESCE(AVG(rating), 0) AS avg_rating, COUNT(*) AS total FROM evt_review WHERE event_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new EventRatingSummary(eventId, rs.getDouble("avg_rating"), rs.getInt("total"));
                }
            }
        }
        return new EventRatingSummary(eventId, 0, 0);
    }

    private Review mapReview(ResultSet rs) throws SQLException {
        Review row = new Review(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("event_id"),
                rs.getInt("rating"),
                rs.getString("comment"),
                rs.getTimestamp("created_at")
        );
        row.setUsername(rs.getString("username"));
        row.setEventTitle(readStringColumn(rs, "event_title"));
        return row;
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null) {
            return "r.id";
        }
        return switch (sortBy) {
            case "username" -> "u.username";
            case "eventTitle" -> "e.title";
            case "rating" -> "r.rating";
            case "createdAt" -> "r.created_at";
            default -> "r.id";
        };
    }

    private String readStringColumn(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
    }
}
