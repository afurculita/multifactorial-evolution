package net.furculita.ga;

import net.furculita.tasks.Knapsack;
import net.furculita.tasks.TSP;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiTaskingGATest {

    private static List<Task> knapsacks() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Knapsack("data/knapsack/50/s000.kp"));
        tasks.add(new Knapsack("data/knapsack/50/s001.kp"));
        return tasks;
    }

    @Test
    void returnsOneFeasibleBestIndividualPerTask() {
        List<Task> tasks = knapsacks();
        List<Individual> best = new MultiTaskingGA(tasks, 20, 0.3, 20, 1L).run(100, 10);

        assertEquals(tasks.size(), best.size());
        for (int t = 0; t < tasks.size(); t++) {
            Individual ind = best.get(t);
            assertTrue(ind.isEvaluatedOn(t));
            assertFalse(tasks.get(t).isInfeasible(ind.getGenes()), "best of task " + t + " is feasible");
            assertEquals(ind.getCost(t), tasks.get(t).computeFitness(ind.getGenes()), "recorded cost matches the genes");
            assertTrue(ind.getCost(t) < 0, "best of task " + t + " selects at least one item");
        }
    }

    @Test
    void bestKnownCostNeverGetsWorseAcrossGenerationsAndRestarts() {
        List<double[]> history = new ArrayList<>();
        List<Individual> best = new MultiTaskingGA(knapsacks(), 20, 0.3, 5, 2L)
                .setProgressListener((g, costs) -> history.add(costs.clone()))
                .run(200, 10);

        assertEquals(200, history.size());
        for (int g = 1; g < history.size(); g++) {
            for (int t = 0; t < 2; t++) {
                assertTrue(history.get(g)[t] <= history.get(g - 1)[t], "elite of task " + t + " lost at generation " + (g + 1));
            }
        }
        for (int t = 0; t < 2; t++) assertEquals(history.get(199)[t], best.get(t).getCost(t));
    }

    @Test
    void sameSeedGivesSameResult() {
        List<Individual> first = new MultiTaskingGA(knapsacks(), 16, 0.3, 10, 7L).run(50, 8);
        List<Individual> second = new MultiTaskingGA(knapsacks(), 16, 0.3, 10, 7L).run(50, 8);
        for (int t = 0; t < 2; t++) {
            assertEquals(first.get(t).getCost(t), second.get(t).getCost(t));
            assertEquals(first.get(t).getGenes(), second.get(t).getGenes());
        }
    }

    @Test
    void tasksOfDifferentDimensionShareOnePopulation() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(new TSP("data/tsp/burma14.tsp"));          // 14 genes
        tasks.add(new Knapsack("data/knapsack/50/s000.kp")); // 50 genes

        List<Individual> best = new MultiTaskingGA(tasks, 20, 0.3, 0, 3L).run(100, 10);

        assertEquals(2, best.size());
        assertEquals(50, best.get(0).getGenes().size(), "chromosome length is the largest task dimension");
        assertTrue(best.get(0).getCost(0) > 0, "TSP tour has positive length");
        assertEquals(best.get(0).getCost(0), tasks.get(0).computeFitness(best.get(0).getGenes()));
        assertFalse(tasks.get(1).isInfeasible(best.get(1).getGenes()));
    }

    @Test
    void localSearchFindsTheBurma14OptimumQuickly() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(new TSP("data/tsp/burma14.tsp", 10));
        List<Individual> best = new MultiTaskingGA(tasks, 20, 0.3, 0, 5L).run(50, 10);
        assertEquals(3323.0, best.get(0).getCost(0), "published optimum of burma14");
    }

    @Test
    void rejectsInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> new MultiTaskingGA(new ArrayList<>(), 10, 0.3, 10, 1L));
        assertThrows(IllegalArgumentException.class, () -> new MultiTaskingGA(knapsacks(), 1, 0.3, 10, 1L));
        assertThrows(IllegalArgumentException.class, () -> new MultiTaskingGA(knapsacks(), 10, 1.5, 10, 1L));
        assertThrows(IllegalArgumentException.class, () -> new MultiTaskingGA(knapsacks(), 10, 0.3, 10, 1L).setMutationRate(2));
    }
}
