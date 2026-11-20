package coremetrics.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import coremetrics.models.Workout;

public class WorkoutDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/core_metrics";
    private static final String USER = "root";
    private static final String PASS = "meetmadaan26.";

    // CREATE
    public void saveWorkout(Workout workout) {
        String sql = "INSERT INTO strength_logs (user_id, exercise_id, weight_kg, reps) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, workout.getUserId());
            stmt.setInt(2, workout.getExerciseId());
            stmt.setDouble(3, workout.getWeightKg());
            stmt.setInt(4, workout.getReps());
            stmt.executeUpdate();
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // READ
    public List<Workout> getWorkoutsByUserId(int userId) {
        List<Workout> history = new ArrayList<>();
        String sql = "SELECT * FROM strength_logs WHERE user_id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int logId = rs.getInt("log_id");
                int exerciseId = rs.getInt("exercise_id");
                double weightKg = rs.getDouble("weight_kg");
                int reps = rs.getInt("reps");
                Timestamp loggedAt = rs.getTimestamp("logged_at");

                history.add(new Workout(logId, userId, exerciseId, weightKg, reps, loggedAt));
            }
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    public Map<String, Double> getMaxWeightPerExercise(int userId) {
        Map<String, Double> maxLifts = new HashMap<>();
        String sql = "SELECT e.name, MAX(s.weight_kg) AS max_weight " +
                     "FROM strength_logs s " +
                     "INNER JOIN exercise_dict e ON s.exercise_id = e.exercise_id " +
                     "WHERE s.user_id = ? " +
                     "GROUP BY e.name";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                maxLifts.put(rs.getString("name"), rs.getDouble("max_weight"));
            }
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
        return maxLifts;
    }
}