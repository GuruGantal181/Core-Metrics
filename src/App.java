import coremetrics.models.User;
import coremetrics.models.Workout;
import coremetrics.models.DietLog;
import coremetrics.dao.UserDAO;
import coremetrics.dao.WorkoutDAO;
import coremetrics.dao.DietLogDAO;
import coremetrics.utils.FitnessMath;
import java.util.Scanner;
import java.util.List;

public class App {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        UserDAO userDb = new UserDAO();
        WorkoutDAO workoutDb = new WorkoutDAO();
        DietLogDAO dietDb = new DietLogDAO();
        
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
            currentUserId = userDb.saveUser(new User(0, w, h, a));
            System.out.println("[SUCCESS] Account created! Your new User ID is: " + currentUserId);
            
            double bmr = FitnessMath.calculateBMR(w, h, a, true);
            System.out.println("-> Your Baseline BMR: " + bmr + " kcal/day");
            
        } else {
            System.out.print("\nEnter your existing User ID: ");
            currentUserId = scanner.nextInt();
            System.out.println("[SUCCESS] Logged in as User " + currentUserId);
        }

        System.out.println("\n--- LOG A WORKOUT ---");
        scanner.nextLine(); 
        System.out.print("Exercise name: ");
        String exercise = scanner.nextLine();
        System.out.print("Weight lifted (kg): ");
        double weightLifted = scanner.nextDouble();
        System.out.print("Reps: ");
        int reps = scanner.nextInt();
        System.out.print("Sets: ");
        int sets = scanner.nextInt();

        workoutDb.saveWorkout(new Workout(0, currentUserId, exercise, weightLifted, reps, sets));
        System.out.println("[SUCCESS] Workout saved.");

        System.out.println("\n--- LOG A MEAL ---");
        scanner.nextLine(); 
        System.out.print("Food item: ");
        String food = scanner.nextLine();
        System.out.print("Calories: ");
        int calories = scanner.nextInt();

        dietDb.saveDietLog(new DietLog(0, currentUserId, food, calories));
        System.out.println("[SUCCESS] Meal saved.");

        System.out.println("\n--- YOUR DATABASE HISTORY ---");
        List<Workout> myWorkouts = workoutDb.getWorkoutsByUserId(currentUserId);
        for (Workout w : myWorkouts) {
            System.out.println("[" + w.getLogDate() + "] WORKOUT: " + w.getExerciseName() + " | " + w.getWeightLifted() + "kg");
        }

        List<DietLog> myMeals = dietDb.getDietLogsByUserId(currentUserId);
        for (DietLog m : myMeals) {
            System.out.println("[" + m.getLogDate() + "] MEAL: " + m.getFoodItem() + " | " + m.getCalories() + " kcal");
        }

        scanner.close();
    }
}