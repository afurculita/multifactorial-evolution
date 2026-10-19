package net.furculita.tasks;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnapsackTest {

    /** Optimum of data/knapsack/100/s000.kp, computed by dynamic programming. */
    private static final double S000_OPTIMUM = 32066;

    @Test
    void emptySelectionIsFeasibleAndWorthless() {
        Knapsack kp = new Knapsack("data/knapsack/100/s000.kp");
        assertEquals(100, kp.getLenGen());
        List<Double> none = new ArrayList<>(Collections.nCopies(100, 0.0));
        assertFalse(kp.checkIndividualVail(none));
        assertEquals(0.0, kp.computeFitness(none));
    }

    @Test
    void overweightSelectionIsDetectedAndRepaired() {
        Knapsack kp = new Knapsack("data/knapsack/100/s000.kp");
        List<Double> all = new ArrayList<>(Collections.nCopies(100, 0.9));
        assertTrue(kp.checkIndividualVail(all), "selecting every item exceeds the capacity");

        kp.makeIndividualVail(all);
        assertFalse(kp.checkIndividualVail(all), "repair must produce a feasible selection");
        double fitness = kp.computeFitness(all);
        assertTrue(fitness < 0, "repair keeps some items");
        assertTrue(fitness >= -S000_OPTIMUM, "no feasible selection beats the optimum");
    }

    @Test
    void unreadableInstanceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Knapsack("data/knapsack/missing.kp"));
    }
}
