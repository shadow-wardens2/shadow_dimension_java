package Repositories.event;

import Entities.event.EventReclamation;
import Entities.event.EventReclamationStatus;
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

public class JdbcEventReclamationRepository implements EventReclamationRepository {

    private final Connection cnx;

    public JdbcEventReclamationRepository() {
        this(ShadowDimensionsDB.getInstance().getConnection());
    }

    public JdbcEventReclamationRepository(Connection cnx) {
        this.cnx = cnx;
    }

    @Override
    public EventReclamation create(EventReclamation reclamation) throws SQLException {
        String sql = "INSERT INTO evt_reclamation(user_id, event_id, status, subject, message, ai_response, admin_response, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setInt(1, reclamation.getUserId());
            ps.setInt(2, reclamation.getEventId());
            ps.setString(3, (reclamation.getStatus() == null ? EventReclamationStatus.OPEN : reclamation.getStatus()).name());
            ps.setString(4, reclamation.getSubject());
            ps.setString(5, reclamation.getMessage());
            ps.setString(6, reclamation.getAiResponse());
            ps.setString(7, reclamation.getAdminResponse());
            ps.setTimestamp(8, reclamation.getCreatedAt() == null ? now : reclamation.getCreatedAt());
            ps.setTimestamp(9, reclamation.getUpdatedAt() == null ? now : reclamation.getUpdatedAt());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getInt(1)).orElseThrow(() -> new SQLException("Reclamation creation failed."));
                }
            }
        }
        throw new SQLException("Reclamation creation failed (no generated key).");
    }

    @Override
    public Optional<EventReclamation> findById(int reclamationId) throws SQLException {
        String sql = baseSelect() + " WHERE r.id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reclamationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReclamation(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<EventReclamation> findOpenByUserAndEvent(int userId, int eventId) throws SQLException {
        String sql = baseSelect() + " WHERE r.user_id = ? AND r.event_id = ? AND r.status IN ('OPEN', 'IN_PROGRESS', 'ESCALATED') ORDER BY r.created_at DESC LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReclamation(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<EventReclamation> findByUser(int userId) throws SQLException {
        String sql = baseSelect() + " WHERE r.user_id = ? ORDER BY r.created_at DESC";
        List<EventReclamation> rows = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapReclamation(rs));
                }
            }
        }
        return rows;
    }

    @Override
    public List<EventReclamation> findForBackOffice(String search, String status, String sortBy, boolean ascending, int offset, int limit) throws SQLException {
        String token = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String statusFilter = status == null ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
        String like = "%" + token + "%";
        String direction = ascending ? "ASC" : "DESC";

        String sql = baseSelect()
            + " WHERE (? = '' OR LOWER(u.username) LIKE ? OR LOWER(u.email) LIKE ? OR LOWER(e.title) LIKE ? OR LOWER(r.status) LIKE ? OR LOWER(r.subject) LIKE ? OR LOWER(r.message) LIKE ?)"
                + " AND (? = 'ALL' OR r.status = ?)"
                + " ORDER BY " + normalizeSortBy(sortBy) + " " + direction
                + " LIMIT ? OFFSET ?";

        List<EventReclamation> rows = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);
            ps.setString(6, like);
            ps.setString(7, like);
            ps.setString(8, statusFilter);
            ps.setString(9, statusFilter);
            ps.setInt(10, limit);
            ps.setInt(11, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapReclamation(rs));
                }
            }
        }
        return rows;
    }

    @Override
    public int countForBackOffice(String search, String status) throws SQLException {
        String token = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String statusFilter = status == null ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
        String like = "%" + token + "%";

        String sql = "SELECT COUNT(*) FROM evt_reclamation r "
                + "JOIN `user` u ON u.id = r.user_id "
                + "JOIN evt_event e ON e.id = r.event_id "
            + "WHERE (? = '' OR LOWER(u.username) LIKE ? OR LOWER(u.email) LIKE ? OR LOWER(e.title) LIKE ? OR LOWER(r.status) LIKE ? OR LOWER(r.subject) LIKE ? OR LOWER(r.message) LIKE ?) "
                + "AND (? = 'ALL' OR r.status = ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);
            ps.setString(6, like);
            ps.setString(7, like);
            ps.setString(8, statusFilter);
            ps.setString(9, statusFilter);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public void escalate(int reclamationId) throws SQLException {
        String sql = "UPDATE evt_reclamation SET status = 'ESCALATED', updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(1, now);
            ps.setInt(2, reclamationId);
            ps.executeUpdate();
        }
    }

    @Override
    public void adminRespond(int reclamationId, EventReclamationStatus status, String adminResponse) throws SQLException {
        String sql = "UPDATE evt_reclamation SET status = ?, admin_response = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setString(1, status.name());
            ps.setString(2, adminResponse);
            ps.setTimestamp(3, now);
            ps.setInt(4, reclamationId);
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteById(int reclamationId) throws SQLException {
        String sql = "DELETE FROM evt_reclamation WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reclamationId);
            ps.executeUpdate();
        }
    }

    private String baseSelect() {
        return "SELECT r.*, u.username, u.email, e.title AS event_title FROM evt_reclamation r "
                + "JOIN `user` u ON u.id = r.user_id "
                + "JOIN evt_event e ON e.id = r.event_id";
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null) {
            return "r.id";
        }
        return switch (sortBy) {
            case "username" -> "u.username";
            case "eventTitle" -> "e.title";
            case "status" -> "r.status";
            case "createdAt" -> "r.created_at";
            default -> "r.id";
        };
    }

    private EventReclamation mapReclamation(ResultSet rs) throws SQLException {
        EventReclamation row = new EventReclamation(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("event_id"),
                EventReclamationStatus.fromDatabase(rs.getString("status")),
            rs.getString("subject"),
            rs.getString("message"),
            rs.getString("ai_response"),
                rs.getString("admin_response"),
                rs.getTimestamp("created_at"),
            rs.getTimestamp("updated_at")
        );
        row.setUsername(rs.getString("username"));
        row.setUserEmail(rs.getString("email"));
        row.setEventTitle(rs.getString("event_title"));
        return row;
    }
}
