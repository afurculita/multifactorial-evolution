package net.furculita.ga;

import net.furculita.tasks.Knapsack;
import net.furculita.tasks.TSP;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiTaskingGATest {
    private PrintStream stdout;

    @BeforeEach
    void silenceProgressOutput() {
        stdout = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(stdout);
    }

    @Test
    void returnsOneFeasibleBestIndividualPerTask() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Knapsack("data/knapsack/50/s000.kp"));
        tasks.add(new Knapsack("data/knapsack/50/s001.kp"));

        List<Individual> best = new MultiTaskingGA(tasks, 20, 0.1, 50).run(10);

        assertEquals(tasks.size(), best.size());
        for (int t = 0; t < tasks.size(); t++) {
            Individual ind = best.get(t);
            assertFalse(tasks.get(t).checkIndividualVail(ind.getGen()), "best of task " + t + " is feasible");
            assertTrue(ind.getFitnessTask().get(t) < 0, "best of task " + t + " selects at least one item");
        }
    }

    @Test
    void tasksOfDifferentDimensionShareOnePopulation() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(new TSP("data/tsp/burma14.tsp"));          // 14 genes
        tasks.add(new Knapsack("data/knapsack/50/s000.kp")); // 50 genes

        List<Individual> best = new MultiTaskingGA(tasks, 20, 0.1, 50).run(10);

        assertEquals(2, best.size());
        assertEquals(50, best.get(0).getGen().size(), "chromosome length is the largest task dimension");
        assertTrue(best.get(0).getFitnessTask().get(0) > 0, "TSP tour has positive length");
        assertFalse(tasks.get(1).checkIndividualVail(best.get(1).getGen()));
    }
}
