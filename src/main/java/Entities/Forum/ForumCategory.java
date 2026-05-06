package Entities.Forum;

public class ForumCategory {
    private int id;
    private String name;
    private String slug;
    private String description;
    private String color;

    public ForumCategory() {}

    public ForumCategory(int id, String name, String slug, String description, String color) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.color = color;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    @Override
    public String toString() {
        return name;
    }
}
