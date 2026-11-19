package coremetrics.models;
import java.sql.Timestamp;

public class DietLog {
    private int id;
    private int userId;
    private String foodItem;
    private int calories;
    private Timestamp logDate;

    public DietLog(int id, int userId, String foodItem, int calories) {
        this.id = id;
        this.userId = userId;
        this.foodItem = foodItem;
        this.calories = calories;
    }

    public DietLog(int id, int userId, String foodItem, int calories, Timestamp logDate) {
        this.id = id;
        this.userId = userId;
        this.foodItem = foodItem;
        this.calories = calories;
        this.logDate = logDate;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getFoodItem() { return foodItem; }
    public int getCalories() { return calories; }
    public Timestamp getLogDate() { return logDate; }
}