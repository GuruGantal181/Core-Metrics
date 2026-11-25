package coremetrics.models;

import java.sql.Timestamp;

public class Workout extends ExerciseLog {
    private double weightKg;
    private int reps;

    public Workout() {
    }

    public Workout(int logId, int userId, int exerciseId, double weightKg, int reps) {
        this(logId, userId, exerciseId, weightKg, reps, null);
    }

    public Workout(int logId, int userId, int exerciseId, double weightKg, int reps, Timestamp loggedAt) {
        super(logId, userId, exerciseId, loggedAt);
        this.weightKg = weightKg;
        this.reps = reps;
    }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }

    @Override
    public String describe() {
        return weightKg + " kg x " + reps + " reps (exercise " + getExerciseId() + ")";
    }

    /** Strength sets have no logged calorie value, so they do not affect energy balance. */
    @Override
    public double energyBalanceKcal() {
        return 0.0;
    }
}
