package net.furculita;

import net.furculita.ga.MultiTaskingGA;
import net.furculita.ga.Task;
import net.furculita.tasks.Knapsack;
import net.furculita.tasks.TSP;

import java.util.ArrayList;
import java.util.List;

/**
 * Example entry point: optimises five 100-item knapsack instances and the 100-city TSP
 * {@code kroA100} together in one multitasking population.
 *
 * <p>Run from the repository root so that the relative {@code data/} paths resolve:
 * {@code java -jar target/multifactorial-evolution-1.0-SNAPSHOT.jar} or
 * {@code mvn exec:java}.
 */
public class MFEAlgorithmRun {
    public static void main(String[] args) {
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Knapsack("data/knapsack/100/s000.kp"));
        tasks.add(new Knapsack("data/knapsack/100/s001.kp"));
        tasks.add(new Knapsack("data/knapsack/100/s002.kp"));
        tasks.add(new Knapsack("data/knapsack/100/s003.kp"));
        tasks.add(new Knapsack("data/knapsack/100/s004.kp"));
        tasks.add(new TSP("data/tsp/kroA100.tsp"));

        // population size, probability of mutation instead of cross-task crossover,
        // generations without improvement before the population is re-initialised
        MultiTaskingGA g = new MultiTaskingGA(tasks, 50, 0.1, 50);
        // 50 parent pairs per generation -> 100 offspring
        g.run(50);
    }
}
