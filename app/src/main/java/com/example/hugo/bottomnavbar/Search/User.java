package com.example.hugo.bottomnavbar.Search;

public class User {
    public String userId;
    public String name;
    public String bio;
    public String userType;
    public String profileImageUrl;
    public Dog dog;
    public double ranking;

    public User() {}

    public User(String userId, String name, String bio, String userType, String profileImageUrl, Dog dog, double ranking) {
        this.userId = userId;
        this.name = name;
        this.bio = bio;
        this.userType = userType;
        this.profileImageUrl = profileImageUrl;
        this.dog = dog;
        this.ranking = ranking;
    }
}