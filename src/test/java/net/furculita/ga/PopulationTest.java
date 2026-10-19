package net.furculita.ga;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PopulationTest {

    private static Individual individual(double... costs) {
        Individual ind = new Individual(new ArrayList<>(List.of(0.1, 0.2)), costs.length);
        for (int t = 0; t < costs.length; t++) ind.setCost(t, costs[t]);
        return ind;
    }

    @Test
    void ranksEvaluatedIndividualsPerTaskAndUnevaluatedLast() {
        double na = Individual.NOT_EVALUATED;
        Individual a = individual(10, na);   // best on task 0, not evaluated on task 1
        Individual b = individual(20, 5);    // second on task 0, best on task 1
        Individual c = individual(na, 7);    // only task 1
        Individual d = individual(na, 7);    // tie with c on task 1
        List<Individual> pool = List.of(a, b, c, d);

        Population.rank(pool);

        assertEquals(1, a.getRank(0));
        assertEquals(2, b.getRank(0));
        assertEquals(4, c.getRank(0), "unevaluated individuals rank last");
        assertEquals(1, b.getRank(1));
        assertEquals(4, a.getRank(1));

        assertEquals(0, a.getSkillFactor());
        assertEquals(1, b.getSkillFactor(), "task 1 rank (1) beats task 0 rank (2)");
        assertEquals(1, c.getSkillFactor());
        assertEquals(1.0, a.getScalarFitness());
        assertEquals(1.0, b.getScalarFitness());
        assertEquals(1.0 / c.getRank(1), c.getScalarFitness());
        assertEquals(1.0 / d.getRank(1), d.getScalarFitness());
    }

    @Test
    void copyIsIndependent() {
        Individual a = individual(10, 20);
        Population.rank(List.of(a));
        Individual c = a.copy();
        a.setCost(0, 99);
        a.setRank(0, 7);
        a.genes().set(0, 0.9);
        assertEquals(10, c.getCost(0));
        assertEquals(1, c.getRank(0));
        assertEquals(0.1, c.getGenes().get(0));
    }
}
