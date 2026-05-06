package Repositories.event;

import Entities.event.Reservation;
import Entities.event.ReservationStatus;
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

public class JdbcReservationRepository implements ReservationRepository {

    private final Connection cnx;

    public JdbcReservationRepository() {
        this(ShadowDimensionsDB.getInstance().getConnection());
    }

    public JdbcReservationRepository(Connection cnx) {
        this.cnx = cnx;
    }

    @Override
    public Optional<Reservation> findByUserAndEvent(int userId, int eventId) throws SQLException {
        String sql = "SELECT r.id AS reservation_id, r.user_id, r.event_id, r.status, r.reserved_at, r.qr_code_checked, u.username, u.email, u.phone, e.title AS event_title, e.start_date, e.end_date "
                + "FROM evt_reservation r "
                + "JOIN `user` u ON u.id = r.user_id "
                + "JOIN evt_event e ON e.id = r.event_id "
                + "WHERE r.user_id = ? AND r.event_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Reservation> findById(int reservationId) throws SQLException {
        String sql = "SELECT r.id AS reservation_id, r.user_id, r.event_id, r.status, r.reserved_at, r.qr_code_checked, u.username, u.email, u.phone, e.title AS event_title, e.start_date, e.end_date "
                + "FROM evt_reservation r "
                + "JOIN `user` u ON u.id = r.user_id "
                + "JOIN evt_event e ON e.id = r.event_id "
                + "WHERE r.id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Reservation createPending(int userId, int eventId) throws SQLException {
        String sql = "INSERT INTO evt_reservation(user_id, event_id, status, reserved_at, qr_code_checked) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setInt(2, eventId);
            ps.setString(3, ReservationStatus.PENDING.name());
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.setBoolean(5, false);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int reservationId = keys.getInt(1);
                    return findById(reservationId).orElseThrow(() -> new SQLException("Reservation creation failed."));
                }
            }
        }
        throw new SQLException("Reservation creation failed (no generated key)." );
    }

    @Override
    public void updateStatus(int reservationId, ReservationStatus status) throws SQLException {
        String sql = "UPDATE evt_reservation SET status = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, reservationId);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int reservationId) throws SQLException {
        String sql = "DELETE FROM evt_reservation WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Reservation> findAcceptedByUser(int userId) throws SQLException {
        String sql = "SELECT r.id AS reservation_id, r.user_id, r.event_id, r.status, r.reserved_at, r.qr_code_checked, u.username, u.email, u.phone, e.title AS event_title, e.start_date, e.end_date "
                + "FROM evt_reservation r "
                + "JOIN `user` u ON u.id = r.user_id "
                + "JOIN evt_event e ON e.id = r.event_id "
                + "WHERE r.user_id = ? AND r.status = ? "
                + "ORDER BY r.reserved_at DESC";

        List<Reservation> rows = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, ReservationStatus.ACCEPTED.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapReservation(rs));
                }
            }
        }
        return rows;
    }

    @Override
    public List<Reservation> findByEvent(int eventId) throws SQLException {
        String sql = "SELECT r.id AS reservation_id, r.user_id, r.event_id, r.status, r.reserved_at, r.qr_code_checked, u.username, u.email, u.phone, e.title AS event_title, e.start_date, e.end_date "
                + "FROM evt_reservation r "
                + "JOIN `user` u ON u.id = r.user_id "
                + "JOIN evt_event e ON e.id = r.event_id "
                + "WHERE r.event_id = ? "
                + "ORDER BY r.reserved_at DESC";

        List<Reservation> rows = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapReservation(rs));
                }
            }
        }
        return rows;
    }

    @Override
    public List<Reservation> findForBackOffice(String search, String sortBy, boolean ascending, int offset, int limit) throws SQLException {
        String normalizedSort = normalizeSortBy(sortBy);
        String direction = ascending ? "ASC" : "DESC";

        String sql = "SELECT r.id AS reservation_id, r.user_id, r.event_id, r.status, r.reserved_at, r.qr_code_checked, u.username, u.email, u.phone, e.title AS event_title, e.start_date, e.end_date "
                + "FROM evt_reservation r "
                + "JOIN `user` u ON u.id = r.user_id "
                + "JOIN evt_event e ON e.id = r.event_id "
                + "WHERE (? = '' OR LOWER(u.username) LIKE ? OR LOWER(u.email) LIKE ? OR LOWER(e.title) LIKE ? OR LOWER(r.status) LIKE ?) "
                + "ORDER BY " + normalizedSort + " " + direction + " LIMIT ? OFFSET ?";

        String token = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String like = "%" + token + "%";

        List<Reservation> rows = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);
            ps.setInt(6, limit);
            ps.setInt(7, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapReservation(rs));
                }
            }
        }
        return rows;
    }

    @Override
    public int countForBackOffice(String search) throws SQLException {
        String sql = "SELECT COUNT(*) FROM evt_reservation r "
                + "JOIN `user` u ON u.id = r.user_id "
                + "JOIN evt_event e ON e.id = r.event_id "
                + "WHERE (? = '' OR LOWER(u.username) LIKE ? OR LOWER(u.email) LIKE ? OR LOWER(e.title) LIKE ? OR LOWER(r.status) LIKE ?)";

        String token = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String like = "%" + token + "%";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null) {
            return "r.id";
        }

        return switch (sortBy) {
            case "username" -> "u.username";
            case "eventTitle" -> "e.title";
            case "status" -> "r.status";
            case "reservedAt" -> "r.reserved_at";
            default -> "r.id";
        };
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation row = new Reservation(
                rs.getInt("reservation_id"),
                rs.getInt("user_id"),
                rs.getInt("event_id"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("reserved_at"),
                rs.getBoolean("qr_code_checked")
        );
        row.setUsername(rs.getString("username"));
        row.setUserEmail(rs.getString("email"));
        row.setUserPhone(rs.getString("phone"));
        row.setEventTitle(rs.getString("event_title"));
        row.setEventStartDate(rs.getTimestamp("start_date"));
        row.setEventEndDate(rs.getTimestamp("end_date"));
        return row;
    }
}
