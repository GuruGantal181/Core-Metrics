package coremetrics.utils;

public class FitnessMath {
    
    // Mifflin-St Jeor Equation for Basal Metabolic Rate (BMR)
    public static double calculateBMR(double weightKg, double heightCm, int age, boolean isMale) {
        double bmr = (10 * weightKg) + (6.25 * heightCm) - (5 * age);
        return isMale ? (bmr + 5) : (bmr - 161);
    }

    // Epley Formula for 1-Rep Max (1RM)
    public static double calculate1RM(double weightLifted, int reps) {
        if (reps <= 1) {
            return weightLifted;
        }
        return weightLifted * (1 + (reps / 30.0));
    }
}