public class Dog {
    public String name;
    public String breed;
    public int age;
    public String profileImageUrl;
    public String birthday;
    public String specialCare;

    public Dog() {
    }

    public Dog(String name, String breed, int age, String profileImageUrl) {
        this.name = name;
        this.breed = breed;
        this.age = age;
        this.profileImageUrl = profileImageUrl;
    }

    public Dog(String name, String breed, int age, String profileImageUrl, String birthday, String specialCare) {
        this.name = name;
        this.breed = breed;
        this.age = age;
        this.profileImageUrl = profileImageUrl;
        this.birthday = birthday;
        this.specialCare = specialCare;
    }
}