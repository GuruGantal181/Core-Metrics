package coremetrics.models;

import java.sql.Timestamp;

/** Multilevel inheritance: LogEntry -> ExerciseLog -> Workout / CardioLog. */
public abstract class ExerciseLog extends LogEntry {
    private int exerciseId;
    private Timestamp loggedAt;

    protected ExerciseLog() {
    }

    protected ExerciseLog(int logId, int userId, int exerciseId, Timestamp loggedAt) {
        super(logId, userId);
        this.exerciseId = exerciseId;
        this.loggedAt = copyOf(loggedAt);
    }

    public int getExerciseId() { return exerciseId; }
    public void setExerciseId(int exerciseId) { this.exerciseId = exerciseId; }

    public Timestamp getLoggedAt() { return copyOf(loggedAt); }
    public void setLoggedAt(Timestamp loggedAt) { this.loggedAt = copyOf(loggedAt); }
}
