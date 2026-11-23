package coremetrics.dao;

import coremetrics.models.DietLog;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

public class DietLogDAO {

    public int saveDietLog(DietLog log) {
        String sql = "INSERT INTO diet_logs (user_id, food_item, calories, protein_g) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, log.getUserId());
            stmt.setString(2, log.getFoodItem().trim());
            stmt.setInt(3, log.getCalories());
            stmt.setDouble(4, log.getProteinG());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DatabaseException("save meal (no generated id returned)", null);
        } catch (SQLException e) {
            throw new DatabaseException("save meal", e);
        }
    }

    public List<DietLog> getDietLogsByUserId(int userId) {
        List<DietLog> history = new ArrayList<>();
        String sql = "SELECT log_id, user_id, food_item, calories, protein_g, consumed_at "
                + "FROM diet_logs WHERE user_id = ? ORDER BY consumed_at DESC, log_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    history.add(new DietLog(
                            rs.getInt("log_id"),
                            rs.getInt("user_id"),
                            rs.getString("food_item"),
                            rs.getInt("calories"),
                            rs.getDouble("protein_g"),
                            rs.getTimestamp("consumed_at")
                    ));
                }
            }
            return history;
        } catch (SQLException e) {
            throw new DatabaseException("load diet history", e);
        }
    }


    /** Reads daily totals from the user_daily_nutrition_view SQL view. */
    public List<Map<String, Object>> getDailyNutritionFromView(int userId) {
        List<Map<String, Object>> days = new ArrayList<>();
        String sql = "SELECT log_date, total_calories_in, total_protein_g "
                + "FROM user_daily_nutrition_view WHERE user_id = ? ORDER BY log_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("logDate", rs.getDate("log_date").toString());
                    row.put("totalCaloriesIn", rs.getInt("total_calories_in"));
                    row.put("totalProteinG", rs.getDouble("total_protein_g"));
                    days.add(row);
                }
            }
            return days;
        } catch (SQLException e) {
            throw new DatabaseException("read daily nutrition SQL view", e);
        }
    }

    /** Executes GetUserWeeklySummary and returns its single-row result. */
    public Map<String, Object> getWeeklySummaryFromProcedure(int userId) {
        String sql = "{CALL GetUserWeeklySummary(?)}";
        Map<String, Object> summary = new LinkedHashMap<>();

        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, userId);
            boolean hasResultSet = stmt.execute();
            while (!hasResultSet && stmt.getUpdateCount() != -1) {
                hasResultSet = stmt.getMoreResults();
            }
            if (!hasResultSet) {
                throw new SQLException("GetUserWeeklySummary did not return a result set.");
            }

            try (ResultSet rs = stmt.getResultSet()) {
                if (rs.next()) {
                    summary.put("weeklyCaloriesIn", rs.getLong("weekly_calories_in"));
                    summary.put("weeklyCardioOut", rs.getDouble("weekly_cardio_out"));
                    summary.put("weeklyStrengthSets", rs.getInt("weekly_strength_sets"));
                }
            }
            return summary;
        } catch (SQLException e) {
            throw new DatabaseException("execute GetUserWeeklySummary SQL procedure", e);
        }
    }

    public long getTotalCaloriesInRange(int userId, Timestamp startInclusive, Timestamp endExclusive) {
        String sql = "SELECT COALESCE(SUM(calories), 0) AS total_cal "
                + "FROM diet_logs WHERE user_id = ? AND consumed_at >= ? AND consumed_at < ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setTimestamp(2, startInclusive);
            stmt.setTimestamp(3, endExclusive);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong("total_cal") : 0L;
            }
        } catch (SQLException e) {
            throw new DatabaseException("calculate calorie intake for date range", e);
        }
    }

    public double getNetCaloriesInRange(int userId, Timestamp startInclusive,
                                        Timestamp endExclusive, CardioLogDAO cardioDao) {
        long caloriesIn = getTotalCaloriesInRange(userId, startInclusive, endExclusive);
        double cardioOut = cardioDao.getTotalCardioCaloriesInRange(userId, startInclusive, endExclusive);
        return caloriesIn - cardioOut;
    }

    public double getCaloricDeficitInRange(int userId, Timestamp startInclusive, Timestamp endExclusive,
                                           double bmr, int daysInRange, CardioLogDAO cardioDao) {
        long caloriesIn = getTotalCaloriesInRange(userId, startInclusive, endExclusive);
        double cardioOut = cardioDao.getTotalCardioCaloriesInRange(userId, startInclusive, endExclusive);
        return (bmr * daysInRange) + cardioOut - caloriesIn;
    }

    /** Returns meals above this user's average logged-meal calorie count. */
    public List<DietLog> getAboveAverageCalorieMeals(int userId) {
        List<DietLog> result = new ArrayList<>();
        String sql = "SELECT log_id, user_id, food_item, calories, protein_g, consumed_at "
                + "FROM diet_logs WHERE user_id = ? AND calories > "
                + "(SELECT AVG(calories) FROM diet_logs WHERE user_id = ?) "
                + "ORDER BY calories DESC, consumed_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new DietLog(
                            rs.getInt("log_id"),
                            rs.getInt("user_id"),
                            rs.getString("food_item"),
                            rs.getInt("calories"),
                            rs.getDouble("protein_g"),
                            rs.getTimestamp("consumed_at")
                    ));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DatabaseException("load above-average meals", e);
        }
    }

    /** Returns dates whose summed intake is at least the supplied threshold. */
    public List<String> getHighCalorieDays(int userId, int minDailyCalories) {
        List<String> days = new ArrayList<>();
        String sql = "SELECT DATE(consumed_at) AS log_date, SUM(calories) AS daily_total "
                + "FROM diet_logs WHERE user_id = ? GROUP BY DATE(consumed_at) "
                + "HAVING SUM(calories) >= ? ORDER BY log_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, minDailyCalories);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    days.add(rs.getDate("log_date") + " -> " + rs.getLong("daily_total") + " kcal");
                }
            }
            return days;
        } catch (SQLException e) {
            throw new DatabaseException("load high-calorie days", e);
        }
    }
}
