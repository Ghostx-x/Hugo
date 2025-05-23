package com.example.hugo.bottomnavbar.Search;

import java.util.List;
import java.util.Map;

public class User {
    public String userId;
    public String name;
    public String bio;
    public String userType;
    public String locationName;
    public double latitude;
    public double longitude;
    public String profileImageBase64;
    public String profileImageUrl;
    public Map<String, List<String>> availability;
    public Double ranking;

    public User() {}

    public User(String userId, String name, String bio, String userType, String locationName, double latitude,
                double longitude, String profileImageBase64, String profileImageUrl,
                Map<String, List<String>> availability, Double ranking) {
        this.userId = userId;
        this.name = name;
        this.bio = bio;
        this.userType = userType;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.profileImageBase64 = profileImageBase64;
        this.profileImageUrl = profileImageUrl;
        this.availability = availability;
        this.ranking = ranking;
    }

    public static class Dog {
        public String name;
        public String breed;
        public String age; // Changed to String to handle Firebase data
        public String profileImageUrl;
        public String imageBase64;
        public String birthday;
        public String gender;
        public String specialCare;

        public Dog() {}

        public Dog(String name, String breed, String age, String profileImageUrl, String imageBase64, String birthday, String gender, String specialCare) {
            this.name = name;
            this.breed = breed;
            this.age = age;
            this.profileImageUrl = profileImageUrl;
            this.imageBase64 = imageBase64;
            this.birthday = birthday;
            this.gender = gender;
            this.specialCare = specialCare;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getBreed() { return breed; }
        public void setBreed(String breed) { this.breed = breed; }
        public String getAge() { return age; }
        public void setAge(String age) { this.age = age; }
        public String getProfileImageUrl() { return profileImageUrl; }
        public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
        public String getImageBase64() { return imageBase64; }
        public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
        public String getBirthday() { return birthday; }
        public void setBirthday(String birthday) { this.birthday = birthday; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public String getSpecialCare() { return specialCare; }
        public void setSpecialCare(String specialCare) { this.specialCare = specialCare; }
    }
}