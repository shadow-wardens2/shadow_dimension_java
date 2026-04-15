package Entities.Artworks;

import java.util.Objects;

public class Categories

{
    private String ID;
    private String title;
    private String description;

    public Categories(){};
    public Categories(String ID, String title, String description)
    {
        this.ID = ID;
        this.title = title;
        this.description = description;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return title != null ? title : "Unknown";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Categories that)) return false;
        return Objects.equals(ID, that.ID) && Objects.equals(title, that.title) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID, title, description);
    }
}
