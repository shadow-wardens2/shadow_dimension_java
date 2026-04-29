package Services.Artworks;

import Entities.Artworks.Categories;
import Interfaces.InterfaceServiceArtworks;
import Utils.ShadowDimensionsDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceCategories implements InterfaceServiceArtworks<Categories> {
    private Connection cnx;

    public ServiceCategories() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(Categories c) throws SQLException {
        String req = "INSERT INTO category(name, description) VALUES (?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, c.getTitle());
        ps.setString(2, c.getDescription());
        ps.executeUpdate();
    }

    @Override
    public void update(Categories c) throws SQLException {
        String req = "UPDATE category SET name=?, description=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, c.getTitle());
        ps.setString(2, c.getDescription());
        ps.setString(3, c.getID());
        ps.executeUpdate();
    }

    @Override
    public void delete(Categories c) throws SQLException {
        String req = "DELETE FROM category WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, c.getID());
        ps.executeUpdate();
    }

    @Override
    public List<Categories> getAll() throws SQLException {
        List<Categories> list = new ArrayList<>();
        String req = "SELECT * FROM category";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            Categories c = new Categories();
            c.setID(rs.getString("id"));
            try {
                c.setTitle(rs.getString("name"));
            } catch (SQLException e) {
                // If the column 'name' is not found, fallback to 'title'
                c.setTitle(rs.getString("title"));
            }
            c.setDescription(rs.getString("description"));
            list.add(c);
        }
        return list;
    }
}
