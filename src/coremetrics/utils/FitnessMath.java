package coremetrics.utils;

public class FitnessMath {

    // Mifflin-St Jeor Equation for Basal Metabolic Rate (BMR)
    public static double calculateBMR(double weightKg, double heightCm, int age, String gender) {
        double bmr = (10 * weightKg) + (6.25 * heightCm) - (5 * age);
        return "M".equalsIgnoreCase(gender) ? (bmr + 5) : (bmr - 161);
    }

    // Epley Formula for 1-Rep Max (1RM)
    public static double calculate1RM(double weightKg, int reps) {
        if (reps <= 1) {
            return weightKg;
        }
        return weightKg * (1 + (reps / 30.0));
    }

    // Active Calorie Burn using MET value from exercise_dict
    public static double calculateMETCalories(double metValue, double weightKg, int durationMinutes) {
        return metValue * weightKg * (durationMinutes / 60.0);
    }
}