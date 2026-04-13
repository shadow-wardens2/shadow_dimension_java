package Entities.event;

import java.sql.Timestamp;

public class Event {
    private int id;
    private String title;
    private String description;
    private String location;
    private Timestamp startDate;
    private Timestamp endDate;
    private String image;
    private int capacity;
    private String qrCodePath;
    private Timestamp createdAt;
    private String status;
    private Category category;
    private int createdById;
    private String visualVibe;
    private String locationType;

    public Event() {
    }

    public Event(int id, String title, String description, String location, Timestamp startDate,
                 Timestamp endDate, String image, int capacity, String qrCodePath,
                 Timestamp createdAt, String status, Category category, int createdById,
                 String visualVibe, String locationType) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.image = image;
        this.capacity = capacity;
        this.qrCodePath = qrCodePath;
        this.createdAt = createdAt;
        this.status = status;
        this.category = category;
        this.createdById = createdById;
        this.visualVibe = visualVibe;
        this.locationType = locationType;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getQrCodePath() {
        return qrCodePath;
    }

    public void setQrCodePath(String qrCodePath) {
        this.qrCodePath = qrCodePath;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public int getCreatedById() {
        return createdById;
    }

    public void setCreatedById(int createdById) {
        this.createdById = createdById;
    }

    public String getVisualVibe() {
        return visualVibe;
    }

    public void setVisualVibe(String visualVibe) {
        this.visualVibe = visualVibe;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getCategoryName() {
        return category != null ? category.getNom() : "";
    }
}
