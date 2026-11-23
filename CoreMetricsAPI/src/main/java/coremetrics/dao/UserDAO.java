package coremetrics.dao;

import coremetrics.models.User;

import java.sql.Connection;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public int saveUser(User user) {
        String sql = "INSERT INTO user_profiles (weight_kg, height_cm, age, gender) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setDouble(1, user.getWeightKg());
            stmt.setDouble(2, user.getHeightCm());
            stmt.setInt(3, user.getAge());
            stmt.setString(4, user.getGender());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DatabaseException("create user profile (no generated id returned)", null);
        } catch (SQLException e) {
            throw new DatabaseException("create user profile", e);
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, weight_kg, height_cm, age, gender FROM user_profiles ORDER BY user_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapUser(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new DatabaseException("load user profiles", e);
        }
    }

    public User getUserById(int userId) {
        String sql = "SELECT user_id, weight_kg, height_cm, age, gender FROM user_profiles WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("load user profile", e);
        }
    }


    public Double getBmrFromSqlFunction(int userId) {
        String sql = "SELECT CalculateUserBMR(?) AS bmr";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                BigDecimal bmr = rs.getBigDecimal("bmr");
                return bmr == null ? null : bmr.doubleValue();
            }
        } catch (SQLException e) {
            throw new DatabaseException("calculate BMR using CalculateUserBMR SQL function", e);
        }
    }

    public boolean updateUserProfile(int userId, User user) {
        String sql = "UPDATE user_profiles SET weight_kg = ?, height_cm = ?, age = ?, gender = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, user.getWeightKg());
            stmt.setDouble(2, user.getHeightCm());
            stmt.setInt(3, user.getAge());
            stmt.setString(4, user.getGender());
            stmt.setInt(5, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("update user profile", e);
        }
    }

    public boolean updateUserWeight(int userId, double newWeightKg) {
        String sql = "UPDATE user_profiles SET weight_kg = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newWeightKg);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("update user weight", e);
        }
    }

    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM user_profiles WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("delete user profile", e);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getDouble("weight_kg"),
                rs.getDouble("height_cm"),
                rs.getInt("age"),
                rs.getString("gender")
        );
    }
}
