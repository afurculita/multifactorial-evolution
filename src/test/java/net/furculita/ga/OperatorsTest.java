package net.furculita.ga;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorsTest {

    private static List<Double> randomGenes(int n, Random r) {
        List<Double> g = new ArrayList<>(n);
        for (int i = 0; i < n; i++) g.add(r.nextDouble());
        return g;
    }

    @Test
    void sbxKeepsChildrenInBoundsAndChangesThem() {
        Random r = new Random(1);
        List<Double> a = randomGenes(200, r), b = randomGenes(200, r);
        List<Double> c1 = new ArrayList<>(a), c2 = new ArrayList<>(b);
        Operators.sbx(c1, c2, MultiTaskingGA.SBX_ETA, r);
        for (int i = 0; i < 200; i++) {
            assertTrue(c1.get(i) >= 0.0 && c1.get(i) <= 1.0);
            assertTrue(c2.get(i) >= 0.0 && c2.get(i) <= 1.0);
        }
        assertNotEquals(a, c1);
        assertNotEquals(b, c2);
    }

    @Test
    void sbxOfIdenticalParentsIsIdentity() {
        Random r = new Random(2);
        List<Double> a = randomGenes(50, r);
        List<Double> c1 = new ArrayList<>(a), c2 = new ArrayList<>(a);
        Operators.sbx(c1, c2, MultiTaskingGA.SBX_ETA, r);
        assertEquals(a, c1);
        assertEquals(a, c2);
    }

    @Test
    void polynomialMutationKeepsGenesInBounds() {
        Random r = new Random(3);
        List<Double> g = randomGenes(500, r);
        g.set(0, 0.0);
        g.set(1, 1.0);
        List<Double> before = new ArrayList<>(g);
        Operators.polynomialMutation(g, 1.0, MultiTaskingGA.PM_ETA, r);
        for (double v : g) assertTrue(v >= 0.0 && v <= 1.0);
        assertNotEquals(before, g);
    }

    @Test
    void zeroMutationRateChangesNothing() {
        Random r = new Random(4);
        List<Double> g = randomGenes(50, r);
        List<Double> before = new ArrayList<>(g);
        Operators.polynomialMutation(g, 0.0, MultiTaskingGA.PM_ETA, r);
        assertEquals(before, g);
    }
}
