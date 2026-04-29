package Services.Tutorials;

import Utils.ShadowDimensionsDB;
import java.sql.*;

public class ServiceLeconProgress {
    private final Connection connection;

    public ServiceLeconProgress() {
        this.connection = ShadowDimensionsDB.getInstance().getConnection();
    }

    public boolean isLeconCompleted(int userId, int leconId) {
        String sql = "SELECT 1 FROM user_lesson_result WHERE user_id = ? AND lesson_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, leconId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void markAsCompleted(int userId, int leconId) {
        if (isLeconCompleted(userId, leconId)) return;

        String sql = "INSERT INTO user_lesson_result (user_id, lesson_id, is_completed) VALUES (?, ?, 1)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, leconId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void resetProgress(int userId, int formationId) {
        String sql = "DELETE FROM user_lesson_result WHERE user_id = ? AND lesson_id IN " +
                "(SELECT id FROM lecon WHERE formation_id = ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, formationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
