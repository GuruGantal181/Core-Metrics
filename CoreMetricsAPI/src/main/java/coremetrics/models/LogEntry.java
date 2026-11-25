package coremetrics.models;

import java.sql.Timestamp;
import java.util.Collection;

/**
 * Abstract base class for every per-user log row (workout, cardio, diet).
 * Holds the shared, encapsulated identity fields and declares the behaviour subclasses must provide.
 */
public abstract class LogEntry implements Describable {
    private int logId;
    private int userId;

    protected LogEntry() {
    }

    protected LogEntry(int logId, int userId) {
        this.logId = logId;
        this.userId = userId;
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    /**
     * Polymorphic hook: this entry's effect on daily energy balance in kcal
     * (positive = intake, negative = burn, 0 = not tracked).
     */
    public abstract double energyBalanceKcal();

    /** Overload of describe(): optionally prefixes the log id. */
    public final String describe(boolean includeId) {
        return includeId ? "#" + logId + " " + describe() : describe();
    }

    /** Dynamic dispatch: each element's own energyBalanceKcal() override is invoked. */
    public static double netEnergyKcal(Collection<? extends LogEntry> entries) {
        double total = 0;
        for (LogEntry entry : entries) {
            total += entry.energyBalanceKcal();
        }
        return total;
    }

    /** Defensive copy so callers cannot mutate a stored Timestamp (Timestamp is mutable). */
    protected static Timestamp copyOf(Timestamp source) {
        if (source == null) {
            return null;
        }
        Timestamp copy = new Timestamp(source.getTime());
        copy.setNanos(source.getNanos());
        return copy;
    }

    @Override
    public String toString() {
        return summaryLine();
    }
}
