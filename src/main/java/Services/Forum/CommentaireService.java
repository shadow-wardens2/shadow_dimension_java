package Services.Forum;

import Entities.Forum.Commentaire;
import Interfaces.InterfaceServiceProduit;
import Services.User.EmailService;
import Utils.ProfanityFilter;
import Utils.SessionManager;
import Utils.ShadowDimensionsDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentaireService implements InterfaceServiceProduit<Commentaire> {

    private final Connection cnx;

    public CommentaireService() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(Commentaire c) throws SQLException {
        String originalContent = c.getContent();
        String filteredContent = ProfanityFilter.filter(originalContent);
        
        // Detect if the comment had bad words
        boolean hasBadWords = !originalContent.equals(filteredContent);
        
        if (hasBadWords) {
            // Increment the user's bad comment strike count
            incrementBadCommentCount(c.getAuthorId());
            
            // Check the new total
            int strikeCount = getBadCommentCount(c.getAuthorId());
            
            if (strikeCount >= 3) {
                // Lock the user's commenting ability (is_locked = 1)
                lockUserComments(c.getAuthorId());
                
                // Send ban notification email in a background thread
                new Thread(() -> {
                    try {
                        String userEmail = getUserEmail(c.getAuthorId());
                        String username = getUserName(c.getAuthorId());
                        if (userEmail != null) {
                            new EmailService().sendBanWarning(userEmail, username);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
                
                throw new SQLException("COMMENT_BAN: Your commenting rights have been revoked due to repeated use of prohibited language.");
            } else {
                int remaining = 3 - strikeCount;
                // Let the comment through but warn user (still filtered)
                c.setContent(filteredContent);
                String warningSql = "INSERT INTO forum_comment (content, post_id, author_id, created_at, is_visible) VALUES (?, ?, ?, ?, 1)";
                PreparedStatement ps = cnx.prepareStatement(warningSql);
                ps.setString(1, filteredContent);
                ps.setInt(2, c.getPostId());
                ps.setInt(3, c.getAuthorId());
                ps.setTimestamp(4, c.getCreatedAt() != null ? c.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
                throw new SQLException("COMMENT_WARN:" + remaining + " - Warning: Prohibited language detected. You have " + remaining + " strike(s) remaining before your commenting rights are revoked.");
            }
        }
        
        // Check if user is already banned
        if (isCommentBanned(c.getAuthorId())) {
            throw new SQLException("COMMENT_BAN: Your commenting rights have been revoked due to repeated use of prohibited language.");
        }
        
        String sql = "INSERT INTO forum_comment (content, post_id, author_id, created_at, is_visible) VALUES (?, ?, ?, ?, 1)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, filteredContent);
        ps.setInt(2, c.getPostId());
        ps.setInt(3, c.getAuthorId());
        ps.setTimestamp(4, c.getCreatedAt() != null ? c.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
        ps.executeUpdate();
    }

    /** Checks if a user is banned from commenting (is_locked = 1). */
    public boolean isCommentBanned(int userId) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("SELECT is_locked FROM user WHERE id = ?");
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt("is_locked") == 1;
    }

    private void incrementBadCommentCount(int userId) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("UPDATE user SET bad_comment_count = bad_comment_count + 1 WHERE id = ?");
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    private int getBadCommentCount(int userId) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("SELECT bad_comment_count FROM user WHERE id = ?");
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt("bad_comment_count") : 0;
    }

    private void lockUserComments(int userId) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("UPDATE user SET is_locked = 1 WHERE id = ?");
        ps.setInt(1, userId);
        ps.executeUpdate();
        // Update session if it's the current user
        if (SessionManager.isLoggedIn() && SessionManager.getCurrentUser().getId() == userId) {
            SessionManager.getCurrentUser().setIsLocked(1);
        }
    }

    private String getUserEmail(int userId) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("SELECT email FROM user WHERE id = ?");
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getString("email") : null;
    }

    private String getUserName(int userId) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("SELECT username FROM user WHERE id = ?");
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getString("username") : "Shadow Dweller";
    }

    @Override
    public void update(Commentaire c) throws SQLException {
        String sql = "UPDATE forum_comment SET content=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, Utils.ProfanityFilter.filter(c.getContent()));
        ps.setInt(2, c.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(Commentaire c) throws SQLException {
        String sql = "DELETE FROM forum_comment WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, c.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Commentaire> getAll() throws SQLException {
        List<Commentaire> list = new ArrayList<>();
        String sql =
            "SELECT fc.*, COALESCE(u.username, CONCAT('user_', fc.author_id)) AS author_name " +
            "FROM forum_comment fc " +
            "LEFT JOIN user u ON fc.author_id = u.id " +
            "ORDER BY fc.id ASC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(mapCommentaire(rs));
        }
        return list;
    }

    /** Fetches all comments for a given post, ordered oldest first. */
    public List<Commentaire> getByPostId(int postId) throws SQLException {
        List<Commentaire> list = new ArrayList<>();
        String sql =
            "SELECT fc.*, COALESCE(u.username, CONCAT('user_', fc.author_id)) AS author_name " +
            "FROM forum_comment fc " +
            "LEFT JOIN user u ON fc.author_id = u.id " +
            "WHERE fc.post_id = ? " +
            "ORDER BY fc.id ASC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, postId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapCommentaire(rs));
        }
        return list;
    }

    private Commentaire mapCommentaire(ResultSet rs) throws SQLException {
        return new Commentaire(
            rs.getInt("id"),
            rs.getString("content"),
            rs.getInt("post_id"),
            rs.getInt("author_id"),
            rs.getString("author_name"),
            rs.getTimestamp("created_at")
        );
    }
}
