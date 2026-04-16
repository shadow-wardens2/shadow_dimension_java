package Services.event;

import Entities.event.Category;
import Interfaces.InterfaceServiceProduit;
import Utils.ShadowDimensionsDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CategoryService implements InterfaceServiceProduit<Category> {

    private final Connection cnx;

    public CategoryService() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(Category c) throws SQLException {
        String req = "INSERT INTO evt_category(nom, description, type_tarification, prix, creator_type, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, c.getNom());
        ps.setString(2, c.getDescription());
        ps.setString(3, c.getTypeTarification());
        if (c.getPrix() == null) {
            ps.setNull(4, java.sql.Types.DECIMAL);
        } else {
            ps.setDouble(4, c.getPrix());
        }
        String creatorType = (c.getCreatorType() == null || c.getCreatorType().isBlank()) ? "SYSTEM" : c.getCreatorType();
        ps.setString(5, creatorType);
        ps.setTimestamp(6, c.getCreatedAt() != null ? c.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
        ps.executeUpdate();
    }

    @Override
    public void update(Category c) throws SQLException {
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
        String creatorType = (c.getCreatorType() == null || c.getCreatorType().isBlank()) ? "SYSTEM" : c.getCreatorType();
        ps.setString(5, creatorType);
        ps.setInt(6, c.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(Category c) throws SQLException {
        String req = "DELETE FROM evt_category WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, c.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Category> getAll() throws SQLException {
        List<Category> list = new ArrayList<>();
        String req = "SELECT * FROM evt_category ORDER BY id DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(mapCategory(rs));
        }
        return list;
    }

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

    private Category mapCategory(ResultSet rs) throws SQLException {
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
