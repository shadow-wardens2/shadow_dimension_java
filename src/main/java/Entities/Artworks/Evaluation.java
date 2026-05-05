package Entities.Artworks;

public class Evaluation {
    private int id;
    private int artworkId;
    private int userId;
    private int rating; // 1 to 5
    private String comment;
    private String date;
    private String artworkTitle;

    public Evaluation() {}

    public Evaluation(int id, int artworkId, int userId, int rating, String comment, String date) {
        this.id = id;
        this.artworkId = artworkId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
        this.date = date;
    }

    public String getArtworkTitle() { return artworkTitle; }
    public void setArtworkTitle(String artworkTitle) { this.artworkTitle = artworkTitle; }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getArtworkId() { return artworkId; }
    public void setArtworkId(int artworkId) { this.artworkId = artworkId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
