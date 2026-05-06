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
        if (cnx == null) throw new SQLException("Database connection is null");
        String req = "INSERT INTO artwork(title, description, price, imageurl, pdf_url, ai_summary, status, category_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, a.getTitle());
        ps.setString(2, a.getDescription());
        ps.setInt(3, a.getPrice());
        ps.setString(4, a.getImageurl());
        ps.setString(5, a.getPdfUrl());
        ps.setString(6, a.getAiSummary());
        ps.setString(7, a.getStatus());
        ps.setInt(8, a.getCategoryID());
        ps.executeUpdate();
    }

    @Override
    public void update(Artworks a) throws SQLException {
        if (cnx == null) throw new SQLException("Database connection is null");
        String req = "UPDATE artwork SET title=?, description=?, price=?, imageurl=?, pdf_url=?, ai_summary=?, status=?, category_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, a.getTitle());
        ps.setString(2, a.getDescription());
        ps.setInt(3, a.getPrice());
        ps.setString(4, a.getImageurl());
        ps.setString(5, a.getPdfUrl());
        ps.setString(6, a.getAiSummary());
        ps.setString(7, a.getStatus());
        ps.setInt(8, a.getCategoryID());
        ps.setInt(9, a.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(Artworks a) throws SQLException {
        if (cnx == null) throw new SQLException("Database connection is null");
        String req = "DELETE FROM artwork WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, a.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Artworks> getAll() throws SQLException {
        if (cnx == null) throw new SQLException("Database connection is null");
        List<Artworks> list = new ArrayList<>();
        String req = "SELECT * FROM artwork";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        
        // Debug: Print available columns
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        System.out.print("Available columns: ");
        for (int i = 1; i <= columnCount; i++) {
            System.out.print(metaData.getColumnName(i) + (i < columnCount ? ", " : ""));
        }
        System.out.println();

        while (rs.next()) {
            Artworks a = new Artworks();
            a.setId(rs.getInt("id"));
            a.setTitle(rs.getString("title"));
            a.setDescription(rs.getString("description"));
            a.setPrice(rs.getInt("price"));
            a.setImageurl(rs.getString("imageurl"));
            a.setPdfUrl(rs.getString("pdf_url"));
            a.setAiSummary(rs.getString("ai_summary"));
            a.setStatus(rs.getString("status"));
            a.setCategoryID(rs.getInt("category_id"));
            list.add(a);
        }
        return list;
    }
}
