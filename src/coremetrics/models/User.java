package coremetrics.models;

public class User {
    private int id;
    private double weightKg;
    private double heightCm;
    private int age;

    public User(int id, double weightKg, double heightCm, int age) {
        this.id = id;
        this.weightKg = weightKg;
        this.heightCm = heightCm;
        this.age = age;
    }

    public int getId() { return id; }
    public double getWeightKg() { return weightKg; }
    public double getHeightCm() { return heightCm; }
    public int getAge() { return age; }
}
