package Entities.event;

import java.sql.Timestamp;

public class Reservation {
    private int id;
    private int userId;
    private int eventId;
    private ReservationStatus status;
    private Timestamp reservedAt;
    private boolean qrCodeChecked;

    private String username;
    private String userEmail;
    private String userPhone;
    private String eventTitle;
    private Timestamp eventStartDate;
    private Timestamp eventEndDate;

    public Reservation() {
    }

    public Reservation(int id, int userId, int eventId, ReservationStatus status, Timestamp reservedAt, boolean qrCodeChecked) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.status = status;
        this.reservedAt = reservedAt;
        this.qrCodeChecked = qrCodeChecked;
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

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
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

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
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

    public Timestamp getEventEndDate() {
        return eventEndDate;
    }

    public void setEventEndDate(Timestamp eventEndDate) {
        this.eventEndDate = eventEndDate;
    }

    public String getStatusLabel() {
        return status == null ? "UNKNOWN" : status.name();
    }
}
