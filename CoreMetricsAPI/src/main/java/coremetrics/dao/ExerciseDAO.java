package coremetrics.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ExerciseDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/core_metrics";
    private static final String USER = "root";
    private static final String PASS = "Keshav@123";

    public void printAllExercises() {
        String sql = "SELECT exercise_id, name, type, met_value FROM exercise_dict ORDER BY exercise_id";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.println(rs.getInt("exercise_id") + " = " + rs.getString("name")
                        + " (" + rs.getString("type") + ", MET: " + rs.getDouble("met_value") + ")");
            }
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int addCustomExercise(String name, String type, double metValue, int createdByUid) {
        String sql = "INSERT INTO exercise_dict (name, type, met_value, created_by_uid) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.setString(2, type);
            stmt.setDouble(3, metValue);
            stmt.setInt(4, createdByUid);
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

    public List<String> getAllExercisesWithMaxWeight(int userId) {
        List<String> summary = new ArrayList<>();
        String sql = "SELECT e.exercise_id, e.name, e.type, COALESCE(MAX(sl.weight_kg), 0) AS max_weight_kg "
                   + "FROM exercise_dict e "
                   + "LEFT JOIN strength_logs sl ON e.exercise_id = sl.exercise_id AND sl.user_id = ? "
                   + "GROUP BY e.exercise_id, e.name, e.type "
                   + "ORDER BY e.exercise_id";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                double maxWt = rs.getDouble("max_weight_kg");
                String status = maxWt > 0 ? (maxWt + " kg max") : "No strength logs yet";
                summary.add(rs.getString("name") + " (" + rs.getString("type") + ") -> " + status);
            }
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
        return summary;
    }
}