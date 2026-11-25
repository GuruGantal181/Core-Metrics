package coremetrics.models;

import java.sql.Timestamp;

public class DietLog extends LogEntry {
    private String foodItem;
    private int calories;
    private double proteinG;
    private Timestamp consumedAt;

    public DietLog() {
    }

    public DietLog(int logId, int userId, String foodItem, int calories, double proteinG) {
        this(logId, userId, foodItem, calories, proteinG, null);
    }

    public DietLog(int logId, int userId, String foodItem, int calories, double proteinG, Timestamp consumedAt) {
        super(logId, userId);
        this.foodItem = foodItem;
        this.calories = calories;
        this.proteinG = proteinG;
        this.consumedAt = copyOf(consumedAt);
    }

    public String getFoodItem() { return foodItem; }
    public void setFoodItem(String foodItem) { this.foodItem = foodItem; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public double getProteinG() { return proteinG; }
    public void setProteinG(double proteinG) { this.proteinG = proteinG; }

    public Timestamp getConsumedAt() { return copyOf(consumedAt); }
    public void setConsumedAt(Timestamp consumedAt) { this.consumedAt = copyOf(consumedAt); }

    @Override
    public String describe() {
        return foodItem + " - " + calories + " kcal, " + proteinG + " g protein";
    }

    /** Meals add to energy balance. */
    @Override
    public double energyBalanceKcal() {
        return calories;
    }
}
