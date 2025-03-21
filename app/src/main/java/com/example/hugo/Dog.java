package com.example.hugo;

public class Dog {
    private String name;
    private String breed;
    private String gender;
    private String birthday;

    // Default constructor required for Firebase
    public Dog() {}

    public Dog(String name, String breed, String gender, String birthday) {
        this.name = name;
        this.breed = breed;
        this.gender = gender;
        this.birthday = birthday;
    }

    // Getters and Setters
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }
}
