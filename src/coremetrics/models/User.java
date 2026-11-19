package coremetrics.models;

public class User {
    private int userId;
    private double weightKg;
    private double heightCm;
    private int age;
    private String gender;

    public User() {
    }

    public User(int userId, double weightKg, double heightCm, int age, String gender) {
        this.userId = userId;
        this.weightKg = weightKg;
        this.heightCm = heightCm;
        this.age = age;
        this.gender = gender;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    public double getHeightCm() { return heightCm; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}