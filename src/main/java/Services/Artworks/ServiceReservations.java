package Services.Artworks;

import Utils.ShadowDimensionsDB;
import java.sql.*;

public class ServiceReservations {
    private Connection cnx;

    public ServiceReservations() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        if (cnx == null) return;
        String sql = "CREATE TABLE IF NOT EXISTS reservations (" +
                     "id INT AUTO_INCREMENT PRIMARY KEY," +
                     "artwork_id INT," +
                     "email VARCHAR(255)," +
                     "reserved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                     "FOREIGN KEY (artwork_id) REFERENCES artwork(id) ON DELETE CASCADE" +
                     ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating reservations table: " + e.getMessage());
        }
    }

    public boolean exists(int artworkId, String email) throws SQLException {
        if (cnx == null) throw new SQLException("Database connection is null");
        String req = "SELECT COUNT(*) FROM reservations WHERE artwork_id = ? AND email = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, artworkId);
        ps.setString(2, email);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    public void add(int artworkId, String email) throws SQLException {
        if (cnx == null) throw new SQLException("Database connection is null");
        String req = "INSERT INTO reservations (artwork_id, email) VALUES (?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, artworkId);
        ps.setString(2, email);
        ps.executeUpdate();
    }
}
