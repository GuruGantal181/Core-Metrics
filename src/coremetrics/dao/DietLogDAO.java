package coremetrics.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import coremetrics.models.DietLog;

public class DietLogDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/core_metrics";
    private static final String USER = "root";
    private static final String PASS = "meetmadaan26.";

    // CREATE
    public void saveDietLog(DietLog log) {
        String sql = "INSERT INTO DIET_LOGS (user_id, food_item, calories) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, log.getUserId());
            stmt.setString(2, log.getFoodItem());
            stmt.setInt(3, log.getCalories());
            stmt.executeUpdate();
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // READ
    public List<DietLog> getDietLogsByUserId(int userId) {
        List<DietLog> history = new ArrayList<>();
        String sql = "SELECT * FROM DIET_LOGS WHERE user_id = ?";
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String food = rs.getString("food_item");
                int calories = rs.getInt("calories");

                java.sql.Timestamp logDate = rs.getTimestamp("log_date");
                history.add(new DietLog(id, userId, food, calories, logDate));
            }
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }
}