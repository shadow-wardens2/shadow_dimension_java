package Entities.Artworks;

import java.util.Objects;

public class Artworks {
    private int  id;
    private String title;
    private String description;
    private int price;
    private String imageurl;
    private String status;
    private int categoryID;

    public Artworks(){}

    public Artworks(int id, String title, String description, int price, String imageurl, String status, int categoryID) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.imageurl = imageurl;
        this.status = status;
        this.categoryID = categoryID;

    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
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
    public int getPrice() {
        return price;
    }
    public void setPrice(int price) {
        this.price = price;
    }
    public String getImageurl() {
        return imageurl;
    }
    public void setImageurl(String imageurl) {
        this.imageurl = imageurl;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public int getCategoryID() {
        return categoryID;
    }
    public void setCategoryID(int categoryID) {
        this.categoryID = categoryID;
    }

    @Override
    public String toString() {
        return "Artworks{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", image='" + imageurl + '\'' +
                ", status='" + status + '\'' +
                ", categoryID=" + categoryID +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Artworks artworks)) return false;
        return id == artworks.id && price == artworks.price && categoryID == artworks.categoryID && Objects.equals(title, artworks.title) && Objects.equals(description, artworks.description) && Objects.equals(imageurl, artworks.imageurl) && Objects.equals(status, artworks.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, price, imageurl, status, categoryID);
    }
}
