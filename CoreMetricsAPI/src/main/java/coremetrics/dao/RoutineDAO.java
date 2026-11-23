package coremetrics.dao;

import coremetrics.models.Routine;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoutineDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/core_metrics";
    private static final String USER = "root";
    private static final String PASS = "Keshav@123";

    public int createRoutine(Routine routine) {
        String sql = "INSERT INTO routines (user_id, routine_name) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, routine.getUserId());
            stmt.setString(2, routine.getRoutineName());
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

    public void addExerciseToRoutine(int routineId, int exerciseId, int defaultSets, int defaultReps, double defaultIncl, int defaultDur) {
        String sql = "INSERT INTO routine_map (routine_id, exercise_id, default_sets, default_reps, default_incl, default_dur) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, routineId);
            stmt.setInt(2, exerciseId);
            stmt.setInt(3, defaultSets);
            stmt.setInt(4, defaultReps);
            stmt.setDouble(5, defaultIncl);
            stmt.setInt(6, defaultDur);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // TRANSACTION (COMMIT / ROLLBACK): Atomically create a routine and map its first exercise
    public int createRoutineWithExerciseAtomic(int userId, String routineName, int exerciseId,
                                               int defaultSets, int defaultReps,
                                               double defaultIncl, int defaultDur) {
        String insertRoutineSql = "INSERT INTO routines (user_id, routine_name) VALUES (?, ?)";
        String insertMapSql = "INSERT INTO routine_map (routine_id, exercise_id, default_sets, default_reps, default_incl, default_dur) "
                            + "VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL, USER, PASS);
            conn.setAutoCommit(false); // Begin explicit transaction

            int generatedRoutineId = -1;
            try (PreparedStatement routineStmt = conn.prepareStatement(insertRoutineSql, Statement.RETURN_GENERATED_KEYS)) {
                routineStmt.setInt(1, userId);
                routineStmt.setString(2, routineName);
                routineStmt.executeUpdate();

                ResultSet rs = routineStmt.getGeneratedKeys();
                if (rs.next()) {
                    generatedRoutineId = rs.getInt(1);
                }
            }

            try (PreparedStatement mapStmt = conn.prepareStatement(insertMapSql)) {
                mapStmt.setInt(1, generatedRoutineId);
                mapStmt.setInt(2, exerciseId);
                mapStmt.setInt(3, defaultSets);
                mapStmt.setInt(4, defaultReps);
                mapStmt.setDouble(5, defaultIncl);
                mapStmt.setInt(6, defaultDur);
                mapStmt.executeUpdate();
            }

            conn.commit(); // Commit both inserts atomically
            return generatedRoutineId;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Roll back routine creation if mapping fails (e.g., invalid exerciseId FK)
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return -1;
    }

    // DELETE: Removes routine and automatically cascades deletion to routine_map
    public boolean deleteRoutine(int routineId) {
        String sql = "DELETE FROM routines WHERE routine_id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, routineId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Routine> getRoutinesByUserId(int userId) {
        List<Routine> routines = new ArrayList<>();
        String sql = "SELECT routine_id, user_id, routine_name FROM routines WHERE user_id = ? ORDER BY routine_id";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                routines.add(new Routine(
                        rs.getInt("routine_id"),
                        rs.getInt("user_id"),
                        rs.getString("routine_name")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return routines;
    }

    public List<String> getRoutineExercises(int routineId) {
        List<String> items = new ArrayList<>();
        String sql = "SELECT e.name, e.type, rm.default_sets, rm.default_reps, rm.default_incl, rm.default_dur "
                   + "FROM routine_map rm "
                   + "INNER JOIN exercise_dict e ON rm.exercise_id = e.exercise_id "
                   + "WHERE rm.routine_id = ? "
                   + "ORDER BY e.exercise_id";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, routineId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                String type = rs.getString("type");
                if ("cardio".equalsIgnoreCase(type)) {
                    items.add(name + " [cardio] - " + rs.getInt("default_dur") + " min @ "
                            + rs.getDouble("default_incl") + "% incline");
                } else {
                    items.add(name + " [strength] - " + rs.getInt("default_sets") + " sets x "
                            + rs.getInt("default_reps") + " reps");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
}