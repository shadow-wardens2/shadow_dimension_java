package Entities.event;

import java.sql.Timestamp;

public class EventReclamation {

    private int id;
    private int userId;
    private int eventId;
    private EventReclamationStatus status;
    private String subject;
    private String message;
    private String aiResponse;
    private String adminResponse;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    private String username;
    private String userEmail;
    private String eventTitle;

    public EventReclamation() {
    }

    public EventReclamation(int id,
                            int userId,
                            int eventId,
                            EventReclamationStatus status,
                            String subject,
                            String message,
                            String aiResponse,
                            String adminResponse,
                            Timestamp createdAt,
                            Timestamp updatedAt) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.status = status;
        this.subject = subject;
        this.message = message;
        this.aiResponse = aiResponse;
        this.adminResponse = adminResponse;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public EventReclamationStatus getStatus() {
        return status;
    }

    public void setStatus(EventReclamationStatus status) {
        this.status = status;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }

    public String getAdminResponse() {
        return adminResponse;
    }

    public void setAdminResponse(String adminResponse) {
        this.adminResponse = adminResponse;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public String getStatusLabel() {
        return status == null ? "OPEN" : status.name();
    }

    public boolean canEscalate() {
        return status == EventReclamationStatus.AI_RESPONDED
                || status == EventReclamationStatus.OPEN
                || status == EventReclamationStatus.IN_PROGRESS;
    }
}
