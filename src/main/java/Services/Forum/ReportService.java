package Services.Forum;

import Utils.ShadowDimensionsDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ReportService {

    private final Connection cnx;

    public ReportService() {
        this.cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    public boolean hasUserReportedPost(int postId, int reporterId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM forum_report WHERE post_id = ? AND reporter_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, postId);
        ps.setInt(2, reporterId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    public void reportPost(int postId, int reporterId, String reason, String description) throws SQLException {
        // 1. Insert the report
        String sqlInsert = "INSERT INTO forum_report (reason, status, created_at, post_id, reporter_id, description) VALUES (?, 'PENDING', ?, ?, ?, ?)";
        PreparedStatement psInsert = cnx.prepareStatement(sqlInsert);
        psInsert.setString(1, reason);
        psInsert.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        psInsert.setInt(3, postId);
        psInsert.setInt(4, reporterId);
        psInsert.setString(5, description);
        psInsert.executeUpdate();

        // 2. Count reports for this post
        String sqlCount = "SELECT COUNT(*) FROM forum_report WHERE post_id = ?";
        PreparedStatement psCount = cnx.prepareStatement(sqlCount);
        psCount.setInt(1, postId);
        ResultSet rs = psCount.executeQuery();
        int reportCount = 0;
        if (rs.next()) {
            reportCount = rs.getInt(1);
        }

        // 3. Auto-hide if threshold met
        if (reportCount >= 2) {
            String sqlHide = "UPDATE forum_post SET is_hidden = 1 WHERE id = ?";
            PreparedStatement psHide = cnx.prepareStatement(sqlHide);
            psHide.setInt(1, postId);
            psHide.executeUpdate();
        }
    }
}
