package com.coremetrics.api.controller;

import coremetrics.dao.CardioLogDAO;
import coremetrics.dao.DietLogDAO;
import coremetrics.dao.ExerciseDAO;
import coremetrics.dao.RoutineDAO;
import coremetrics.dao.UserDAO;
import coremetrics.dao.WorkoutDAO;
import coremetrics.models.CardioLog;
import coremetrics.models.DietLog;
import coremetrics.models.Routine;
import coremetrics.models.RoutineExercise;
import coremetrics.models.User;
import coremetrics.models.Workout;
import coremetrics.utils.FitnessMath;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class fitnessController {

    private final UserDAO userDao = new UserDAO();
    private final WorkoutDAO workoutDao = new WorkoutDAO();
    private final CardioLogDAO cardioDao = new CardioLogDAO();
    private final DietLogDAO dietDao = new DietLogDAO();
    private final ExerciseDAO exerciseDao = new ExerciseDAO();
    private final RoutineDAO routineDao = new RoutineDAO();

    // ---------------------------------------------------------------------
    // Users
    // ---------------------------------------------------------------------

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userDao.getAllUsers();
    }

    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody User user) {
        validateUser(user);
        int id = userDao.saveUser(user);
        Map<String, Object> response = new HashMap<>();
        response.put("userId", id);
        response.put("bmr", FitnessMath.calculateBMR(
                user.getWeightKg(), user.getHeightCm(), user.getAge(), user.getGender()));
        return response;
    }

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable int id) {
        User user = userDao.getUserById(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found.");
        }
        return user;
    }

    @PutMapping("/users/{id}")
    public Map<String, Object> updateUserProfile(@PathVariable int id, @RequestBody User user) {
        validateUser(user);
        boolean updated = userDao.updateUserProfile(id, user);
        if (!updated) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found.");
        }
        return Map.of("updated", true);
    }

    @PutMapping("/users/{id}/weight")
    public Map<String, Object> updateUserWeight(@PathVariable int id, @RequestBody Map<String, Object> body) {
        double weightKg = requiredNumber(body, "weightKg").doubleValue();
        if (!Double.isFinite(weightKg) || weightKg <= 0 || weightKg > 999.99) {
            throw new IllegalArgumentException("Weight must be greater than 0 kg and no more than 999.99 kg.");
        }
        boolean updated = userDao.updateUserWeight(id, weightKg);
        if (!updated) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found.");
        }
        return Map.of("updated", true, "weightKg", weightKg);
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(@PathVariable int id) {
        boolean deleted = userDao.deleteUser(id);
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found.");
        }
        return Map.of("deleted", true);
    }

    // ---------------------------------------------------------------------
    // Exercise dictionary
    // ---------------------------------------------------------------------

    @GetMapping("/exercises")
    public List<Map<String, Object>> getAllExercises() {
        return exerciseDao.getAllExercises();
    }

    @PostMapping("/exercises")
    public Map<String, Object> addCustomExercise(@RequestBody Map<String, Object> body) {
        String name = requiredString(body, "name").trim();
        String type = requiredString(body, "type").toLowerCase();
        double metValue = requiredNumber(body, "metValue").doubleValue();
        int createdByUid = requiredNumber(body, "createdByUid").intValue();

        if (name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("Exercise name must contain 1 to 100 characters.");
        }
        if (!"strength".equals(type) && !"cardio".equals(type)) {
            throw new IllegalArgumentException("Exercise type must be 'strength' or 'cardio'.");
        }
        if (!Double.isFinite(metValue) || metValue <= 0 || metValue > 99.99) {
            throw new IllegalArgumentException("MET value must be greater than 0 and no more than 99.99.");
        }
        requireExistingUser(createdByUid);

        int id = exerciseDao.addCustomExercise(name, type, metValue, createdByUid);
        return Map.of("exerciseId", id, "status", "saved");
    }

    @GetMapping("/exercises/{userId}/summary")
    public List<Map<String, Object>> getExerciseSummary(@PathVariable int userId) {
        requireExistingUser(userId);
        return exerciseDao.getAllExercisesWithMaxWeight(userId);
    }

    // ---------------------------------------------------------------------
    // Strength workouts
    // ---------------------------------------------------------------------

    @PostMapping("/workouts")
    public Map<String, Object> logWorkout(@RequestBody Workout workout) {
        if (workout == null) {
            throw new IllegalArgumentException("Workout data is required.");
        }
        if (workout.getUserId() <= 0) {
            throw new IllegalArgumentException("A valid userId is required.");
        }
        requireExistingUser(workout.getUserId());
        if (!exerciseDao.isExerciseType(workout.getExerciseId(), "strength")) {
            throw new IllegalArgumentException("Select an exercise whose type is strength.");
        }
        if (!Double.isFinite(workout.getWeightKg()) || workout.getWeightKg() < 0
                || workout.getWeightKg() > 999.99 || workout.getReps() <= 0) {
            throw new IllegalArgumentException("Weight must be between 0 and 999.99 kg and reps must be positive.");
        }

        int logId = workoutDao.saveWorkout(workout);
        Map<String, Object> response = new HashMap<>();
        response.put("logId", logId);
        response.put("status", "saved");
        response.put("estimated1RM", FitnessMath.calculate1RM(workout.getWeightKg(), workout.getReps()));
        return response;
    }

    @GetMapping("/workouts/{userId}")
    public List<Workout> getWorkouts(@PathVariable int userId) {
        requireExistingUser(userId);
        return workoutDao.getWorkoutsByUserId(userId);
    }

    @GetMapping("/workouts/{userId}/max")
    public List<Map<String, Object>> getMaxWeights(@PathVariable int userId) {
        requireExistingUser(userId);
        return workoutDao.getMaxWeightPerExercise(userId);
    }

    // ---------------------------------------------------------------------
    // Cardio
    // ---------------------------------------------------------------------

    @PostMapping("/cardio")
    public Map<String, Object> logCardio(@RequestBody CardioLog log) {
        if (log == null) {
            throw new IllegalArgumentException("Cardio session data is required.");
        }
        if (log.getUserId() <= 0) {
            throw new IllegalArgumentException("A valid userId is required.");
        }
        requireExistingUser(log.getUserId());
        if (!exerciseDao.isExerciseType(log.getExerciseId(), "cardio")) {
            throw new IllegalArgumentException("Select an exercise whose type is cardio.");
        }
        if (!Double.isFinite(log.getInclinePct()) || log.getInclinePct() < 0 || log.getInclinePct() > 99.99
                || log.getDurationM() <= 0 || !Double.isFinite(log.getCaloriesOut())
                || log.getCaloriesOut() < 0 || log.getCaloriesOut() > 9999.99) {
            throw new IllegalArgumentException("Provide a valid incline, positive duration, and calories between 0 and 9999.99.");
        }

        int id = cardioDao.saveCardioLog(log);
        Map<String, Object> response = new HashMap<>();
        response.put("logId", id);
        response.put("caloriesOut", log.getCaloriesOut());
        response.put("status", "saved");
        return response;
    }

    @GetMapping("/cardio/{userId}")
    public List<CardioLog> getCardio(@PathVariable int userId) {
        requireExistingUser(userId);
        return cardioDao.getCardioLogsByUserId(userId);
    }

    // ---------------------------------------------------------------------
    // Diet and SQL insights
    // ---------------------------------------------------------------------

    @PostMapping("/diet")
    public Map<String, Object> logDiet(@RequestBody DietLog log) {
        if (log == null) {
            throw new IllegalArgumentException("Meal data is required.");
        }
        if (log.getUserId() <= 0) {
            throw new IllegalArgumentException("A valid userId is required.");
        }
        requireExistingUser(log.getUserId());
        if (log.getFoodItem() == null || log.getFoodItem().isBlank() || log.getFoodItem().length() > 100) {
            throw new IllegalArgumentException("Food item must contain 1 to 100 characters.");
        }
        if (log.getCalories() < 0 || !Double.isFinite(log.getProteinG())
                || log.getProteinG() < 0 || log.getProteinG() > 999.99) {
            throw new IllegalArgumentException("Calories and protein must be zero or greater.");
        }

        int id = dietDao.saveDietLog(log);
        return Map.of("logId", id, "status", "saved");
    }

    @GetMapping("/diet/{userId}")
    public List<DietLog> getDiet(@PathVariable int userId) {
        requireExistingUser(userId);
        return dietDao.getDietLogsByUserId(userId);
    }

    @GetMapping("/diet/{userId}/above-average")
    public List<DietLog> getAboveAverageMeals(@PathVariable int userId) {
        requireExistingUser(userId);
        return dietDao.getAboveAverageCalorieMeals(userId);
    }

    @GetMapping("/diet/{userId}/high-calorie-days")
    public List<String> getHighCalorieDays(@PathVariable int userId,
                                            @RequestParam(defaultValue = "1800") int min) {
        requireExistingUser(userId);
        if (min < 0 || min > 100000) {
            throw new IllegalArgumentException("Calorie threshold must be between 0 and 100000.");
        }
        return dietDao.getHighCalorieDays(userId, min);
    }

    // ---------------------------------------------------------------------
    // Routines
    // ---------------------------------------------------------------------

    @PostMapping("/routines")
    public Map<String, Object> createRoutine(@RequestBody Routine routine) {
        if (routine == null) {
            throw new IllegalArgumentException("Routine data is required.");
        }
        if (routine.getUserId() <= 0) {
            throw new IllegalArgumentException("A valid userId is required.");
        }
        requireExistingUser(routine.getUserId());
        if (routine.getRoutineName() == null || routine.getRoutineName().isBlank()
                || routine.getRoutineName().length() > 100) {
            throw new IllegalArgumentException("Routine name must contain 1 to 100 characters.");
        }
        int id = routineDao.createRoutine(routine);
        return Map.of("routineId", id);
    }

    @GetMapping("/routines/{userId}")
    public List<Routine> getRoutines(@PathVariable int userId) {
        requireExistingUser(userId);
        return routineDao.getRoutinesByUserId(userId);
    }

    @GetMapping("/routines/{routineId}/exercises")
    public List<RoutineExercise> getRoutineExercises(@PathVariable int routineId) {
        return routineDao.getRoutineExercises(routineId);
    }

    @PostMapping("/routines/{routineId}/exercises")
    public Map<String, String> addExerciseToRoutine(@PathVariable int routineId,
                                                     @RequestBody Map<String, Object> body) {
        int exerciseId = requiredNumber(body, "exerciseId").intValue();
        int defaultSets = optionalNumber(body, "defaultSets", 3).intValue();
        int defaultReps = optionalNumber(body, "defaultReps", 10).intValue();
        double defaultIncl = optionalNumber(body, "defaultIncl", 0).doubleValue();
        int defaultDur = optionalNumber(body, "defaultDur", 0).intValue();

        if (defaultSets < 0 || defaultReps < 0 || !Double.isFinite(defaultIncl)
                || defaultIncl < 0 || defaultIncl > 99.99 || defaultDur < 0) {
            throw new IllegalArgumentException("Routine defaults cannot be negative and incline cannot exceed 99.99%.");
        }
        if (!exerciseDao.getAllExercises().stream()
                .anyMatch(exercise -> ((Number) exercise.get("exerciseId")).intValue() == exerciseId)) {
            throw new IllegalArgumentException("The selected exercise does not exist.");
        }
        routineDao.addExerciseToRoutine(routineId, exerciseId, defaultSets, defaultReps, defaultIncl, defaultDur);
        return Map.of("status", "saved");
    }

    @DeleteMapping("/routines/{routineId}")
    public Map<String, Object> deleteRoutine(@PathVariable int routineId) {
        boolean deleted = routineDao.deleteRoutine(routineId);
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Routine not found.");
        }
        return Map.of("deleted", true);
    }

    // ---------------------------------------------------------------------
    // SQL objects: view, stored procedures, stored function, and cursor
    // ---------------------------------------------------------------------

    @GetMapping("/sql-insights/{userId}")
    public Map<String, Object> getSqlInsights(@PathVariable int userId) {
        requireExistingUser(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("sqlBmr", userDao.getBmrFromSqlFunction(userId));
        response.put("weeklySummary", dietDao.getWeeklySummaryFromProcedure(userId));
        response.put("dailyNutrition", dietDao.getDailyNutritionFromView(userId));
        response.put("totalStrengthVolume", workoutDao.getTotalStrengthVolumeFromProcedure(userId));
        return response;
    }

    // ---------------------------------------------------------------------
    // Analytics
    // ---------------------------------------------------------------------

    @GetMapping("/analytics/{userId}")
    public Map<String, Object> getAnalytics(@PathVariable int userId,
                                             @RequestParam(defaultValue = "7") int days) {
        if (days != 1 && days != 7 && days != 30) {
            throw new IllegalArgumentException("Analytics days must be 1, 7, or 30.");
        }
        User user = requireExistingUser(userId);
        double bmr = FitnessMath.calculateBMR(
                user.getWeightKg(), user.getHeightCm(), user.getAge(), user.getGender());

        // The selected period is the last N calendar days, including today.
        LocalDate today = LocalDate.now();
        Timestamp start = Timestamp.valueOf(today.minusDays(days - 1L).atStartOfDay());
        Timestamp end = Timestamp.valueOf(today.plusDays(1).atStartOfDay());

        long totalIntake = dietDao.getTotalCaloriesInRange(userId, start, end);
        double totalCardioBurn = cardioDao.getTotalCardioCaloriesInRange(userId, start, end);
        double netCalories = totalIntake - totalCardioBurn;
        double deficit = dietDao.getCaloricDeficitInRange(userId, start, end, bmr, days, cardioDao);

        Map<String, Object> response = new HashMap<>();
        response.put("days", days);
        response.put("bmr", bmr);
        response.put("totalIntake", totalIntake);
        response.put("totalCardioBurn", totalCardioBurn);
        response.put("netCalories", netCalories);
        response.put("caloricDeficit", deficit);
        return response;
    }

    // ---------------------------------------------------------------------
    // Validation helpers
    // ---------------------------------------------------------------------

    private User requireExistingUser(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("A valid userId is required.");
        }
        User user = userDao.getUserById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found.");
        }
        return user;
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User profile data is required.");
        }
        if (!Double.isFinite(user.getWeightKg()) || user.getWeightKg() <= 0 || user.getWeightKg() > 999.99
                || !Double.isFinite(user.getHeightCm()) || user.getHeightCm() <= 0 || user.getHeightCm() > 999.99
                || user.getAge() <= 0 || user.getAge() > 120) {
            throw new IllegalArgumentException("Provide a valid weight, height, and age (1 to 120 years).");
        }
        if (!"M".equalsIgnoreCase(user.getGender()) && !"F".equalsIgnoreCase(user.getGender())) {
            throw new IllegalArgumentException("Gender must be 'M' or 'F'.");
        }
        user.setGender(user.getGender().toUpperCase(java.util.Locale.ROOT));
    }

    private Number requiredNumber(Map<String, Object> body, String key) {
        if (body == null) {
            throw new IllegalArgumentException("A JSON request body is required.");
        }
        Object value = body.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Field '" + key + "' must be a number.");
        }
        return number;
    }

    private Number optionalNumber(Map<String, Object> body, String key, Number fallback) {
        if (body == null) {
            throw new IllegalArgumentException("A JSON request body is required.");
        }
        Object value = body.get(key);
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Field '" + key + "' must be a number.");
        }
        return number;
    }

    private String requiredString(Map<String, Object> body, String key) {
        if (body == null) {
            throw new IllegalArgumentException("A JSON request body is required.");
        }
        Object value = body.get(key);
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException("Field '" + key + "' must be a string.");
        }
        return string;
    }
}
