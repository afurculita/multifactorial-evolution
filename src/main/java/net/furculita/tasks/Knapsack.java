package net.furculita.tasks;

import net.furculita.ga.Task;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 0/1 knapsack problem.
 *
 * <p>Instance file format (whitespace separated): the number of items, the capacity, then one
 * {@code weight value} pair per item. See {@code data/knapsack}.
 *
 * <p>Decoding reads the first {@code dimension} genes: a gene above {@code 0.5} selects the
 * item. The fitness is the <em>negated</em> total value so that the task is minimised like
 * every other {@link Task}. An over-weight selection is repaired by dropping selected items
 * in ascending value/weight order until the capacity is respected.
 */
public class Knapsack extends Task {

    /** A gene strictly above this threshold selects the item. */
    static final double THRESHOLD = 0.5;

    private final int[] weights;
    private final int[] values;
    /** Item indices in ascending value/weight order: the order in which repair drops items. */
    private final Integer[] dropOrder;

    /**
     * @param fileName path to an instance file in the format described in the class comment
     * @throws IllegalArgumentException if the file cannot be read
     */
    public Knapsack(String fileName) {
        super();
        try (java.util.Scanner in = new java.util.Scanner(new File(fileName))) {
            dimension = in.nextInt();
            capacity = in.nextInt();
            weights = new int[dimension];
            values = new int[dimension];
            for (int i = 0; i < dimension; i++) {
                weights[i] = in.nextInt();
                values[i] = in.nextInt();
            }
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Cannot load knapsack instance '" + fileName + "'", e);
        } catch (java.util.NoSuchElementException e) {
            throw new IllegalArgumentException("Malformed knapsack instance '" + fileName + "'", e);
        }
        dropOrder = new Integer[dimension];
        for (int i = 0; i < dimension; i++) dropOrder[i] = i;
        Arrays.sort(dropOrder, Comparator.comparingDouble(i -> values[i] / (double) weights[i]));
    }

    static boolean selected(double gene) {
        return gene > THRESHOLD;
    }

    private long weightOf(List<Double> genes) {
        long w = 0;
        for (int i = 0; i < dimension; i++) if (selected(genes.get(i))) w += weights[i];
        return w;
    }

    /** @return the negated total value of the selected items */
    @Override
    public double computeFitness(List<Double> genes) {
        long value = 0;
        for (int i = 0; i < dimension; i++) if (selected(genes.get(i))) value += values[i];
        return -value;
    }

    /** @return {@code true} if the selected items exceed the capacity */
    @Override
    public boolean isInfeasible(List<Double> genes) {
        return weightOf(genes) > capacity;
    }

    /**
     * Drops selected items in ascending value/weight order, by lowering the corresponding
     * genes below the selection threshold, until the total weight fits the capacity.
     */
    @Override
    public void repair(List<Double> genes) {
        long weight = weightOf(genes);
        for (int k = 0; k < dimension && weight > capacity; k++) {
            int item = dropOrder[k];
            if (selected(genes.get(item))) {
                weight -= weights[item];
                genes.set(item, genes.get(item) - THRESHOLD);
            }
        }
    }

    @Override
    public int getLenGen() {
        return dimension;
    }
}
