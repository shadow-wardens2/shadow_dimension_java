package Services.Artworks;

import Entities.Artworks.Artworks;
import Interfaces.InterfaceServiceArtworks;
import Utils.ShadowDimensionsDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServiceArtworks implements InterfaceServiceArtworks<Artworks> {

    private Connection cnx;

    public ServiceArtworks() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    public ServiceArtworks(Connection cnx) {
        this.cnx = cnx;
    }

    @Override
    public void add(Artworks a) throws SQLException {
        if (cnx == null) throw new SQLException("Database connection is null");
        String req = "INSERT INTO artwork(title, description, price, imageurl, pdf_url, ai_summary, status, category_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
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

        ResultSetMetaData metaData = rs.getMetaData();
        Set<String> availableColumns = getAvailableColumns(metaData);

        while (rs.next()) {
            Artworks a = new Artworks();
            a.setId(getInt(rs, availableColumns, 0, "id"));
            a.setTitle(getString(rs, availableColumns, "title", "name"));
            a.setDescription(getString(rs, availableColumns, "description", "details"));
            a.setPrice(getInt(rs, availableColumns, 0, "price"));
            a.setImageurl(getString(rs, availableColumns, "imageurl", "image_url", "image_filename"));
            a.setPdfUrl(getString(rs, availableColumns, "pdf_url", "excerpt_pdf", "book_excerpt"));
            a.setAiSummary(getString(rs, availableColumns, "ai_summary", "chapter_title"));
            a.setStatus(getString(rs, availableColumns, "status"));
            a.setCategoryID(getInt(rs, availableColumns, 0, "category_id", "categoryID"));
            list.add(a);
        }
        return list;
    }

    private Set<String> getAvailableColumns(ResultSetMetaData metaData) throws SQLException {
        Set<String> availableColumns = new HashSet<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            availableColumns.add(metaData.getColumnLabel(i).toLowerCase());
            availableColumns.add(metaData.getColumnName(i).toLowerCase());
        }
        return availableColumns;
    }

    private String getString(ResultSet rs, Set<String> availableColumns, String... columnAliases) throws SQLException {
        for (String alias : columnAliases) {
            if (availableColumns.contains(alias.toLowerCase())) {
                return rs.getString(alias);
            }
        }
        return null;
    }

    private int getInt(ResultSet rs, Set<String> availableColumns, int defaultValue, String... columnAliases) throws SQLException {
        for (String alias : columnAliases) {
            if (availableColumns.contains(alias.toLowerCase())) {
                int value = rs.getInt(alias);
                return rs.wasNull() ? defaultValue : value;
            }
        }
        return defaultValue;
    }
}
