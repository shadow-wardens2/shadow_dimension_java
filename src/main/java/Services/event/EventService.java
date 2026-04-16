package Services.event;

// Imports the category model used when rebuilding joined records.
import Entities.event.Category;
// Imports the event model manipulated by this service.
import Entities.event.Event;
// Reuses the generic CRUD service contract already used in the project.
import Interfaces.InterfaceServiceProduit;
// Gives access to the singleton database connection provider.
import Utils.ShadowDimensionsDB;

// JDBC connection type.
import java.sql.Connection;
// JDBC prepared statement type for parameterized SQL.
import java.sql.PreparedStatement;
// JDBC result set type for reading query rows.
import java.sql.ResultSet;
// JDBC checked exception type.
import java.sql.SQLException;
// JDBC basic statement type.
import java.sql.Statement;
// JDBC timestamp type.
import java.sql.Timestamp;
// Dynamic list implementation.
import java.util.ArrayList;
// Keeps insertion order for aggregate map results.
import java.util.LinkedHashMap;
// List interface.
import java.util.List;
// Map interface.
import java.util.Map;

// Service responsible for CRUD and reporting operations on evt_event.
public class EventService implements InterfaceServiceProduit<Event> {

    // Holds a reusable database connection for this service instance.
    private final Connection cnx;

