package coremetrics.dao;

import coremetrics.models.CardioLog;
import coremetrics.utils.FitnessMath;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CardioLogDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/core_metrics";
    private static final String USER = "root";
    private static final String PASS = "Keshav@123";

    public double getExerciseMetValue(int exerciseId) {
        String sql = "SELECT met_value FROM exercise_dict WHERE exercise_id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, exerciseId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("met_value");
            }
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
        return 5.0; // Default fallback MET
    }

    public double getUserWeightKg(int userId) {
        String sql = "SELECT weight_kg FROM user_profiles WHERE user_id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("weight_kg");
            }
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
        return 70.0; // Default fallback weight
    }

    public int saveCardioLog(CardioLog log) {
        double metValue = getExerciseMetValue(log.getExerciseId());
        double weightKg = getUserWeightKg(log.getUserId());
        double computedCalories = log.getCaloriesOut() > 0
                ? log.getCaloriesOut()
                : FitnessMath.calculateMETCalories(metValue, weightKg, log.getDurationM());

        String sql = "INSERT INTO cardio_logs (user_id, exercise_id, incline_pct, duration_m, calories_out) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, log.getUserId());
            stmt.setInt(2, log.getExerciseId());
            stmt.setDouble(3, log.getInclinePct());
            stmt.setInt(4, log.getDurationM());
            stmt.setDouble(5, computedCalories);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<CardioLog> getCardioLogsByUserId(int userId) {
        List<CardioLog> logs = new ArrayList<>();
        String sql = "SELECT log_id, user_id, exercise_id, incline_pct, duration_m, calories_out, logged_at "
                   + "FROM cardio_logs WHERE user_id = ? ORDER BY logged_at DESC";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
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
        catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public double getTotalCardioCaloriesInRange(int userId, Timestamp startInclusive, Timestamp endExclusive) {
        String sql = "SELECT COALESCE(SUM(calories_out), 0) AS total_burned "
                   + "FROM cardio_logs "
                   + "WHERE user_id = ? AND logged_at >= ? AND logged_at < ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setTimestamp(2, startInclusive);
            stmt.setTimestamp(3, endExclusive);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("total_burned");
            }
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}