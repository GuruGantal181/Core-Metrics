package com.coremetrics.api.Model;

public class WorkOut {
    private  String exerciseName;
    private  double weightkg;
    private  int reps;

    public WorkOut(String exerciseName, double weightKg, int reps) {
        this.exerciseName = exerciseName;
        this.weightkg = weightKg;
        this.reps = reps;
    }
    public String getExerciseName() {
        return exerciseName;
    }
    public double getWeightKg(){
        return  weightkg;
    }
    public int getReps() {
        return reps;
    }
}

