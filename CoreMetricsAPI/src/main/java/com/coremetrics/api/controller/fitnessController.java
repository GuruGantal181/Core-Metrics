package com.coremetrics.api.controller;

import coremetrics.dao.*;
import coremetrics.models.*;
import coremetrics.utils.FitnessMath;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class fitnessController {

    private final UserDAO userDao = new UserDAO();
    private final WorkoutDAO workoutDao = new WorkoutDAO();
    private final CardioLogDAO cardioDao = new CardioLogDAO();
    private final DietLogDAO dietDao = new DietLogDAO();
    private final ExerciseDAO exerciseDao = new ExerciseDAO();
    private final RoutineDAO routineDao = new RoutineDAO();

    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody User user) {
        int id = userDao.saveUser(user);
        double bmr = FitnessMath.calculateBMR(user.getWeightKg(), user.getHeightCm(), user.getAge(), user.getGender());
        Map<String, Object> res = new HashMap<>();
        res.put("userId", id);
        res.put("bmr", bmr);
        return res;
    }

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable int id) {
        return userDao.getUserById(id);
    }

    @PostMapping("/workouts")
    public Map<String, Object> logWorkout(@RequestBody Workout workout) {
        workoutDao.saveWorkout(workout);
        double est1RM = FitnessMath.calculate1RM(workout.getWeightKg(), workout.getReps());
        Map<String, Object> res = new HashMap<>();
        res.put("status", "saved");
        res.put("estimated1RM", est1RM);
        return res;
    }

    @GetMapping("/workouts/{userId}")
    public List<Workout> getWorkouts(@PathVariable int userId) {
        return workoutDao.getWorkoutsByUserId(userId);
    }

    @GetMapping("/workouts/{userId}/max")
    public Map<String, Double> getMaxWeights(@PathVariable int userId) {
        return workoutDao.getMaxWeightPerExercise(userId);
    }

    @PostMapping("/cardio")
    public Map<String, Object> logCardio(@RequestBody CardioLog log) {
        int id = cardioDao.saveCardioLog(log);
        Map<String, Object> res = new HashMap<>();
        res.put("logId", id);
        res.put("status", "saved");
        return res;
    }

    @GetMapping("/cardio/{userId}")
    public List<CardioLog> getCardio(@PathVariable int userId) {
        return cardioDao.getCardioLogsByUserId(userId);
    }

    @PostMapping("/diet")
    public Map<String, String> logDiet(@RequestBody DietLog log) {
        dietDao.saveDietLog(log);
        return Map.of("status", "saved");
    }

    @GetMapping("/diet/{userId}")
    public List<DietLog> getDiet(@PathVariable int userId) {
        return dietDao.getDietLogsByUserId(userId);
    }

    @GetMapping("/exercises/{userId}/summary")
    public List<String> getExerciseSummary(@PathVariable int userId) {
        return exerciseDao.getAllExercisesWithMaxWeight(userId);
    }

    @PostMapping("/routines")
    public Map<String, Object> createRoutine(@RequestBody Routine routine) {
        int id = routineDao.createRoutine(routine);
        return Map.of("routineId", id);
    }

    @GetMapping("/routines/{userId}")
    public List<Routine> getRoutines(@PathVariable int userId) {
        return routineDao.getRoutinesByUserId(userId);
    }

    @GetMapping("/routines/{routineId}/exercises")
    public List<String> getRoutineExercises(@PathVariable int routineId) {
        return routineDao.getRoutineExercises(routineId);
    }

    @GetMapping("/analytics/{userId}")
    public Map<String, Object> getAnalytics(@PathVariable int userId, @RequestParam(defaultValue = "7") int days) {
        User user = userDao.getUserById(userId);
        double bmr = (user != null)
                ? FitnessMath.calculateBMR(user.getWeightKg(), user.getHeightCm(), user.getAge(), user.getGender())
                : 1700.0;

        Timestamp end = Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS));
        Timestamp start = Timestamp.from(Instant.now().minus(days, ChronoUnit.DAYS));

        int totalIntake = dietDao.getTotalCaloriesInRange(userId, start, end);
        double totalCardioBurn = cardioDao.getTotalCardioCaloriesInRange(userId, start, end);
        int netCalories = dietDao.getNetCaloriesInRange(userId, start, end, cardioDao);
        double deficit = dietDao.getCaloricDeficitInRange(userId, start, end, bmr, days, cardioDao);

        Map<String, Object> res = new HashMap<>();
        res.put("days", days);
        res.put("bmr", bmr);
        res.put("totalIntake", totalIntake);
        res.put("totalCardioBurn", totalCardioBurn);
        res.put("netCalories", netCalories);
        res.put("caloricDeficit", deficit);
        return res;
    }
}