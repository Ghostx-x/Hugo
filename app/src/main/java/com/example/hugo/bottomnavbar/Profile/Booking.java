package com.example.hugo.bottomnavbar.Profile;

public class Booking {
    public String appointmentId;
    public String bookedUserId;
    public String bookedUserName;
    public String bookedUserPhotoBase64;
    public String bookedTime;
    public String status;

    public Booking() {
    }

    public Booking(String appointmentId, String bookedUserId, String bookedUserName, String bookedUserPhotoBase64,
                   String bookedTime, String status) {
        this.appointmentId = appointmentId;
        this.bookedUserId = bookedUserId;
        this.bookedUserName = bookedUserName;
        this.bookedUserPhotoBase64 = bookedUserPhotoBase64;
        this.bookedTime = bookedTime;
        this.status = status;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getBookedUserId() {
        return bookedUserId;
    }

    public void setBookedUserId(String bookedUserId) {
        this.bookedUserId = bookedUserId;
    }

    public String getBookedUserName() {
        return bookedUserName;
    }

    public void setBookedUserName(String bookedUserName) {
        this.bookedUserName = bookedUserName;
    }

    public String getBookedUserPhotoBase64() {
        return bookedUserPhotoBase64;
    }

    public void setBookedUserPhotoBase64(String bookedUserPhotoBase64) {
        this.bookedUserPhotoBase64 = bookedUserPhotoBase64;
    }

    public String getBookedTime() {
        return bookedTime;
    }

    public void setBookedTime(String bookedTime) {
        this.bookedTime = bookedTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}