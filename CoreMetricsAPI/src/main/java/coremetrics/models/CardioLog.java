package coremetrics.models;

import java.sql.Timestamp;

public class CardioLog extends ExerciseLog {
    private double inclinePct;
    private int durationM;
    private double caloriesOut;

    public CardioLog() {
    }

    public CardioLog(int logId, int userId, int exerciseId, double inclinePct, int durationM, double caloriesOut) {
        this(logId, userId, exerciseId, inclinePct, durationM, caloriesOut, null);
    }

    public CardioLog(int logId, int userId, int exerciseId, double inclinePct, int durationM, double caloriesOut, Timestamp loggedAt) {
        super(logId, userId, exerciseId, loggedAt);
        this.inclinePct = inclinePct;
        this.durationM = durationM;
        this.caloriesOut = caloriesOut;
    }

    public double getInclinePct() { return inclinePct; }
    public void setInclinePct(double inclinePct) { this.inclinePct = inclinePct; }

    public int getDurationM() { return durationM; }
    public void setDurationM(int durationM) { this.durationM = durationM; }

    public double getCaloriesOut() { return caloriesOut; }
    public void setCaloriesOut(double caloriesOut) { this.caloriesOut = caloriesOut; }

    @Override
    public String describe() {
        return durationM + " min at " + inclinePct + "% incline, " + caloriesOut + " kcal burned";
    }

    /** Cardio burn reduces energy balance. */
    @Override
    public double energyBalanceKcal() {
        return -caloriesOut;
    }
}
