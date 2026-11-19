package coremetrics.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import coremetrics.models.Workout;

public class WorkoutDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/core_metrics";
    private static final String USER = "root";
    private static final String PASS = "meetmadaan26.";

    // CREATE
    public void saveWorkout(Workout workout) {
        String sql = "INSERT INTO STRENGTH_LOGS (user_id, exercise_name, weight_lifted, reps, sets) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, workout.getUserId());
            stmt.setString(2, workout.getExerciseName());
            stmt.setDouble(3, workout.getWeightLifted());
            stmt.setInt(4, workout.getReps());
            stmt.setInt(5, workout.getSets());
            stmt.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // READ
    public List<Workout> getWorkoutsByUserId(int userId) {
        List<Workout> history = new ArrayList<>();
        String sql = "SELECT * FROM STRENGTH_LOGS WHERE user_id = ?";
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String exercise = rs.getString("exercise_name");
                double weight = rs.getDouble("weight_lifted");
                int reps = rs.getInt("reps");
                int sets = rs.getInt("sets");
                java.sql.Timestamp logDate = rs.getTimestamp("log_date");
                
                history.add(new Workout(id, userId, exercise, weight, reps, sets, logDate));
            }
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }
}