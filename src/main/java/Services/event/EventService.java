package Services.event;

import Entities.event.Category;
import Entities.event.Event;
import Interfaces.InterfaceServiceProduit;
import Utils.ShadowDimensionsDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EventService implements InterfaceServiceProduit<Event> {

    private final Connection cnx;

    public EventService() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(Event e) throws SQLException {
        String req = "INSERT INTO evt_event(title, description, location, start_date, end_date, image, capacity, qr_code_path, created_at, status, category_id, created_by_id, visual_vibe, location_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, e.getTitle());
        ps.setString(2, e.getDescription());
        ps.setString(3, e.getLocation());
        ps.setTimestamp(4, e.getStartDate());
        ps.setTimestamp(5, e.getEndDate());
        ps.setString(6, e.getImage());
        ps.setInt(7, e.getCapacity());
        if (e.getQrCodePath() == null || e.getQrCodePath().isBlank()) {
            ps.setNull(8, java.sql.Types.VARCHAR);
        } else {
            ps.setString(8, e.getQrCodePath());
        }
        ps.setTimestamp(9, e.getCreatedAt() != null ? e.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
        ps.setString(10, e.getStatus());
        ps.setInt(11, e.getCategory().getId());
        ps.setInt(12, e.getCreatedById());
        ps.setString(13, e.getVisualVibe());
        ps.setString(14, e.getLocationType());
        ps.executeUpdate();
    }

    @Override
    public void update(Event e) throws SQLException {
        String req = "UPDATE evt_event SET title=?, description=?, location=?, start_date=?, end_date=?, image=?, capacity=?, qr_code_path=?, status=?, category_id=?, created_by_id=?, visual_vibe=?, location_type=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, e.getTitle());
        ps.setString(2, e.getDescription());
        ps.setString(3, e.getLocation());
        ps.setTimestamp(4, e.getStartDate());
        ps.setTimestamp(5, e.getEndDate());
        ps.setString(6, e.getImage());
        ps.setInt(7, e.getCapacity());
        if (e.getQrCodePath() == null || e.getQrCodePath().isBlank()) {
            ps.setNull(8, java.sql.Types.VARCHAR);
        } else {
            ps.setString(8, e.getQrCodePath());
        }
        ps.setString(9, e.getStatus());
        ps.setInt(10, e.getCategory().getId());
        ps.setInt(11, e.getCreatedById());
        ps.setString(12, e.getVisualVibe());
        ps.setString(13, e.getLocationType());
        ps.setInt(14, e.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(Event e) throws SQLException {
        String req = "DELETE FROM evt_event WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, e.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Event> getAll() throws SQLException {
        List<Event> list = new ArrayList<>();
        String req = "SELECT e.*, c.nom AS category_name, c.description AS category_description, c.type_tarification, c.prix AS category_price, c.creator_type, c.created_at AS category_created_at FROM evt_event e JOIN evt_category c ON e.category_id = c.id ORDER BY e.id DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(mapEvent(rs));
        }
        return list;
    }

    public Event getById(int id) throws SQLException {
        String req = "SELECT e.*, c.nom AS category_name, c.description AS category_description, c.type_tarification, c.prix AS category_price, c.creator_type, c.created_at AS category_created_at FROM evt_event e JOIN evt_category c ON e.category_id = c.id WHERE e.id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapEvent(rs);
        }
        return null;
    }

    public Map<String, Integer> getEventCountByCategory() throws SQLException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String req = "SELECT c.nom AS category_name, COUNT(e.id) AS total FROM evt_category c LEFT JOIN evt_event e ON e.category_id = c.id GROUP BY c.id, c.nom ORDER BY total DESC, c.nom ASC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            counts.put(rs.getString("category_name"), rs.getInt("total"));
        }
        return counts;
    }

    private Event mapEvent(ResultSet rs) throws SQLException {
        Double categoryPrice = rs.getObject("category_price") != null ? rs.getDouble("category_price") : null;
        Category category = new Category(
                rs.getInt("category_id"),
                rs.getString("category_name"),
                rs.getString("category_description"),
                rs.getString("type_tarification"),
                categoryPrice,
                rs.getString("creator_type"),
                rs.getTimestamp("category_created_at")
        );

        return new Event(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("location"),
                rs.getTimestamp("start_date"),
                rs.getTimestamp("end_date"),
                rs.getString("image"),
                rs.getInt("capacity"),
                rs.getString("qr_code_path"),
                rs.getTimestamp("created_at"),
                rs.getString("status"),
                category,
                rs.getInt("created_by_id"),
                rs.getString("visual_vibe"),
                rs.getString("location_type")
        );
    }
}
