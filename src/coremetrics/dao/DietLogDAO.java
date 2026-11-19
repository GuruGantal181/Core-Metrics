package coremetrics.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import coremetrics.models.DietLog;

public class DietLogDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/core_metrics";
    private static final String USER = "root";
    private static final String PASS = "meetmadaan26.";

    // CREATE
    public void saveDietLog(DietLog log) {
        String sql = "INSERT INTO diet_logs (user_id, food_item, calories, protein_g) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, log.getUserId());
            stmt.setString(2, log.getFoodItem());
            stmt.setInt(3, log.getCalories());
            stmt.setDouble(4, log.getProteinG());
            stmt.executeUpdate();
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // READ
    public List<DietLog> getDietLogsByUserId(int userId) {
        List<DietLog> history = new ArrayList<>();
        String sql = "SELECT * FROM diet_logs WHERE user_id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int logId = rs.getInt("log_id");
                String food = rs.getString("food_item");
                int calories = rs.getInt("calories");
                double proteinG = rs.getDouble("protein_g");
                Timestamp consumedAt = rs.getTimestamp("consumed_at");

                history.add(new DietLog(logId, userId, food, calories, proteinG, consumedAt));
            }
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    public int getTotalCaloriesInRange(int userId, Timestamp start, Timestamp end) {
        String sql = "SELECT COALESCE(SUM(calories), 0) AS total_cal " +
                     "FROM diet_logs " +
                     "WHERE user_id = ? AND consumed_at >= ? AND consumed_at < ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setTimestamp(2, start);
            stmt.setTimestamp(3, end);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total_cal");
            }
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}