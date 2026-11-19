package coremetrics.models;
import java.sql.Timestamp;

public class Workout {
    private int id;
    private int userId;
    private String exerciseName;
    private double weightLifted;
    private int reps;
    private int sets;
    private Timestamp logDate;

    public Workout(int id, int userId, String exerciseName, double weightLifted, int reps, int sets) {
        this.id = id;
        this.userId = userId;
        this.exerciseName = exerciseName;
        this.weightLifted = weightLifted;
        this.reps = reps;
        this.sets = sets;
    }

    public Workout(int id, int userId, String exerciseName, double weightLifted, int reps, int sets, Timestamp logDate) {
        this.id = id;
        this.userId = userId;
        this.exerciseName = exerciseName;
        this.weightLifted = weightLifted;
        this.reps = reps;
        this.sets = sets;
        this.logDate = logDate;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getExerciseName() { return exerciseName; }
    public double getWeightLifted() { return weightLifted; }
    public int getReps() { return reps; }
    public int getSets() { return sets; }
    public Timestamp getLogDate() { return logDate; }
}