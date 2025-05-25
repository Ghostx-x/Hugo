package com.example.hugo.bottomnavbar.Profile;

import java.io.Serializable;

public class Dog implements Serializable {
    private String key; // Firebase push ID
    private String name;
    private String breed;
    private String birthDate;
    private String gender;
    private String size;
    private String description;
    private String imageBase64;

    public Dog() {}

    public Dog(String key, String name, String breed, String birthDate, String gender, String size, String description, String imageBase64) {
        this.key = key;
        this.name = name;
        this.breed = breed;
        this.birthDate = birthDate;
        this.gender = gender;
        this.size = size;
        this.description = description;
        this.imageBase64 = imageBase64;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

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

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}