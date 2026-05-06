package Services.Forum;

import Entities.Forum.ForumCategory;
import Interfaces.InterfaceServiceProduit;
import Utils.ShadowDimensionsDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryService implements InterfaceServiceProduit<ForumCategory> {

    private final Connection cnx;

    public CategoryService() {
        this.cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(ForumCategory fc) throws SQLException {
        String sql = "INSERT INTO forum_category (name, slug, description, color) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, fc.getName());
        ps.setString(2, fc.getSlug());
        ps.setString(3, fc.getDescription());
        ps.setString(4, fc.getColor());
        ps.executeUpdate();
    }

    @Override
    public void update(ForumCategory fc) throws SQLException {
        String sql = "UPDATE forum_category SET name=?, slug=?, description=?, color=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, fc.getName());
        ps.setString(2, fc.getSlug());
        ps.setString(3, fc.getDescription());
        ps.setString(4, fc.getColor());
        ps.setInt(5, fc.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(ForumCategory fc) throws SQLException {
        String sql = "DELETE FROM forum_category WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, fc.getId());
        ps.executeUpdate();
    }

    @Override
    public List<ForumCategory> getAll() throws SQLException {
        List<ForumCategory> list = new ArrayList<>();
        String sql = "SELECT * FROM forum_category";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(new ForumCategory(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("description"),
                rs.getString("color")
            ));
        }
        return list;
    }
}
