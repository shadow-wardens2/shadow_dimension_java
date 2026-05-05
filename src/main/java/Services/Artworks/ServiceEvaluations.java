package Services.Artworks;

import Entities.Artworks.Evaluation;
import Utils.ShadowDimensionsDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceEvaluations {
    private Connection cnx;

    public ServiceEvaluations() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS evaluations (" +
                     "id INT AUTO_INCREMENT PRIMARY KEY," +
                     "artwork_id INT," +
                     "user_id INT," +
                     "rating INT," +
                     "comment TEXT," +
                     "date_manifested TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                     "FOREIGN KEY (artwork_id) REFERENCES artworks(id) ON DELETE CASCADE" +
                     ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating evaluations table: " + e.getMessage());
        }
    }

    public void add(Evaluation e) throws SQLException {
        String req = "INSERT INTO evaluations (artwork_id, user_id, rating, comment, date_manifested) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, e.getArtworkId());
        ps.setInt(2, e.getUserId());
        ps.setInt(3, e.getRating());
        ps.setString(4, e.getComment());
        ps.executeUpdate();
    }

    public List<Evaluation> getByArtwork(int artworkId) throws SQLException {
        List<Evaluation> list = new ArrayList<>();
        String req = "SELECT * FROM evaluations WHERE artwork_id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, artworkId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new Evaluation(
                rs.getInt("id"),
                rs.getInt("artwork_id"),
                rs.getInt("user_id"),
                rs.getInt("rating"),
                rs.getString("comment"),
                rs.getString("date_manifested")
            ));
        }
        return list;
    }

    public double getAverageRating(int artworkId) throws SQLException {
        String req = "SELECT AVG(rating) as avg_rating FROM evaluations WHERE artwork_id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, artworkId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getDouble("avg_rating");
        }
        return 0;
    }
    public List<Evaluation> getAllWithArtworks() throws SQLException {
        List<Evaluation> list = new ArrayList<>();
        String req = "SELECT e.*, a.title as artwork_title FROM evaluations e JOIN artworks a ON e.artwork_id = a.id ORDER BY e.date_manifested DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            Evaluation e = new Evaluation(
                rs.getInt("id"),
                rs.getInt("artwork_id"),
                rs.getInt("user_id"),
                rs.getInt("rating"),
                rs.getString("comment"),
                rs.getString("date_manifested")
            );
            e.setArtworkTitle(rs.getString("artwork_title"));
            list.add(e);
        }
        return list;
    }

    public void delete(int id) throws SQLException {
        String req = "DELETE FROM evaluations WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
