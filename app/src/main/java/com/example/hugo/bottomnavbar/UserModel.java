package com.example.hugo.bottomnavbar;

public class UserModel {
    private String name, userType, bio, email, location, profileImageUrl;
    private int dogCount;

    public UserModel(String name, String userType, String email, String bio, int dogCount, String location, String profileImageUrl) {
        this.name = name;
        this.userType = userType;
        this.email = email;
        this.bio = bio;
        this.dogCount = dogCount;
        this.location = location;  // Keep location
        this.profileImageUrl = profileImageUrl;  // Keep profileImageUrl
    }

    public String getName() { return name; }
    public String getUserType() { return userType; }
    public String getBio() { return bio; }
    public String getEmail() { return email; }
    public int getDogCount() { return dogCount; }
    public String getLocation() { return location; }  // Getter for location
    public String getProfileImageUrl() { return profileImageUrl; }  // Getter for profileImageUrl
}
