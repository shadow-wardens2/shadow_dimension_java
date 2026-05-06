package Entities.Forum;

import java.sql.Timestamp;

public class Commentaire {

    private int id;
    private String content;
    private int postId;
    private int authorId;
    private String authorName; // transient — resolved via JOIN
    private Timestamp createdAt;

    public Commentaire() {}

    public Commentaire(int id, String content, int postId, int authorId,
                       String authorName, Timestamp createdAt) {
        this.id = id;
        this.content = content;
        this.postId = postId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = createdAt;
    }

    // ---- Getters & Setters ----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return content; }
}
