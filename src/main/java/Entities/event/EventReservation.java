package Entities.event;

import java.sql.Timestamp;

public class EventReservation {
    private int id;
    private String status;
    private Timestamp reservedAt;
    private boolean qrCodeChecked;
    private int userId;
    private int eventId;
    private String username;
    private String userEmail;
    private String eventTitle;
    private Timestamp eventStartDate;

    public EventReservation() {
    }

    public EventReservation(int id, String status, Timestamp reservedAt, boolean qrCodeChecked,
                            int userId, int eventId, String username, String userEmail,
                            String eventTitle, Timestamp eventStartDate) {
        this.id = id;
        this.status = status;
        this.reservedAt = reservedAt;
        this.qrCodeChecked = qrCodeChecked;
        this.userId = userId;
        this.eventId = eventId;
        this.username = username;
        this.userEmail = userEmail;
        this.eventTitle = eventTitle;
        this.eventStartDate = eventStartDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(Timestamp reservedAt) {
        this.reservedAt = reservedAt;
    }

    public boolean isQrCodeChecked() {
        return qrCodeChecked;
    }

    public void setQrCodeChecked(boolean qrCodeChecked) {
        this.qrCodeChecked = qrCodeChecked;
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

    public Timestamp getEventStartDate() {
        return eventStartDate;
    }

    public void setEventStartDate(Timestamp eventStartDate) {
        this.eventStartDate = eventStartDate;
    }
}
