package net.furculita;

import net.furculita.ga.MultiTaskingGA;
import net.furculita.ga.Task;
import net.furculita.tasks.Knapsack;
import net.furculita.tasks.TSP;

import java.util.ArrayList;

public class MFEAlgorithmRun {
    public static void main(String[] args) {
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.add(new Knapsack("data/knapsack/100/s000.kp"));
        tasks.add(new Knapsack("data/knapsack/100/s001.kp"));
        tasks.add(new Knapsack("data/knapsack/100/s002.kp"));
        tasks.add(new Knapsack("data/knapsack/100/s003.kp"));
        tasks.add(new Knapsack("data/knapsack/100/s004.kp"));
        tasks.add(new TSP("data/tsp/kroA100.tsp"));

        MultiTaskingGA g = new MultiTaskingGA(tasks, 50, 0.1, 50);
        g.run(50);
    }
}
