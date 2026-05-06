package Entities.Forum;

import java.sql.Timestamp;

public class Post {

    private int id;
    private String title;
    private String content;
    private int categoryId;
    private String categoryName; // transient — resolved via JOIN
    private String imageUrl;
    private int votes;
    private int authorId;
    private String authorName; // transient — resolved via JOIN
    private Timestamp createdAt;
    private int commentCount;  // transient — resolved via subquery
    private int currentUserVote; // transient — 1 for upvote, -1 for downvote, 0 for none
    private boolean locked;
    private boolean hidden;

    public Post() {}

    public Post(int id, String title, String content, int categoryId, String categoryName,
                String imageUrl, int votes, int authorId,
                String authorName, Timestamp createdAt, int commentCount, int currentUserVote,
                boolean locked, boolean hidden) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.imageUrl = imageUrl;
        this.votes = votes;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.commentCount = commentCount;
        this.currentUserVote = currentUserVote;
        this.locked = locked;
        this.hidden = hidden;
    }

    // ---- Getters & Setters ----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getVotes() { return votes; }
    public void setVotes(int votes) { this.votes = votes; }

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public int getCurrentUserVote() { return currentUserVote; }
    public void setCurrentUserVote(int currentUserVote) { this.currentUserVote = currentUserVote; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    @Override
    public String toString() { return title; }
}