    // Constructor initializes connection through the shared DB singleton.
    public EventService() {
        // Fetches the unique configured JDBC connection.
        cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    // Persists a new event row in the database.
    @Override
    public void add(Event e) throws SQLException {
        // SQL insert statement covering all stored event columns.
        String req = "INSERT INTO evt_event(title, description, location, start_date, end_date, image, capacity, qr_code_path, created_at, status, category_id, created_by_id, visual_vibe, location_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        // Creates a prepared statement to safely bind event values.
        PreparedStatement ps = cnx.prepareStatement(req);
        // Binds event title.
        ps.setString(1, e.getTitle());
        // Binds event description.
        ps.setString(2, e.getDescription());
        // Binds event location.
        ps.setString(3, e.getLocation());
        // Binds start timestamp.
        ps.setTimestamp(4, e.getStartDate());
        // Binds end timestamp.
        ps.setTimestamp(5, e.getEndDate());
        // Binds image URL/path.
        ps.setString(6, e.getImage());
        // Binds maximum capacity.
        ps.setInt(7, e.getCapacity());
        // Handles nullable qr_code_path cleanly.
        if (e.getQrCodePath() == null || e.getQrCodePath().isBlank()) {
            // Stores NULL when QR code is absent.
            ps.setNull(8, java.sql.Types.VARCHAR);
        } else {
            // Stores explicit QR code path when provided.
            ps.setString(8, e.getQrCodePath());
        }
        // Uses provided creation date or defaults to current timestamp.
        ps.setTimestamp(9, e.getCreatedAt() != null ? e.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
        // Binds workflow status.
        ps.setString(10, e.getStatus());
        // Binds foreign key to event category.
        ps.setInt(11, e.getCategory().getId());
        // Binds creator user id.
        ps.setInt(12, e.getCreatedById());
        // Binds visual vibe value (nullable by business choice).
        ps.setString(13, e.getVisualVibe());
        // Binds indoor/outdoor location type.
        ps.setString(14, e.getLocationType());
        // Executes the insert against the database.
        ps.executeUpdate();
    }

    // Updates an existing event row by id.
    @Override
    public void update(Event e) throws SQLException {
        // SQL update statement for mutable columns.
        String req = "UPDATE evt_event SET title=?, description=?, location=?, start_date=?, end_date=?, image=?, capacity=?, qr_code_path=?, status=?, category_id=?, created_by_id=?, visual_vibe=?, location_type=? WHERE id=?";
        // Creates the prepared statement for secure parameter binding.
        PreparedStatement ps = cnx.prepareStatement(req);
        // Binds title.
        ps.setString(1, e.getTitle());
        // Binds description.
        ps.setString(2, e.getDescription());
        // Binds location.
        ps.setString(3, e.getLocation());
        // Binds start date.
        ps.setTimestamp(4, e.getStartDate());
        // Binds end date.
        ps.setTimestamp(5, e.getEndDate());
        // Binds image path/URL.
        ps.setString(6, e.getImage());
        // Binds capacity.
        ps.setInt(7, e.getCapacity());
        // Handles nullable qr_code_path on update.
        if (e.getQrCodePath() == null || e.getQrCodePath().isBlank()) {
            // Writes NULL when no QR code exists.
            ps.setNull(8, java.sql.Types.VARCHAR);
        } else {
            // Writes given QR code value.
            ps.setString(8, e.getQrCodePath());
        }
        // Binds status.
        ps.setString(9, e.getStatus());
        // Binds category id.
        ps.setInt(10, e.getCategory().getId());
        // Binds created-by id.
        ps.setInt(11, e.getCreatedById());
        // Binds visual vibe.
        ps.setString(12, e.getVisualVibe());
        // Binds location type.
        ps.setString(13, e.getLocationType());
        // Binds row id target in WHERE clause.
        ps.setInt(14, e.getId());
        // Executes update command.
        ps.executeUpdate();
    }

    // Removes an event row by id.
    @Override
    public void delete(Event e) throws SQLException {
        // SQL delete command targeting one id.
        String req = "DELETE FROM evt_event WHERE id=?";
        // Creates prepared statement for deletion.
        PreparedStatement ps = cnx.prepareStatement(req);
        // Binds event id to delete.
        ps.setInt(1, e.getId());
        // Executes delete.
        ps.executeUpdate();
    }

    // Returns all events joined with their category data.
    @Override
    public List<Event> getAll() throws SQLException {
        // Container that will be returned to caller.
        List<Event> list = new ArrayList<>();
        // Query joins category fields so UI does not need extra requests.
        String req = "SELECT e.*, c.nom AS category_name, c.description AS category_description, c.type_tarification, c.prix AS category_price, c.creator_type, c.created_at AS category_created_at FROM evt_event e JOIN evt_category c ON e.category_id = c.id ORDER BY e.id DESC";
        // Creates basic statement for full list query.
        Statement st = cnx.createStatement();
        // Executes query and receives cursor-like result set.
        ResultSet rs = st.executeQuery(req);
        // Iterates all rows.
        while (rs.next()) {
            // Converts current row into Event entity and appends it.
            list.add(mapEvent(rs));
        }
        // Returns fully mapped list.
        return list;
    }

    // Returns one event by id including category details.
    public Event getById(int id) throws SQLException {
        // Parameterized select for single row lookup.
        String req = "SELECT e.*, c.nom AS category_name, c.description AS category_description, c.type_tarification, c.prix AS category_price, c.creator_type, c.created_at AS category_created_at FROM evt_event e JOIN evt_category c ON e.category_id = c.id WHERE e.id=?";
        // Creates prepared statement for id lookup.
        PreparedStatement ps = cnx.prepareStatement(req);
        // Binds requested id.
        ps.setInt(1, id);
        // Executes query.
        ResultSet rs = ps.executeQuery();
        // Returns mapped row when found.
        if (rs.next()) {
            // Maps and returns a single event entity.
            return mapEvent(rs);
        }
        // Returns null when no event exists for id.
        return null;
    }

    // Returns aggregated number of events grouped by category name.
    public Map<String, Integer> getEventCountByCategory() throws SQLException {
        // LinkedHashMap preserves SQL ordering for predictable chart rendering.
        Map<String, Integer> counts = new LinkedHashMap<>();
        // LEFT JOIN keeps categories even when they currently have zero events.
        String req = "SELECT c.nom AS category_name, COUNT(e.id) AS total FROM evt_category c LEFT JOIN evt_event e ON e.category_id = c.id GROUP BY c.id, c.nom ORDER BY total DESC, c.nom ASC";
        // Creates statement for aggregate query.
        Statement st = cnx.createStatement();
        // Executes aggregate query.
        ResultSet rs = st.executeQuery(req);
        // Reads each grouped row.
        while (rs.next()) {
            // Stores category name and total events for that category.
            counts.put(rs.getString("category_name"), rs.getInt("total"));
        }
        // Returns aggregation map.
        return counts;
    }

    // Internal helper that maps one SQL row into Event + nested Category objects.
    private Event mapEvent(ResultSet rs) throws SQLException {
        // Reads nullable category_price as Double instead of primitive 0.0 fallback.
        Double categoryPrice = rs.getObject("category_price") != null ? rs.getDouble("category_price") : null;
        // Builds category object from aliased joined columns.
        Category category = new Category(
                rs.getInt("category_id"),
                rs.getString("category_name"),
                rs.getString("category_description"),
                rs.getString("type_tarification"),
                categoryPrice,
                rs.getString("creator_type"),
                rs.getTimestamp("category_created_at")
        );

            // Builds and returns full event object from current result row.
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
