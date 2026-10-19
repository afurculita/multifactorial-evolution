package net.furculita.ga;

import java.util.List;

/**
 * One optimisation task taking part in a multitasking run.
 *
 * <p>Every task is a <em>minimisation</em> problem evaluated on the shared random-key
 * chromosome used by {@link MultiTaskingGA}: a list of doubles in {@code [0, 1]} whose
 * length is the largest {@link #getLenGen()} among all tasks in the run. Following Gupta,
 * Ong and Feng (2016), a task of dimension {@code D} reads the first {@code D} genes of the
 * unified chromosome and ignores the rest.
 *
 * <p>Besides fitness, a task may define feasibility ({@link #isInfeasible(List)} and
 * {@link #repair(List)}) and an optional Lamarckian local search
 * ({@link #localSearch(List)}) applied to offspring whose skill factor is this task.
 */
public abstract class Task {

    /** Number of decision variables of the task (cities for a TSP, items for a knapsack). */
    protected int dimension;

    /** Task-specific capacity. Used by the knapsack; unused by the TSP. */
    protected int capacity;

    /**
     * Checks a chromosome against this task's constraints.
     *
     * @param genes the unified chromosome (at least {@link #getLenGen()} genes)
     * @return {@code true} if the chromosome violates a constraint and needs {@link #repair}
     */
    public abstract boolean isInfeasible(List<Double> genes);

    /**
     * Repairs a chromosome in place so that it satisfies this task's constraints.
     * Must be a no-op for a chromosome that is already feasible.
     *
     * @param genes the unified chromosome to repair
     */
    public abstract void repair(List<Double> genes);

    /**
     * Evaluates a chromosome. Lower is better: TSP returns the tour length, knapsack the
     * negated total value.
     *
     * @param genes the unified chromosome
     * @return the objective value to minimise
     */
    public abstract double computeFitness(List<Double> genes);

    /**
     * Optional local search that improves the chromosome in place (Lamarckian learning).
     * The default does nothing. Implementations must leave the chromosome feasible.
     *
     * @param genes the unified chromosome to improve
     */
    public void localSearch(List<Double> genes) {
    }

    /**
     * @return the number of genes this task reads, i.e. its {@link #dimension}
     */
    public abstract int getLenGen();
}
