package coremetrics.models;
import java.sql.Timestamp;

public class DietLog {
    private int logId;
    private int userId;
    private String foodItem;
    private int calories;
    private double proteinG;
    private Timestamp consumedAt;

    public DietLog() {
    }

    public DietLog(int logId, int userId, String foodItem, int calories, double proteinG) {
        this.logId = logId;
        this.userId = userId;
        this.foodItem = foodItem;
        this.calories = calories;
        this.proteinG = proteinG;
    }

    public DietLog(int logId, int userId, String foodItem, int calories, double proteinG, Timestamp consumedAt) {
        this.logId = logId;
        this.userId = userId;
        this.foodItem = foodItem;
        this.calories = calories;
        this.proteinG = proteinG;
        this.consumedAt = consumedAt;
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getFoodItem() { return foodItem; }
    public void setFoodItem(String foodItem) { this.foodItem = foodItem; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public double getProteinG() { return proteinG; }
    public void setProteinG(double proteinG) { this.proteinG = proteinG; }

    public Timestamp getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Timestamp consumedAt) { this.consumedAt = consumedAt; }
}