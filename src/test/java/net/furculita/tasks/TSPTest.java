package net.furculita.tasks;

import net.furculita.util.TourFiles;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TSPTest {

    /** Random-key encoding of a tour: the city visited at position p gets key p / n. */
    private static List<Double> encode(List<Integer> tour, int length) {
        List<Double> genes = new ArrayList<>(length);
        for (int i = 0; i < length; i++) genes.add(0.0);
        for (int pos = 0; pos < tour.size(); pos++) {
            genes.set(tour.get(pos), pos / (double) tour.size());
        }
        return genes;
    }

    private static List<Double> randomGenes(int n, long seed) {
        List<Double> genes = new ArrayList<>(n);
        Random r = new Random(seed);
        for (int i = 0; i < n; i++) genes.add(r.nextDouble());
        return genes;
    }

    @Test
    void fitnessIsClosedTourLength() throws Exception {
        TSP tsp = new TSP("data/tsp/berlin52.tsp");
        List<Integer> optimal = TourFiles.read("data/tsp/berlin52.opt.tour");
        assertEquals(52, tsp.getLenGen());
        assertEquals(7542.0, tsp.computeFitness(encode(optimal, 52)));
    }

    @Test
    void decodeIsAlwaysAPermutation() {
        TSP tsp = new TSP("data/tsp/burma14.tsp");
        List<Double> genes = randomGenes(14, 7);
        genes.set(3, genes.get(9)); // duplicate keys must not produce a repeated city
        int[] tour = tsp.decode(genes);
        boolean[] seen = new boolean[14];
        for (int city : tour) {
            assertFalse(seen[city], "city visited twice");
            seen[city] = true;
        }
        assertFalse(tsp.isInfeasible(genes));
    }

    @Test
    void longerChromosomeUsesOnlyItsFirstGenes() {
        TSP tsp = new TSP("data/tsp/burma14.tsp");
        List<Double> genes = randomGenes(50, 11);
        assertEquals(tsp.computeFitness(genes.subList(0, 14)), tsp.computeFitness(genes));
        assertEquals(tsp.computeFitness(genes), tsp.computeFitness(genes));
    }

    @Test
    void localSearchNeverWorsensAndIsWrittenBackToGenes() {
        TSP tsp = new TSP("data/tsp/berlin52.tsp", 5);
        for (long seed = 0; seed < 10; seed++) {
            List<Double> genes = randomGenes(60, seed);
            double before = tsp.computeFitness(genes);
            tsp.localSearch(genes);
            double after = tsp.computeFitness(genes);
            assertTrue(after <= before, "2-opt must not lengthen the tour");
            assertTrue(after < before, "a random berlin52 tour is never 2-optimal");
            for (double g : genes) assertTrue(g >= 0.0 && g <= 1.0);
        }
    }

    @Test
    void localSearchCanBeDisabled() {
        TSP tsp = new TSP("data/tsp/berlin52.tsp", 0);
        List<Double> genes = randomGenes(52, 3);
        List<Double> copy = new ArrayList<>(genes);
        tsp.localSearch(genes);
        assertEquals(copy, genes);
        assertArrayEquals(tsp.decode(copy), tsp.decode(genes));
    }

    @Test
    void unreadableInstanceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TSP("data/tsp/bays29.tsp"));
        assertThrows(IllegalArgumentException.class, () -> new TSP("data/tsp/missing.tsp"));
        assertThrows(IllegalArgumentException.class, () -> new TSP("data/tsp/burma14.tsp", -1));
    }
}
