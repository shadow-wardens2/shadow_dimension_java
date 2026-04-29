package Services.Artworks;

import Entities.Artworks.Artworks;
import Interfaces.InterfaceServiceArtworks;
import Utils.ShadowDimensionsDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceArtworks implements InterfaceServiceArtworks<Artworks> {

    private Connection cnx;

    public ServiceArtworks() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(Artworks a) throws SQLException {
        String req = "INSERT INTO artworks(title, description, price, imageurl, status, category_id) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, a.getTitle());
        ps.setString(2, a.getDescription());
        ps.setInt(3, a.getPrice());
        ps.setString(4, a.getImageurl());
        ps.setString(5, a.getStatus());
        ps.setInt(6, a.getCategoryID());
        ps.executeUpdate();
    }

    @Override
    public void update(Artworks a) throws SQLException {
        String req = "UPDATE artworks SET title=?, description=?, price=?, imageurl=?, status=?, category_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, a.getTitle());
        ps.setString(2, a.getDescription());
        ps.setInt(3, a.getPrice());
        ps.setString(4, a.getImageurl());
        ps.setString(5, a.getStatus());
        ps.setInt(6, a.getCategoryID());
        ps.setInt(7, a.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(Artworks a) throws SQLException {
        String req = "DELETE FROM artworks WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, a.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Artworks> getAll() throws SQLException {
        List<Artworks> list = new ArrayList<>();
        String req = "SELECT * FROM artworks";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            Artworks a = new Artworks();
            a.setId(rs.getInt("id"));
            a.setTitle(rs.getString("title"));
            a.setDescription(rs.getString("description"));
            a.setPrice(rs.getInt("price"));
            a.setImageurl(rs.getString("imageurl"));
            a.setStatus(rs.getString("status"));
            a.setCategoryID(rs.getInt("category_id"));
            list.add(a);
        }
        return list;
    }
}
