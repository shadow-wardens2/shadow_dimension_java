package Services.event;

import Entities.event.EventReservation;
import Utils.ShadowDimensionsDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EventReservationService {

    private final Connection cnx;

    public EventReservationService() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
        ensureReservationTable();
    }

    public void reserveSpot(int userId, int eventId) throws SQLException {
        if (hasActiveReservation(userId, eventId)) {
            throw new IllegalArgumentException("Vous avez deja une reservation en attente ou acceptee pour cet evenement.");
        }

        if (isEventStarted(eventId)) {
            throw new IllegalArgumentException("Cet evenement a deja commence. Reservation indisponible.");
        }

        int capacity = getEventCapacity(eventId);
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacite invalide pour cet evenement.");
        }

        int reservedCount = countReservedSlots(eventId);
        if (reservedCount >= capacity) {
            throw new IllegalArgumentException("Aucune place disponible pour cet evenement.");
        }

        String req = "INSERT INTO evt_reservation(status, reserved_at, qr_code_checked, user_id, event_id) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, "PENDING");
        ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        ps.setInt(3, 0);
        ps.setInt(4, userId);
        ps.setInt(5, eventId);
        ps.executeUpdate();
    }

    public List<EventReservation> getAllWithDetails() throws SQLException {
        List<EventReservation> list = new ArrayList<>();
        String req = "SELECT r.*, u.username, u.email, e.title AS event_title, e.start_date AS event_start_date "
                + "FROM evt_reservation r "
                + "JOIN `user` u ON u.id = r.user_id "
                + "JOIN evt_event e ON e.id = r.event_id "
                + "ORDER BY r.id DESC";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(mapReservation(rs));
        }
        return list;
    }

    public boolean hasActiveReservation(int userId, int eventId) throws SQLException {
        String req = "SELECT COUNT(*) AS total FROM evt_reservation WHERE user_id = ? AND event_id = ? AND status IN ('PENDING', 'ACCEPTED')";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ps.setInt(2, eventId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("total") > 0;
        }
        return false;
    }

    public void updateStatus(int reservationId, String newStatus) throws SQLException {
        if (!"PENDING".equals(newStatus) && !"ACCEPTED".equals(newStatus) && !"REFUSED".equals(newStatus)) {
            throw new IllegalArgumentException("Statut de reservation invalide.");
        }

        String req = "UPDATE evt_reservation SET status=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, newStatus);
        ps.setInt(2, reservationId);
        ps.executeUpdate();
    }

    public int countReservedSlots(int eventId) throws SQLException {
        String req = "SELECT COUNT(*) AS total FROM evt_reservation WHERE event_id = ? AND status IN ('PENDING', 'ACCEPTED')";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, eventId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("total");
        }
        return 0;
    }

    private int getEventCapacity(int eventId) throws SQLException {
        String req = "SELECT capacity FROM evt_event WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, eventId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("capacity");
        }
        return 0;
    }

    private boolean isEventStarted(int eventId) throws SQLException {
        String req = "SELECT start_date FROM evt_event WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, eventId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Timestamp startDate = rs.getTimestamp("start_date");
            return startDate != null && startDate.before(new Timestamp(System.currentTimeMillis()));
        }
        return false;
    }

    private EventReservation mapReservation(ResultSet rs) throws SQLException {
        return new EventReservation(
                rs.getInt("id"),
                rs.getString("status"),
                rs.getTimestamp("reserved_at"),
                rs.getInt("qr_code_checked") == 1,
                rs.getInt("user_id"),
                rs.getInt("event_id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("event_title"),
                rs.getTimestamp("event_start_date")
        );
    }

    private void ensureReservationTable() {
        String req = "CREATE TABLE IF NOT EXISTS evt_reservation ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', "
                + "reserved_at DATETIME NOT NULL, "
                + "qr_code_checked TINYINT(1) NOT NULL DEFAULT 0, "
                + "user_id INT NOT NULL, "
                + "event_id INT NOT NULL, "
                + "INDEX idx_evt_reservation_user_id (user_id), "
                + "INDEX idx_evt_reservation_event_id (event_id), "
                + "CONSTRAINT fk_evt_reservation_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE, "
                + "CONSTRAINT fk_evt_reservation_event FOREIGN KEY (event_id) REFERENCES evt_event(id) ON DELETE CASCADE"
                + ")";

        try (Statement st = cnx.createStatement()) {
            st.execute(req);
        } catch (SQLException ignored) {
            // Keep app startup resilient if table migration cannot run automatically.
        }
    }
}
