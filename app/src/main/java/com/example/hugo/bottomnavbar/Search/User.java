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
        public int age;
        public String profileImageUrl;
        public String birthday;
        public String gender;
        public String specialCare;

        public Dog() {}

        public Dog(String name, String breed, int age, String profileImageUrl, String birthday, String gender, String specialCare) {
            this.name = name;
            this.breed = breed;
            this.age = age;
            this.profileImageUrl = profileImageUrl;
            this.birthday = birthday;
            this.gender = gender;
            this.specialCare = specialCare;
        }
    }
}