import coremetrics.dao.CardioLogDAO;
import coremetrics.dao.DietLogDAO;
import coremetrics.dao.ExerciseDAO;
import coremetrics.dao.RoutineDAO;
import coremetrics.dao.UserDAO;
import coremetrics.dao.WorkoutDAO;
import coremetrics.models.CardioLog;
import coremetrics.models.DietLog;
import coremetrics.models.Routine;
import coremetrics.models.User;
import coremetrics.models.Workout;
import coremetrics.utils.FitnessMath;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        UserDAO userDb = new UserDAO();
        WorkoutDAO workoutDb = new WorkoutDAO();
        CardioLogDAO cardioDb = new CardioLogDAO();
        DietLogDAO dietDb = new DietLogDAO();
        ExerciseDAO exerciseDb = new ExerciseDAO();
        RoutineDAO routineDb = new RoutineDAO();

        System.out.println("=== COREMETRICS APP TERMINAL ===\n");
        System.out.println("1. Create New Account");
        System.out.println("2. Login to Existing Account");
        System.out.print("Choose an option (1 or 2): ");
        int choice = scanner.nextInt();

        int currentUserId = -1;
        double bmr = 1700.0; // Fallback if logging in directly

        if (choice == 1) {
            System.out.println("\n--- NEW USER REGISTRATION ---");
            System.out.print("Enter weight (kg): ");
            double w = scanner.nextDouble();
            System.out.print("Enter height (cm): ");
            double h = scanner.nextDouble();
            System.out.print("Enter age: ");
            int a = scanner.nextInt();
            System.out.print("Enter gender (M/F): ");
            String g = scanner.next().toUpperCase();

            currentUserId = userDb.saveUser(new User(0, w, h, a, g));
            System.out.println("[SUCCESS] Account created! Your new User ID is: " + currentUserId);

            bmr = FitnessMath.calculateBMR(w, h, a, g);
            System.out.println("-> Your Baseline BMR: " + bmr + " kcal/day");
        } else {
            System.out.print("\nEnter your existing User ID: ");
            currentUserId = scanner.nextInt();
            bmr = cardioDb.getUserWeightKg(currentUserId) * 22.0; 
            System.out.println("[SUCCESS] Logged in as User " + currentUserId);
        }

        System.out.println("\n--- LOG A STRENGTH WORKOUT ---");
        System.out.println("Available exercises:");
        exerciseDb.printAllExercises();
        System.out.println("0 = Add a new custom exercise");
        System.out.print("Choose Exercise ID: ");
        int exerciseId = scanner.nextInt();

        if (exerciseId == 0) {
            scanner.nextLine();
            System.out.print("New exercise name: ");
            String customName = scanner.nextLine();
            System.out.print("Type (strength / cardio): ");
            String customType = scanner.next().toLowerCase();
            System.out.print("MET value (e.g., 5.0 for lifting, 8.0 for vigorous): ");
            double metVal = scanner.nextDouble();

            exerciseId = exerciseDb.addCustomExercise(customName, customType, metVal, currentUserId);
            System.out.println("[SUCCESS] Added '" + customName + "' with ID: " + exerciseId);
        }

        System.out.print("Weight lifted (kg): ");
        double weightKg = scanner.nextDouble();
        System.out.print("Reps: ");
        int reps = scanner.nextInt();

        workoutDb.saveWorkout(new Workout(0, currentUserId, exerciseId, weightKg, reps));
        double est1RM = FitnessMath.calculate1RM(weightKg, reps);
        System.out.printf("[SUCCESS] Workout saved. Estimated 1RM (Epley): %.2f kg%n", est1RM);

        System.out.println("\n--- LOG A CARDIO SESSION ---");
        System.out.print("Cardio Exercise ID: ");
        int cardioExId = scanner.nextInt();
        System.out.print("Incline (%): ");
        double inclinePct = scanner.nextDouble();
        System.out.print("Duration (minutes): ");
        int durationM = scanner.nextInt();

        int cardioId = cardioDb.saveCardioLog(new CardioLog(0, currentUserId, cardioExId, inclinePct, durationM, 0.0));
        System.out.println("[SUCCESS] Cardio log saved (ID: " + cardioId + ").");

        System.out.println("\n--- LOG A MEAL ---");
        scanner.nextLine();
        System.out.print("Food item: ");
        String food = scanner.nextLine();
        System.out.print("Calories: ");
        int calories = scanner.nextInt();
        System.out.print("Protein (g): ");
        double proteinG = scanner.nextDouble();

        dietDb.saveDietLog(new DietLog(0, currentUserId, food, calories, proteinG));
        System.out.println("[SUCCESS] Meal saved.");

        System.out.println("\n--- CREATE ROUTINE & MAP EXERCISE (M:N) ---");
        scanner.nextLine();
        System.out.print("Routine name: ");
        String routineName = scanner.nextLine();
        int routineId = routineDb.createRoutine(new Routine(0, currentUserId, routineName));

        System.out.print("Exercise ID to map into routine: ");
        int mapExId = scanner.nextInt();
        System.out.print("Default sets (0 if cardio): ");
        int defSets = scanner.nextInt();
        System.out.print("Default reps (0 if cardio): ");
        int defReps = scanner.nextInt();
        System.out.print("Default incline % (0 if strength): ");
        double defIncl = scanner.nextDouble();
        System.out.print("Default duration mins (0 if strength): ");
        int defDur = scanner.nextInt();

        routineDb.addExerciseToRoutine(routineId, mapExId, defSets, defReps, defIncl, defDur);
        System.out.println("[SUCCESS] Routine #" + routineId + " mapped exercises:");
        for (String item : routineDb.getRoutineExercises(routineId)) {
            System.out.println("  - " + item);
        }

        System.out.println("\n--- YOUR DATABASE HISTORY ---");
        List<Workout> myWorkouts = workoutDb.getWorkoutsByUserId(currentUserId);
        for (Workout w : myWorkouts) {
            System.out.println("[" + w.getLoggedAt() + "] WORKOUT (Ex ID " + w.getExerciseId() + "): " + w.getWeightKg() + "kg x " + w.getReps() + " reps");
        }

        List<CardioLog> myCardio = cardioDb.getCardioLogsByUserId(currentUserId);
        for (CardioLog c : myCardio) {
            System.out.println("[" + c.getLoggedAt() + "] CARDIO (Ex ID " + c.getExerciseId() + "): "
                    + c.getDurationM() + " min @ " + c.getInclinePct() + "% incline | "
                    + c.getCaloriesOut() + " kcal burned");
        }

        List<DietLog> myMeals = dietDb.getDietLogsByUserId(currentUserId);
        for (DietLog m : myMeals) {
            System.out.println("[" + m.getConsumedAt() + "] MEAL: " + m.getFoodItem()
                    + " | " + m.getCalories() + " kcal | " + m.getProteinG() + "g protein");
        }

        System.out.println("\n--- MAX WEIGHT PER EXERCISE (INNER JOIN + GROUP BY) ---");
        Map<String, Double> maxLifts = workoutDb.getMaxWeightPerExercise(currentUserId);
        for (Map.Entry<String, Double> entry : maxLifts.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue() + " kg");
        }

        System.out.println("\n--- ALL EXERCISES + USER MAX WEIGHT (LEFT JOIN) ---");
        for (String row : exerciseDb.getAllExercisesWithMaxWeight(currentUserId)) {
            System.out.println("  " + row);
        }

        int days = 7;
        Timestamp endExclusive = Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS));
        Timestamp startInclusive = Timestamp.from(Instant.now().minus(days, ChronoUnit.DAYS));

        int totalIntake = dietDb.getTotalCaloriesInRange(currentUserId, startInclusive, endExclusive);
        double totalCardioBurn = cardioDb.getTotalCardioCaloriesInRange(currentUserId, startInclusive, endExclusive);
        int netCalories = dietDb.getNetCaloriesInRange(currentUserId, startInclusive, endExclusive, cardioDb);
        double deficit = dietDb.getCaloricDeficitInRange(currentUserId, startInclusive, endExclusive, bmr, days, cardioDb);

        System.out.println("\n--- LAST " + days + " DAYS CALORIE & DEFICIT SUMMARY ---");
        System.out.println("  Total Food Intake: " + totalIntake + " kcal");
        System.out.printf("  Total Cardio Burn: %.2f kcal%n", totalCardioBurn);
        System.out.println("  Net Calories (Intake - Cardio): " + netCalories + " kcal");
        System.out.printf("  Estimated Deficit / (Surplus): %.2f kcal%n", deficit);

        scanner.close();
    }
}