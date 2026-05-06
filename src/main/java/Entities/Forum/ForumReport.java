package Entities.Forum;

import java.sql.Timestamp;

public class ForumReport {
    private int id;
    private String reason;
    private String status;
    private Timestamp createdAt;
    private int postId;
    private int commentId;
    private int reporterId;
    private String description;

    public ForumReport() {}

    public ForumReport(int id, String reason, String status, Timestamp createdAt, int postId, int commentId, int reporterId, String description) {
        this.id = id;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.postId = postId;
        this.commentId = commentId;
        this.reporterId = reporterId;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public int getCommentId() { return commentId; }
    public void setCommentId(int commentId) { this.commentId = commentId; }

    public int getReporterId() { return reporterId; }
    public void setReporterId(int reporterId) { this.reporterId = reporterId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
