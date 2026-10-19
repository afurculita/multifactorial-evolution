package net.furculita.tasks;

import net.furculita.util.TourFiles;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TSPTest {

    /** Random-key encoding of a tour: the city visited at position p gets key p / n. */
    private static List<Double> encode(List<Integer> tour) {
        List<Double> genes = new ArrayList<>(tour.size());
        for (int i = 0; i < tour.size(); i++) genes.add(0.0);
        for (int pos = 0; pos < tour.size(); pos++) {
            genes.set(tour.get(pos), pos / (double) tour.size());
        }
        return genes;
    }

    @Test
    void fitnessIsClosedTourLength() throws Exception {
        TSP tsp = new TSP("data/tsp/berlin52.tsp");
        List<Integer> optimal = TourFiles.read("data/tsp/berlin52.opt.tour");
        assertEquals(52, tsp.getLenGen());
        assertEquals(7542.0, tsp.computeFitness(encode(optimal)));
    }

    @Test
    void everyChromosomeIsFeasible() {
        TSP tsp = new TSP("data/tsp/burma14.tsp");
        List<Double> genes = new ArrayList<>();
        Random r = new Random(7);
        for (int i = 0; i < 14; i++) genes.add(r.nextDouble());
        assertFalse(tsp.checkIndividualVail(genes));
    }

    @Test
    void longerChromosomeIsFoldedDeterministically() {
        TSP tsp = new TSP("data/tsp/burma14.tsp");
        List<Double> genes = new ArrayList<>();
        Random r = new Random(11);
        for (int i = 0; i < 50; i++) genes.add(r.nextDouble());
        Double first = tsp.computeFitness(genes);
        assertEquals(first, tsp.computeFitness(genes));
    }

    @Test
    void unreadableInstanceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TSP("data/tsp/bays29.tsp"));
        assertThrows(IllegalArgumentException.class, () -> new TSP("data/tsp/missing.tsp"));
    }
}
