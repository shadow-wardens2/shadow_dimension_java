package Services.event;

// Category entity managed by this service.
import Entities.event.Category;
// Generic CRUD interface reused in project architecture.
import Interfaces.InterfaceServiceProduit;
// Shared DB singleton provider.
import Utils.ShadowDimensionsDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

// Service class responsible for evt_category CRUD operations.
public class CategoryService implements InterfaceServiceProduit<Category> {

    // Reusable JDBC connection.
    private final Connection cnx;

    // Constructor initializes DB connection through singleton provider.
    public CategoryService() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    // Inserts a new category row.
    @Override
    public void add(Category c) throws SQLException {
        // SQL insert statement for category table.
        String req = "INSERT INTO evt_category(nom, description, type_tarification, prix, creator_type, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        // Prepared statement to prevent SQL injection and simplify binding.
        PreparedStatement ps = cnx.prepareStatement(req);
        // Binds simple text fields.
        ps.setString(1, c.getNom());
        ps.setString(2, c.getDescription());
        ps.setString(3, c.getTypeTarification());
        // Handles nullable price for FREE categories.
        if (c.getPrix() == null) {
            ps.setNull(4, java.sql.Types.DECIMAL);
        } else {
            ps.setDouble(4, c.getPrix());
        }
        // Applies default creator type when empty.
        String creatorType = (c.getCreatorType() == null || c.getCreatorType().isBlank()) ? "SYSTEM" : c.getCreatorType();
        ps.setString(5, creatorType);
        // Uses provided timestamp or current time as fallback.
        ps.setTimestamp(6, c.getCreatedAt() != null ? c.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
        // Executes insertion.
        ps.executeUpdate();
    }

    // Updates an existing category row by id.
    @Override
    public void update(Category c) throws SQLException {
        // SQL update command.
        String req = "UPDATE evt_category SET nom=?, description=?, type_tarification=?, prix=?, creator_type=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, c.getNom());
        ps.setString(2, c.getDescription());
        ps.setString(3, c.getTypeTarification());
        if (c.getPrix() == null) {
            ps.setNull(4, java.sql.Types.DECIMAL);
        } else {
            ps.setDouble(4, c.getPrix());
        }
        // Keeps same default behavior for creator type.
        String creatorType = (c.getCreatorType() == null || c.getCreatorType().isBlank()) ? "SYSTEM" : c.getCreatorType();
        ps.setString(5, creatorType);
        // Binds target row id.
        ps.setInt(6, c.getId());
        // Executes update.
        ps.executeUpdate();
    }

    // Deletes one category row by id.
    @Override
    public void delete(Category c) throws SQLException {
        String req = "DELETE FROM evt_category WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, c.getId());
        ps.executeUpdate();
    }

    // Returns all categories ordered by newest first.
    @Override
    public List<Category> getAll() throws SQLException {
        // Output list.
        List<Category> list = new ArrayList<>();
        // Basic select query.
        String req = "SELECT * FROM evt_category ORDER BY id DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        // Maps each row to Category entity.
        while (rs.next()) {
            list.add(mapCategory(rs));
        }
        return list;
    }

    // Returns one category by id or null when missing.
    public Category getById(int id) throws SQLException {
        String req = "SELECT * FROM evt_category WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapCategory(rs);
        }
        return null;
    }

    // Internal mapper from ResultSet row to Category entity.
    private Category mapCategory(ResultSet rs) throws SQLException {
        // Preserves null price instead of default primitive 0.0.
        Double price = rs.getObject("prix") != null ? rs.getDouble("prix") : null;
        return new Category(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("description"),
                rs.getString("type_tarification"),
                price,
                rs.getString("creator_type"),
                rs.getTimestamp("created_at")
        );
    }
}
