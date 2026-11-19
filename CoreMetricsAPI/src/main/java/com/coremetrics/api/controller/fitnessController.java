package com.coremetrics.api.controller;

import com.coremetrics.api.Model.WorkOut;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class fitnessController {
    @GetMapping("/api/workouts")
    public List<WorkOut> getWorkoutsHistory(){
        WorkOut w1 = new WorkOut("Bench Press" , 100.0 , 5);
        WorkOut w2 = new WorkOut("Squat", 140.0, 3);
        WorkOut w3 = new WorkOut("Deadlift", 180.5, 1);

        return Arrays.asList(w1, w2, w3);
    }
}
