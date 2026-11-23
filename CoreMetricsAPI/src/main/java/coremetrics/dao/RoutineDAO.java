package coremetrics.dao;

import coremetrics.models.Routine;
import coremetrics.models.RoutineExercise;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoutineDAO {

    public int createRoutine(Routine routine) {
        String sql = "INSERT INTO routines (user_id, routine_name) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, routine.getUserId());
            stmt.setString(2, routine.getRoutineName().trim());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DatabaseException("create routine (no generated id returned)", null);
        } catch (SQLException e) {
            throw new DatabaseException("create routine", e);
        }
    }

    public void addExerciseToRoutine(int routineId, int exerciseId, int defaultSets,
                                     int defaultReps, double defaultIncl, int defaultDur) {
        String sql = "INSERT INTO routine_map (routine_id, exercise_id, default_sets, default_reps, default_incl, default_dur) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, routineId);
            stmt.setInt(2, exerciseId);
            stmt.setInt(3, defaultSets);
            stmt.setInt(4, defaultReps);
            stmt.setDouble(5, defaultIncl);
            stmt.setInt(6, defaultDur);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("map exercise to routine", e);
        }
    }

    /** Creates a routine and its first mapped exercise in one transaction. */
    public int createRoutineWithExerciseAtomic(int userId, String routineName, int exerciseId,
                                               int defaultSets, int defaultReps,
                                               double defaultIncl, int defaultDur) {
        String insertRoutineSql = "INSERT INTO routines (user_id, routine_name) VALUES (?, ?)";
        String insertMapSql = "INSERT INTO routine_map (routine_id, exercise_id, default_sets, default_reps, default_incl, default_dur) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int routineId;
                try (PreparedStatement routineStmt = conn.prepareStatement(insertRoutineSql, Statement.RETURN_GENERATED_KEYS)) {
                    routineStmt.setInt(1, userId);
                    routineStmt.setString(2, routineName.trim());
                    routineStmt.executeUpdate();
                    try (ResultSet keys = routineStmt.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("No routine id was generated.");
                        }
                        routineId = keys.getInt(1);
                    }
                }

                try (PreparedStatement mapStmt = conn.prepareStatement(insertMapSql)) {
                    mapStmt.setInt(1, routineId);
                    mapStmt.setInt(2, exerciseId);
                    mapStmt.setInt(3, defaultSets);
                    mapStmt.setInt(4, defaultReps);
                    mapStmt.setDouble(5, defaultIncl);
                    mapStmt.setInt(6, defaultDur);
                    mapStmt.executeUpdate();
                }

                conn.commit();
                return routineId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("create routine and map its first exercise", e);
        }
    }

    public boolean deleteRoutine(int routineId) {
        String sql = "DELETE FROM routines WHERE routine_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, routineId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("delete routine", e);
        }
    }

    public List<Routine> getRoutinesByUserId(int userId) {
        List<Routine> routines = new ArrayList<>();
        String sql = "SELECT routine_id, user_id, routine_name FROM routines WHERE user_id = ? ORDER BY routine_name, routine_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    routines.add(new Routine(
                            rs.getInt("routine_id"),
                            rs.getInt("user_id"),
                            rs.getString("routine_name")
                    ));
                }
            }
            return routines;
        } catch (SQLException e) {
            throw new DatabaseException("load routines", e);
        }
    }

    public List<RoutineExercise> getRoutineExercises(int routineId) {
        List<RoutineExercise> items = new ArrayList<>();
        String sql = "SELECT e.exercise_id, e.name, e.type, rm.default_sets, rm.default_reps, rm.default_incl, rm.default_dur "
                + "FROM routine_map rm INNER JOIN exercise_dict e ON rm.exercise_id = e.exercise_id "
                + "WHERE rm.routine_id = ? ORDER BY e.name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, routineId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new RoutineExercise(
                            rs.getInt("exercise_id"),
                            rs.getString("name"),
                            rs.getString("type"),
                            rs.getInt("default_sets"),
                            rs.getInt("default_reps"),
                            rs.getDouble("default_incl"),
                            rs.getInt("default_dur")
                    ));
                }
            }
            return items;
        } catch (SQLException e) {
            throw new DatabaseException("load exercises mapped to routine", e);
        }
    }
}
