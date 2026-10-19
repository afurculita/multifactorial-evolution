package net.furculita.ga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A member of the multitasking population.
 *
 * <p>Following Gupta, Ong and Feng (2016) an individual carries, besides its random-key
 * chromosome, the multifactorial bookkeeping the algorithm needs:
 * <ul>
 *   <li><b>factorial cost</b>: its objective value on every task it has been evaluated on;
 *       the others hold {@link #NOT_EVALUATED} (selective evaluation);</li>
 *   <li><b>factorial rank</b>: its 1-based position in the population sorted by factorial
 *       cost on each task; individuals not evaluated on a task are ranked last there;</li>
 *   <li><b>skill factor</b>: the index of the task on which it ranks best;</li>
 *   <li><b>scalar fitness</b>: {@code 1 / min(factorial rank)}, the single value used for
 *       survivor selection across tasks.</li>
 * </ul>
 * Genes and costs are exposed read-only; {@link #copy()} produces an independent snapshot
 * (used for the per-task elites that survive population restarts).
 */
public final class Individual {

    /** Factorial cost of a task the individual has not been evaluated on. */
    public static final double NOT_EVALUATED = Double.POSITIVE_INFINITY;

    private final List<Double> genes;
    private final double[] costs;
    private final int[] ranks;
    private int skillFactor = -1;
    private double scalarFitness;

    /**
     * @param genes  the random-key chromosome (owned by the individual afterwards)
     * @param nTasks the number of tasks in the run
     */
    Individual(List<Double> genes, int nTasks) {
        this.genes = genes;
        this.costs = new double[nTasks];
        Arrays.fill(costs, NOT_EVALUATED);
        this.ranks = new int[nTasks];
    }

    /** @return a deep, independent copy */
    public Individual copy() {
        Individual c = new Individual(new ArrayList<>(genes), costs.length);
        System.arraycopy(costs, 0, c.costs, 0, costs.length);
        System.arraycopy(ranks, 0, c.ranks, 0, ranks.length);
        c.skillFactor = skillFactor;
        c.scalarFitness = scalarFitness;
        return c;
    }

    /** @return the random-key chromosome, read-only */
    public List<Double> getGenes() {
        return Collections.unmodifiableList(genes);
    }

    /** Mutable access for the variation operators and repair. */
    List<Double> genes() {
        return genes;
    }

    /** @return the factorial cost on {@code task}, or {@link #NOT_EVALUATED} */
    public double getCost(int task) {
        return costs[task];
    }

    /** @return a copy of the factorial cost vector */
    public double[] getCosts() {
        return costs.clone();
    }

    void setCost(int task, double cost) {
        costs[task] = cost;
    }

    /** @return {@code true} if the individual has a factorial cost on {@code task} */
    public boolean isEvaluatedOn(int task) {
        return costs[task] != NOT_EVALUATED;
    }

    /** @return the 1-based factorial rank on {@code task} as of the last ranking */
    public int getRank(int task) {
        return ranks[task];
    }

    void setRank(int task, int rank) {
        ranks[task] = rank;
    }

    /** @return the index of the task on which this individual ranks best */
    public int getSkillFactor() {
        return skillFactor;
    }

    void setSkillFactor(int skillFactor) {
        this.skillFactor = skillFactor;
    }

    /** @return {@code 1 / min(factorial rank)}; higher is better */
    public double getScalarFitness() {
        return scalarFitness;
    }

    void setScalarFitness(double scalarFitness) {
        this.scalarFitness = scalarFitness;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Individual[skillFactor=").append(skillFactor).append(", costs=[");
        for (int t = 0; t < costs.length; t++) {
            if (t > 0) sb.append(", ");
            sb.append(costs[t] == NOT_EVALUATED ? "n/a" : String.valueOf(costs[t]));
        }
        return sb.append("], ranks=").append(Arrays.toString(ranks))
                .append(", scalarFitness=").append(scalarFitness)
                .append(", genes=").append(genes).append(']').toString();
    }
}
