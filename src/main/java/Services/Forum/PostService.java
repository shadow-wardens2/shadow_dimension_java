package Services.Forum;

import Entities.Forum.Post;
import Interfaces.InterfaceServiceProduit;
import Utils.SessionManager;
import Utils.ShadowDimensionsDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostService implements InterfaceServiceProduit<Post> {

    private final Connection cnx;

    public PostService() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    @Override
    public void add(Post p) throws SQLException {
        String sql = "INSERT INTO forum_post (title, content, category_id, image, votes, is_locked, status, is_hidden, author_id, created_at) " +
                     "VALUES (?, ?, ?, ?, 0, 0, 'published', 0, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, Utils.ProfanityFilter.filter(p.getTitle()));
        ps.setString(2, Utils.ProfanityFilter.filter(p.getContent()));
        ps.setInt(3, p.getCategoryId());
        ps.setString(4, p.getImageUrl());
        ps.setInt(5, p.getAuthorId());
        ps.setTimestamp(6, p.getCreatedAt() != null ? p.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
        ps.executeUpdate();
    }

    @Override
    public void update(Post p) throws SQLException {
        String sql = "UPDATE forum_post SET title=?, content=?, category_id=?, image=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, Utils.ProfanityFilter.filter(p.getTitle()));
        ps.setString(2, Utils.ProfanityFilter.filter(p.getContent()));
        ps.setInt(3, p.getCategoryId());
        ps.setString(4, p.getImageUrl());
        ps.setInt(5, p.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(Post p) throws SQLException {
        String sql = "DELETE FROM forum_post WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, p.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Post> getAll() throws SQLException {
        return getAllWithCommentCount();
    }

    /**
     * Fetches all posts with comment count and author username via JOINs.
     */
    public List<Post> getAllWithCommentCount() throws SQLException {
        int userId = SessionManager.isLoggedIn() ? SessionManager.getCurrentUser().getId() : -1;
        boolean isAdmin = SessionManager.isLoggedIn() && SessionManager.getCurrentUser().isAdmin();
        String whereClause = isAdmin ? "" : "WHERE p.is_hidden = 0 ";
        List<Post> list = new ArrayList<>();
        String sql =
            "SELECT p.*, " +
            "  COALESCE(u.username, CONCAT('user_', p.author_id)) AS author_name, " +
            "  fc.name AS category_name, " +
            "  (SELECT COUNT(*) FROM forum_commentaire c WHERE c.post_id = p.id) AS comment_count, " +
            "  COALESCE(fv.vote_type, 0) AS current_user_vote " +
            "FROM forum_post p " +
            "LEFT JOIN user u ON p.author_id = u.id " +
            "LEFT JOIN forum_category fc ON p.category_id = fc.id " +
            "LEFT JOIN forum_vote fv ON p.id = fv.post_id AND fv.user_id = ? " +
            whereClause +
            "ORDER BY p.id DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapPost(rs));
        }
        return list;
    }

    /**
     * Fetches all posts filtered by category (with comment counts).
     */
    public List<Post> getByCategory(String category) throws SQLException {
        int userId = SessionManager.isLoggedIn() ? SessionManager.getCurrentUser().getId() : -1;
        boolean isAdmin = SessionManager.isLoggedIn() && SessionManager.getCurrentUser().isAdmin();
        String hiddenFilter = isAdmin ? "" : "AND p.is_hidden = 0 ";
        List<Post> list = new ArrayList<>();
        String sql =
            "SELECT p.*, " +
            "  COALESCE(u.username, CONCAT('user_', p.author_id)) AS author_name, " +
            "  fc.name AS category_name, " +
            "  (SELECT COUNT(*) FROM forum_commentaire c WHERE c.post_id = p.id) AS comment_count, " +
            "  COALESCE(fv.vote_type, 0) AS current_user_vote " +
            "FROM forum_post p " +
            "LEFT JOIN user u ON p.author_id = u.id " +
            "LEFT JOIN forum_category fc ON p.category_id = fc.id " +
            "LEFT JOIN forum_vote fv ON p.id = fv.post_id AND fv.user_id = ? " +
            "WHERE fc.name = ? " + hiddenFilter +
            "ORDER BY p.id DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setString(2, category);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapPost(rs));
        }
        return list;
    }

    /**
     * Fetches all posts authored by a specific user.
     */
    public List<Post> getPostsByUserId(int authorId) throws SQLException {
        int currentUserId = SessionManager.isLoggedIn() ? SessionManager.getCurrentUser().getId() : -1;
        boolean isAdmin = SessionManager.isLoggedIn() && SessionManager.getCurrentUser().isAdmin();
        String hiddenFilter = isAdmin ? "" : "AND p.is_hidden = 0 ";
        List<Post> list = new ArrayList<>();
        String sql =
            "SELECT p.*, " +
            "  COALESCE(u.username, CONCAT('user_', p.author_id)) AS author_name, " +
            "  fc.name AS category_name, " +
            "  (SELECT COUNT(*) FROM forum_commentaire c WHERE c.post_id = p.id) AS comment_count, " +
            "  COALESCE(fv.vote_type, 0) AS current_user_vote " +
            "FROM forum_post p " +
            "LEFT JOIN user u ON p.author_id = u.id " +
            "LEFT JOIN forum_category fc ON p.category_id = fc.id " +
            "LEFT JOIN forum_vote fv ON p.id = fv.post_id AND fv.user_id = ? " +
            "WHERE (p.author_id = ? OR EXISTS (SELECT 1 FROM forum_post_share fps WHERE fps.post_id = p.id AND fps.user_id = ?)) " + hiddenFilter +
            "ORDER BY p.id DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, currentUserId);
        ps.setInt(2, authorId);
        ps.setInt(3, authorId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapPost(rs));
        }
        return list;
    }

    /**
     * Shares a post to a user's profile feed.
     */
    public void sharePost(int postId, int userId) throws SQLException {
        String sql = "INSERT IGNORE INTO forum_post_share (post_id, user_id) VALUES (?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, postId);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    public Post getById(int id) throws SQLException {
        int userId = SessionManager.isLoggedIn() ? SessionManager.getCurrentUser().getId() : -1;
        String sql =
            "SELECT p.*, " +
            "  COALESCE(u.username, CONCAT('user_', p.author_id)) AS author_name, " +
            "  fc.name AS category_name, " +
            "  (SELECT COUNT(*) FROM forum_commentaire c WHERE c.post_id = p.id) AS comment_count, " +
            "  COALESCE(fv.vote_type, 0) AS current_user_vote " +
            "FROM forum_post p " +
            "LEFT JOIN user u ON p.author_id = u.id " +
            "LEFT JOIN forum_category fc ON p.category_id = fc.id " +
            "LEFT JOIN forum_vote fv ON p.id = fv.post_id AND fv.user_id = ? " +
            "WHERE p.id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapPost(rs);
        return null;
    }

    /** Increments the vote counter by +1. */
    public void upvote(int postId) throws SQLException {
        handleVote(postId, 1);
    }

    /** Decrements the vote counter by -1. */
    public void downvote(int postId) throws SQLException {
        handleVote(postId, -1);
    }

    private void handleVote(int postId, int targetVote) throws SQLException {
        if (!SessionManager.isLoggedIn()) return;
        int userId = SessionManager.getCurrentUser().getId();

        // Check if vote exists
        PreparedStatement psCheck = cnx.prepareStatement("SELECT vote_type FROM forum_vote WHERE post_id=? AND user_id=?");
        psCheck.setInt(1, postId);
        psCheck.setInt(2, userId);
        ResultSet rs = psCheck.executeQuery();

        if (rs.next()) {
            int currentVote = rs.getInt("vote_type");
            if (currentVote == targetVote) {
                // Remove vote (unvote)
                PreparedStatement psDel = cnx.prepareStatement("DELETE FROM forum_vote WHERE post_id=? AND user_id=?");
                psDel.setInt(1, postId);
                psDel.setInt(2, userId);
                psDel.executeUpdate();
            } else {
                // Switch vote
                PreparedStatement psUpd = cnx.prepareStatement("UPDATE forum_vote SET vote_type=? WHERE post_id=? AND user_id=?");
                psUpd.setInt(1, targetVote);
                psUpd.setInt(2, postId);
                psUpd.setInt(3, userId);
                psUpd.executeUpdate();
            }
        } else {
            // New vote
            PreparedStatement psIns = cnx.prepareStatement("INSERT INTO forum_vote (post_id, user_id, vote_type) VALUES (?, ?, ?)");
            psIns.setInt(1, postId);
            psIns.setInt(2, userId);
            psIns.setInt(3, targetVote);
            psIns.executeUpdate();
        }

        // Recalculate total votes
        PreparedStatement psRecalc = cnx.prepareStatement(
            "UPDATE forum_post p SET votes = COALESCE((SELECT SUM(vote_type) FROM forum_vote WHERE post_id = p.id), 0) WHERE p.id = ?"
        );
        psRecalc.setInt(1, postId);
        psRecalc.executeUpdate();
    }

    public void toggleHidden(int postId, boolean hidden) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("UPDATE forum_post SET is_hidden = ? WHERE id = ?");
        ps.setBoolean(1, hidden);
        ps.setInt(2, postId);
        ps.executeUpdate();
    }

    public void toggleLocked(int postId, boolean locked) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("UPDATE forum_post SET is_locked = ? WHERE id = ?");
        ps.setBoolean(1, locked);
        ps.setInt(2, postId);
        ps.executeUpdate();
    }

    private Post mapPost(ResultSet rs) throws SQLException {
        return new Post(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("content"),
            rs.getInt("category_id"),
            rs.getString("category_name"),
            rs.getString("image"),
            rs.getInt("votes"),
            rs.getInt("author_id"),
            rs.getString("author_name"),
            rs.getTimestamp("created_at"),
            rs.getInt("comment_count"),
            rs.getInt("current_user_vote"),
            rs.getBoolean("is_locked"),
            rs.getBoolean("is_hidden")
        );
    }

    /**
     * Statistics: Number of posts per category
     */
    public Map<String, Integer> getPostsCountByCategory() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT fc.name AS category_name, COUNT(p.id) AS post_count " +
                     "FROM forum_post p " +
                     "JOIN forum_category fc ON p.category_id = fc.id " +
                     "GROUP BY fc.name";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            stats.put(rs.getString("category_name"), rs.getInt("post_count"));
        }
        return stats;
    }

    /**
     * Statistics: Number of posts created per date (last 7 active days)
     */
    public Map<String, Integer> getPostsCountByDate() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        // Note: Using DATE() to group by day. Depending on DB, format might differ slightly, but standard MySQL DATE() works.
        String sql = "SELECT DATE(created_at) AS post_date, COUNT(id) AS post_count " +
                     "FROM forum_post " +
                     "GROUP BY DATE(created_at) " +
                     "ORDER BY post_date DESC " +
                     "LIMIT 7";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            // Using string representation of the date
            String dateStr = rs.getString("post_date"); 
            stats.put(dateStr, rs.getInt("post_count"));
        }
        return stats;
    }
}
