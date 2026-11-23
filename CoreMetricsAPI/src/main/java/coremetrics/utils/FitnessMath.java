package coremetrics.utils;

public final class FitnessMath {

    private FitnessMath() {
    }

    /** Mifflin-St Jeor basal metabolic rate in kcal/day. */
    public static double calculateBMR(double weightKg, double heightCm, int age, String gender) {
        if (weightKg <= 0 || heightCm <= 0 || age <= 0 || age > 120) {
            throw new IllegalArgumentException("Weight, height, and age must be positive and age cannot exceed 120.");
        }
        if (!"M".equalsIgnoreCase(gender) && !"F".equalsIgnoreCase(gender)) {
            throw new IllegalArgumentException("Gender must be 'M' or 'F'.");
        }

        double base = (10 * weightKg) + (6.25 * heightCm) - (5 * age);
        return "M".equalsIgnoreCase(gender) ? base + 5 : base - 161;
    }

    /** Epley estimated one-repetition maximum in kg. */
    public static double calculate1RM(double weightKg, int reps) {
        if (weightKg < 0 || reps <= 0) {
            throw new IllegalArgumentException("Weight must be zero or greater and reps must be positive.");
        }
        if (reps == 1) {
            return weightKg;
        }
        return weightKg * (1 + (reps / 30.0));
    }

    /** MET-based active calorie estimate: MET × kg × hours. */
    public static double calculateMETCalories(double metValue, double weightKg, int durationMinutes) {
        if (metValue <= 0 || weightKg <= 0 || durationMinutes <= 0) {
            throw new IllegalArgumentException("MET, body weight, and duration must be positive.");
        }
        return metValue * weightKg * (durationMinutes / 60.0);
    }
}
