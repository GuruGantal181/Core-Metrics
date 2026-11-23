package coremetrics.dao;

import coremetrics.models.CardioLog;
import coremetrics.utils.FitnessMath;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CardioLogDAO {

    public int saveCardioLog(CardioLog log) {
        double calories = log.getCaloriesOut();
        if (calories <= 0) {
            double metValue = getExerciseMetValue(log.getExerciseId());
            double weightKg = getUserWeightKg(log.getUserId());
            calories = FitnessMath.calculateMETCalories(metValue, weightKg, log.getDurationM());
        }
        if (!Double.isFinite(calories) || calories < 0 || calories > 9999.99) {
            throw new IllegalArgumentException(
                    "Cardio calories must fit the database limit of 0 to 9999.99 kcal.");
        }

        String sql = "INSERT INTO cardio_logs (user_id, exercise_id, incline_pct, duration_m, calories_out) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, log.getUserId());
            stmt.setInt(2, log.getExerciseId());
            stmt.setDouble(3, log.getInclinePct());
            stmt.setInt(4, log.getDurationM());
            stmt.setDouble(5, calories);
            stmt.executeUpdate();

            log.setCaloriesOut(calories);
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DatabaseException("save cardio session (no generated id returned)", null);
        } catch (SQLException e) {
            throw new DatabaseException("save cardio session", e);
        }
    }

    public List<CardioLog> getCardioLogsByUserId(int userId) {
        List<CardioLog> logs = new ArrayList<>();
        String sql = "SELECT log_id, user_id, exercise_id, incline_pct, duration_m, calories_out, logged_at "
                + "FROM cardio_logs WHERE user_id = ? ORDER BY logged_at DESC, log_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(new CardioLog(
                            rs.getInt("log_id"),
                            rs.getInt("user_id"),
                            rs.getInt("exercise_id"),
                            rs.getDouble("incline_pct"),
                            rs.getInt("duration_m"),
                            rs.getDouble("calories_out"),
                            rs.getTimestamp("logged_at")
                    ));
                }
            }
            return logs;
        } catch (SQLException e) {
            throw new DatabaseException("load cardio history", e);
        }
    }

    public double getTotalCardioCaloriesInRange(int userId, Timestamp startInclusive, Timestamp endExclusive) {
        String sql = "SELECT COALESCE(SUM(calories_out), 0) AS total_burned "
                + "FROM cardio_logs WHERE user_id = ? AND logged_at >= ? AND logged_at < ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setTimestamp(2, startInclusive);
            stmt.setTimestamp(3, endExclusive);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble("total_burned") : 0.0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("calculate cardio calories for date range", e);
        }
    }

    private double getExerciseMetValue(int exerciseId) {
        String sql = "SELECT met_value FROM exercise_dict WHERE exercise_id = ? AND type = 'cardio'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, exerciseId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("met_value");
                }
            }
            throw new IllegalArgumentException("The selected exercise does not exist or is not a cardio exercise.");
        } catch (SQLException e) {
            throw new DatabaseException("load MET value for cardio exercise", e);
        }
    }

    private double getUserWeightKg(int userId) {
        String sql = "SELECT weight_kg FROM user_profiles WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("weight_kg");
                }
            }
            throw new IllegalArgumentException("The selected user profile does not exist.");
        } catch (SQLException e) {
            throw new DatabaseException("load user weight for cardio calculation", e);
        }
    }
}
