package coremetrics.dao;

import coremetrics.models.Workout;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkoutDAO {

    public int saveWorkout(Workout workout) {
        String sql = "INSERT INTO strength_logs (user_id, exercise_id, weight_kg, reps) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, workout.getUserId());
            stmt.setInt(2, workout.getExerciseId());
            stmt.setDouble(3, workout.getWeightKg());
            stmt.setInt(4, workout.getReps());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DatabaseException("save strength workout (no generated id returned)", null);
        } catch (SQLException e) {
            throw new DatabaseException("save strength workout", e);
        }
    }


    /** Executes the cursor-based CalculateTotalStrengthVolume stored procedure. */
    public double getTotalStrengthVolumeFromProcedure(int userId) {
        String sql = "{CALL CalculateTotalStrengthVolume(?, ?)}";

        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, userId);
            stmt.registerOutParameter(2, Types.DECIMAL);
            stmt.execute();
            BigDecimal volume = stmt.getBigDecimal(2);
            return volume == null ? 0.0 : volume.doubleValue();
        } catch (SQLException e) {
            throw new DatabaseException("execute CalculateTotalStrengthVolume cursor procedure", e);
        }
    }

    public List<Workout> getWorkoutsByUserId(int userId) {
        List<Workout> history = new ArrayList<>();
        String sql = "SELECT log_id, user_id, exercise_id, weight_kg, reps, logged_at "
                + "FROM strength_logs WHERE user_id = ? ORDER BY logged_at DESC, log_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp loggedAt = rs.getTimestamp("logged_at");
                    history.add(new Workout(
                            rs.getInt("log_id"),
                            rs.getInt("user_id"),
                            rs.getInt("exercise_id"),
                            rs.getDouble("weight_kg"),
                            rs.getInt("reps"),
                            loggedAt
                    ));
                }
            }
            return history;
        } catch (SQLException e) {
            throw new DatabaseException("load strength workout history", e);
        }
    }

    public List<Map<String, Object>> getMaxWeightPerExercise(int userId) {
        List<Map<String, Object>> maxLifts = new ArrayList<>();
        String sql = "SELECT e.exercise_id, e.name, MAX(s.weight_kg) AS max_weight "
                + "FROM strength_logs s "
                + "INNER JOIN exercise_dict e ON s.exercise_id = e.exercise_id "
                + "WHERE s.user_id = ? AND e.type = 'strength' "
                + "GROUP BY e.exercise_id, e.name ORDER BY e.name, e.exercise_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("exerciseId", rs.getInt("exercise_id"));
                    row.put("name", rs.getString("name"));
                    row.put("maxWeightKg", rs.getDouble("max_weight"));
                    maxLifts.add(row);
                }
            }
            return maxLifts;
        } catch (SQLException e) {
            throw new DatabaseException("load max weight per exercise", e);
        }
    }
}
