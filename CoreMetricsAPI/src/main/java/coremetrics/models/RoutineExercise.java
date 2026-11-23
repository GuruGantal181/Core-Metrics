package coremetrics.models;

public class RoutineExercise {
    private int exerciseId;
    private String name;
    private String type;
    private int defaultSets;
    private int defaultReps;
    private double defaultIncl;
    private int defaultDur;

    public RoutineExercise() {
    }

    public RoutineExercise(int exerciseId, String name, String type, int defaultSets,
                           int defaultReps, double defaultIncl, int defaultDur) {
        this.exerciseId = exerciseId;
        this.name = name;
        this.type = type;
        this.defaultSets = defaultSets;
        this.defaultReps = defaultReps;
        this.defaultIncl = defaultIncl;
        this.defaultDur = defaultDur;
    }

    public int getExerciseId() { return exerciseId; }
    public void setExerciseId(int exerciseId) { this.exerciseId = exerciseId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getDefaultSets() { return defaultSets; }
    public void setDefaultSets(int defaultSets) { this.defaultSets = defaultSets; }

    public int getDefaultReps() { return defaultReps; }
    public void setDefaultReps(int defaultReps) { this.defaultReps = defaultReps; }

    public double getDefaultIncl() { return defaultIncl; }
    public void setDefaultIncl(double defaultIncl) { this.defaultIncl = defaultIncl; }

    public int getDefaultDur() { return defaultDur; }
    public void setDefaultDur(int defaultDur) { this.defaultDur = defaultDur; }
}
