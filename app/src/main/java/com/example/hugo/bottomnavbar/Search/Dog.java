package com.example.hugo.bottomnavbar.Search;

public class Dog {
    public String name;
    public String breed;
    public int age;
    public String profileImageBase64;

    public Dog() {
        // Default constructor for Firebase
    }

    public Dog(String name, String breed, int age, String profileImageBase64) {
        this.name = name;
        this.breed = breed;
        this.age = age;
        this.profileImageBase64 = profileImageBase64;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public void setProfileImageBase64(String profileImageBase64) {
        this.profileImageBase64 = profileImageBase64;
    }
}