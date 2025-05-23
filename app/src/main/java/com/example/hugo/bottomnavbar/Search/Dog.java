package com.example.hugo.bottomnavbar.Search;

import java.io.Serializable;

public class Dog implements Serializable {
    private String name;
    private String breed;
    private String birthDate;
    private String gender;
    private String size;
    private String description;
    private String imageBase64;

    public Dog() {}

    public Dog(String name, String breed, String birthDate, String gender, String size, String description, String imageBase64) {
        this.name = name;
        this.breed = breed;
        this.birthDate = birthDate;
        this.gender = gender;
        this.size = size;
        this.description = description;
        this.imageBase64 = imageBase64;
    }

    public String getName() { return name; }
    public String getBreed() { return breed; }
    public String getBirthDate() { return birthDate; }
    public String getGender() { return gender; }
    public String getSize() { return size; }
    public String getDescription() { return description; }
    public String getImageBase64() { return imageBase64; }
    public void setName(String name) { this.name = name; }
    public void setBreed(String breed) { this.breed = breed; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public void setGender(String gender) { this.gender = gender; }
    public void setSize(String size) { this.size = size; }
    public void setDescription(String description) { this.description = description; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
}