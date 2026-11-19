package coremetrics.models;

import java.sql.Timestamp;

public class CardioLog {
    private int logId;
    private int userId;
    private int exerciseId;
    private double inclinePct;
    private int durationM;
    private double caloriesOut;
    private Timestamp loggedAt;

    public CardioLog() {
    }

    public CardioLog(int logId, int userId, int exerciseId, double inclinePct, int durationM, double caloriesOut) {
        this.logId = logId;
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.inclinePct = inclinePct;
        this.durationM = durationM;
        this.caloriesOut = caloriesOut;
    }

    public CardioLog(int logId, int userId, int exerciseId, double inclinePct, int durationM, double caloriesOut, Timestamp loggedAt) {
        this.logId = logId;
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.inclinePct = inclinePct;
        this.durationM = durationM;
        this.caloriesOut = caloriesOut;
        this.loggedAt = loggedAt;
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getExerciseId() { return exerciseId; }
    public void setExerciseId(int exerciseId) { this.exerciseId = exerciseId; }

    public double getInclinePct() { return inclinePct; }
    public void setInclinePct(double inclinePct) { this.inclinePct = inclinePct; }

    public int getDurationM() { return durationM; }
    public void setDurationM(int durationM) { this.durationM = durationM; }

    public double getCaloriesOut() { return caloriesOut; }
    public void setCaloriesOut(double caloriesOut) { this.caloriesOut = caloriesOut; }

    public Timestamp getLoggedAt() { return loggedAt; }
    public void setLoggedAt(Timestamp loggedAt) { this.loggedAt = loggedAt; }
}