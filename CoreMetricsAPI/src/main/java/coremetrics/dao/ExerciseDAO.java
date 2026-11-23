package coremetrics.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExerciseDAO {

    public List<Map<String, Object>> getAllExercises() {
        List<Map<String, Object>> exercises = new ArrayList<>();
        String sql = "SELECT exercise_id, name, type, met_value, created_by_uid "
                + "FROM exercise_dict ORDER BY type, name, exercise_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> exercise = new LinkedHashMap<>();
                exercise.put("exerciseId", rs.getInt("exercise_id"));
                exercise.put("name", rs.getString("name"));
                exercise.put("type", rs.getString("type"));
                exercise.put("metValue", rs.getDouble("met_value"));
                int createdByUid = rs.getInt("created_by_uid");
                exercise.put("createdByUid", rs.wasNull() ? null : createdByUid);
                exercises.add(exercise);
            }
            return exercises;
        } catch (SQLException e) {
            throw new DatabaseException("load exercise dictionary", e);
        }
    }

    public int addCustomExercise(String name, String type, double metValue, int createdByUid) {
        String sql = "INSERT INTO exercise_dict (name, type, met_value, created_by_uid) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name.trim());
            stmt.setString(2, type.toLowerCase());
            stmt.setDouble(3, metValue);
            stmt.setInt(4, createdByUid);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DatabaseException("create custom exercise (no generated id returned)", null);
        } catch (SQLException e) {
            throw new DatabaseException("create custom exercise", e);
        }
    }

    public boolean isExerciseType(int exerciseId, String expectedType) {
        String sql = "SELECT 1 FROM exercise_dict WHERE exercise_id = ? AND type = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, exerciseId);
            stmt.setString(2, expectedType.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("validate exercise type", e);
        }
    }

    public List<Map<String, Object>> getAllExercisesWithMaxWeight(int userId) {
        List<Map<String, Object>> summary = new ArrayList<>();
        String sql = "SELECT e.exercise_id, e.name, e.type, MAX(sl.weight_kg) AS max_weight_kg "
                + "FROM exercise_dict e "
                + "LEFT JOIN strength_logs sl ON e.exercise_id = sl.exercise_id AND sl.user_id = ? "
                + "WHERE e.type = 'strength' "
                + "GROUP BY e.exercise_id, e.name, e.type "
                + "ORDER BY e.name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("exerciseId", rs.getInt("exercise_id"));
                    row.put("name", rs.getString("name"));
                    row.put("type", rs.getString("type"));
                    double maxWeight = rs.getDouble("max_weight_kg");
                    row.put("maxWeightKg", rs.wasNull() ? null : maxWeight);
                    summary.add(row);
                }
            }
            return summary;
        } catch (SQLException e) {
            throw new DatabaseException("load exercise max-weight summary", e);
        }
    }
}
