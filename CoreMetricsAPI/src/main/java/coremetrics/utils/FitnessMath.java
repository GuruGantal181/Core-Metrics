package coremetrics.utils;

import coremetrics.models.User;
import coremetrics.models.Workout;

import java.time.Duration;

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

    // ---- Method overloading: convenience variants that delegate to the primitive versions ----

    /** Overload: BMR straight from a {@link User} profile. */
    public static double calculateBMR(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User profile is required.");
        }
        return calculateBMR(user.getWeightKg(), user.getHeightCm(), user.getAge(), user.getGender());
    }

    /** Overload: Epley 1RM straight from a {@link Workout}. */
    public static double calculate1RM(Workout workout) {
        if (workout == null) {
            throw new IllegalArgumentException("Workout is required.");
        }
        return calculate1RM(workout.getWeightKg(), workout.getReps());
    }

    /** Overload: MET calories using a {@link Duration} instead of whole minutes. */
    public static double calculateMETCalories(double metValue, double weightKg, Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("MET, body weight, and duration must be positive.");
        }
        if (metValue <= 0 || weightKg <= 0) {
            throw new IllegalArgumentException("MET, body weight, and duration must be positive.");
        }
        return metValue * weightKg * (duration.toMillis() / 3_600_000.0);
    }
}
