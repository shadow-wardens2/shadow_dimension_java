package Services.Tutorials;

import Utils.ShadowDimensionsDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceQuizProgress {
    private final Connection connection;

    public ServiceQuizProgress() {
        this.connection = ShadowDimensionsDB.getInstance().getConnection();
    }

    /** Check if the user has attempted/finished this quiz */
    public boolean isQuizCompleted(int userId, int quizId) {
        String sql = "SELECT 1 FROM user_quiz_result WHERE user_id = ? AND quiz_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isQuizPassed(int userId, int quizId) {
        String sql = "SELECT 1 FROM user_quiz_result WHERE user_id = ? AND quiz_id = ? AND is_passed = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Save quiz result (score + is_passed) into user_quiz_result */
    public void markAsCompleted(int userId, int quizId, int score, boolean isPassed) {
        // First check if an entry exists
        boolean exists = false;
        String checkSql = "SELECT 1 FROM user_quiz_result WHERE user_id = ? AND quiz_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
            ps.setInt(1, userId);
            ps.setInt(2, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                exists = rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (exists) {
            // Update the existing attempt
            String updateSql = "UPDATE user_quiz_result SET score = ?, is_passed = ?, completed_at = CURRENT_TIMESTAMP WHERE user_id = ? AND quiz_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setInt(1, score);
                ps.setInt(2, isPassed ? 1 : 0);
                ps.setInt(3, userId);
                ps.setInt(4, quizId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            // Insert new attempt
            String insertSql = "INSERT INTO user_quiz_result (user_id, quiz_id, score, is_passed, completed_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, quizId);
                ps.setInt(3, score);
                ps.setInt(4, isPassed ? 1 : 0);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /** Reset all quiz results for this user in this formation */
    public void resetProgress(int userId, int formationId) {
        String sql = "DELETE FROM user_quiz_result WHERE user_id = ? AND quiz_id IN " +
                "(SELECT id FROM quiz WHERE formation_id = ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, formationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Count total quizzes completed (passed) by this user — used for rank
     * calculation
     */
    public int getCompletedQuizzesCount(int userId) {
        String sql = "SELECT COUNT(*) FROM user_quiz_result WHERE user_id = ? AND is_passed = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** Get IDs of formations where the user has at least one quiz result */
    public List<Integer> getStartedFormations(int userId) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT DISTINCT f.id FROM formation f " +
                "JOIN quiz q ON f.id = q.formation_id " +
                "JOIN user_quiz_result uqr ON q.id = uqr.quiz_id " +
                "WHERE uqr.user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    ids.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }
}
