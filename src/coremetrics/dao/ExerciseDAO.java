package coremetrics.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ExerciseDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/core_metrics";
    private static final String USER = "root";
    private static final String PASS = "meetmadaan26.";

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
}