package coremetrics.models;
import java.sql.Timestamp;

public class Workout {
    private int logId;
    private int userId;
    private int exerciseId;
    private double weightKg;
    private int reps;
    private Timestamp loggedAt;

    public Workout() {
    }

    public Workout(int logId, int userId, int exerciseId, double weightKg, int reps) {
        this.logId = logId;
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.weightKg = weightKg;
        this.reps = reps;
    }

    public Workout(int logId, int userId, int exerciseId, double weightKg, int reps, Timestamp loggedAt) {
        this.logId = logId;
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.weightKg = weightKg;
        this.reps = reps;
        this.loggedAt = loggedAt;
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getExerciseId() { return exerciseId; }
    public void setExerciseId(int exerciseId) { this.exerciseId = exerciseId; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }

    public Timestamp getLoggedAt() { return loggedAt; }
    public void setLoggedAt(Timestamp loggedAt) { this.loggedAt = loggedAt; }
}