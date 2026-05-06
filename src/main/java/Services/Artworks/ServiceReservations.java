package Services.Artworks;

import Utils.ShadowDimensionsDB;
import java.sql.*;

public class ServiceReservations {
    private Connection cnx;

    public ServiceReservations() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    public boolean exists(int artworkId, String email) throws SQLException {
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
        String req = "INSERT INTO reservations (artwork_id, email) VALUES (?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, artworkId);
        ps.setString(2, email);
        ps.executeUpdate();
    }
}
