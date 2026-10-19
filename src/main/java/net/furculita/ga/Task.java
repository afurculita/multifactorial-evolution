package net.furculita.ga;

import java.util.List;

/**
 * One optimisation task taking part in a multitasking run.
 *
 * <p>Every task is a <em>minimisation</em> problem evaluated on the shared random-key
 * chromosome used by {@link MultiTaskingGA}: a list of doubles in {@code [0, 1)} whose
 * length is the largest {@link #getLenGen()} among all tasks in the run. A task decodes
 * that unified chromosome into its own solution representation (a tour, an item
 * selection, ...) before evaluating it.
 *
 * <p>Besides fitness, a task may define feasibility: {@link #checkIndividualVail(List)}
 * reports whether a chromosome violates the task's constraints and
 * {@link #makeIndividualVail(List)} repairs it in place.
 */
public abstract class Task {

    /** Number of decision variables of the task (cities for a TSP, items for a knapsack). */
    protected int dimension;

    /** Task-specific capacity. Used by the knapsack; unused by the TSP. */
    protected int capacity;

    /**
     * Computes the stride with which a chromosome longer than {@link #dimension} is folded
     * into exactly {@code dimension} values. Each decoded value is the weighted sum of
     * {@code window.length} consecutive genes starting at {@code k * stride}, for
     * {@code k = 0 .. dimension - 1}. When the genes do not divide evenly, {@code tmp_tx}
     * is zero-padded in place so that the last window stays inside the list.
     *
     * @param tx     the original chromosome (only its size is used)
     * @param tmp_tx a mutable copy of the chromosome that may be padded with zeros
     * @param window the window weights
     * @return the stride between consecutive windows
     */
    protected int getStride(List<Double> tx, List<Double> tmp_tx, double[] window) {
        int stride;
        if ((tmp_tx.size() - window.length) % (dimension - 1) == 0) {
            stride = (tmp_tx.size() - window.length) / (dimension - 1);
        } else {
            stride = (tmp_tx.size() - window.length) / (dimension - 1) + 1;
            int zero_padding = (dimension - 1) * stride + window.length - tx.size();
            for (int i = 0; i < zero_padding; i++) {
                tmp_tx.add(0.0);
            }
        }
        return stride;
    }

    /**
     * Repairs a chromosome in place so that it satisfies this task's constraints.
     * Must be a no-op for a chromosome that is already feasible.
     *
     * @param ind the chromosome to repair
     */
    public abstract void makeIndividualVail(List<Double> ind);

    /**
     * Checks a chromosome against this task's constraints.
     *
     * <p>Note the inverted naming inherited from the original code: the method returns
     * {@code true} when the chromosome is <strong>infeasible</strong> and needs repair.
     *
     * @param ind the chromosome to check
     * @return {@code true} if the chromosome violates a constraint
     */
    public abstract boolean checkIndividualVail(List<Double> ind);

    /**
     * Evaluates a chromosome. Lower is better: TSP returns the tour length, knapsack the
     * negated total value.
     *
     * @param ind the chromosome to evaluate
     * @return the objective value to minimise
     */
    public abstract Double computeFitness(List<Double> ind);

    /**
     * @return the number of genes this task needs, i.e. its {@link #dimension}
     */
    public abstract int getLenGen();
}
