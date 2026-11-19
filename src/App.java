import coremetrics.models.User;
import coremetrics.models.Workout;
import coremetrics.models.DietLog;
import coremetrics.dao.UserDAO;
import coremetrics.dao.WorkoutDAO;
import coremetrics.dao.DietLogDAO;
import coremetrics.dao.ExerciseDAO;
import coremetrics.utils.FitnessMath;
import java.util.Scanner;
import java.util.List;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        UserDAO userDb = new UserDAO();
        WorkoutDAO workoutDb = new WorkoutDAO();
        DietLogDAO dietDb = new DietLogDAO();
        ExerciseDAO exerciseDb = new ExerciseDAO();

        System.out.println("=== COREMETRICS APP TERMINAL ===\n");
        System.out.println("1. Create New Account");
        System.out.println("2. Login to Existing Account");
        System.out.print("Choose an option (1 or 2): ");
        int choice = scanner.nextInt();

        int currentUserId = -1;

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

            double bmr = FitnessMath.calculateBMR(w, h, a, g);
            System.out.println("-> Your Baseline BMR: " + bmr + " kcal/day");

        } 
        else {
            System.out.print("\nEnter your existing User ID: ");
            currentUserId = scanner.nextInt();
            System.out.println("[SUCCESS] Logged in as User " + currentUserId);
        }

        System.out.println("\n--- LOG A WORKOUT ---");
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
        System.out.println("[SUCCESS] Workout saved.");

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

        System.out.println("\n--- YOUR DATABASE HISTORY ---");
        List<Workout> myWorkouts = workoutDb.getWorkoutsByUserId(currentUserId);
        for (Workout w : myWorkouts) {
            System.out.println("[" + w.getLoggedAt() + "] WORKOUT (Ex ID " + w.getExerciseId() + "): "
                    + w.getWeightKg() + "kg x " + w.getReps() + " reps");
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

        scanner.close();
    }
}