package coremetrics.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import coremetrics.models.User;

public class UserDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/core_metrics";
    private static final String USER = "root";
    private static final String PASS = "meetmadaan26.";

    // CREATE 
    public int saveUser(User user) {
        String sql = "INSERT INTO USER_PROFILES (weight_kg, height_cm, age) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            stmt.setDouble(1, user.getWeightKg());
            stmt.setDouble(2, user.getHeightCm());
            stmt.setInt(3, user.getAge());
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; 
    }

    // READ
    public User getUserById(int id) {
        String sql = "SELECT * FROM USER_PROFILES WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new User(
                    rs.getInt("id"),
                    rs.getDouble("weight_kg"),
                    rs.getDouble("height_cm"),
                    rs.getInt("age")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}