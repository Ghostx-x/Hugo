package com.example.hugo.bottomnavbar.Profile;

public class Booking {
    public String bookedUserId;
    public String bookedUserName;
    public String bookedUserPhotoUrl;
    public String bookedTime;

    public Booking(String bookedUserId, String bookedUserName, String bookedUserPhotoUrl, String bookedTime) {
        this.bookedUserId = bookedUserId;
        this.bookedUserName = bookedUserName;
        this.bookedUserPhotoUrl = bookedUserPhotoUrl;
        this.bookedTime = bookedTime;
    }

    public String getBookedUserId() {
        return bookedUserId;
    }

    public String getBookedUserName() {
        return bookedUserName;
    }

    public String getBookedUserPhotoUrl() {
        return bookedUserPhotoUrl;
    }

    public String getBookedTime() {
        return bookedTime;
    }

    public void setBookedUserId(String bookedUserId) {
        this.bookedUserId = bookedUserId;
    }

    public void setBookedUserName(String bookedUserName) {
        this.bookedUserName = bookedUserName;
    }

    public void setBookedUserPhotoUrl(String bookedUserPhotoUrl) {
        this.bookedUserPhotoUrl = bookedUserPhotoUrl;
    }

    public void setBookedTime(String bookedTime) {
        this.bookedTime = bookedTime;
    }
}

