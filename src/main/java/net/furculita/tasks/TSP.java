package net.furculita.tasks;

import net.furculita.ga.Task;
import net.furculita.util.TSPException;
import net.furculita.util.TSPFileParser;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Symmetric travelling salesman problem read from a TSPLIB file.
 *
 * <p>Decoding uses the random-key rule on the first {@code dimension} genes: cities are
 * visited in ascending order of their gene value, so any chromosome is a valid permutation
 * and the task has no constraints to repair. The fitness is the length of the closed tour.
 *
 * <p>{@link #localSearch(List)} runs a bounded number of first-improvement 2-opt sweeps on
 * the decoded tour and writes the improved tour back into the genes (Lamarckian learning).
 */
public class TSP extends Task {

    /** Default number of 2-opt sweeps applied to each offspring with this skill factor. */
    public static final int DEFAULT_LOCAL_SEARCH_SWEEPS = 1;

    private final int[][] graph;
    private final int localSearchSweeps;

    /**
     * @param fileName path to a TSPLIB {@code .tsp} file with {@code EUC_2D} or {@code GEO}
     *                 edge weights
     * @throws IllegalArgumentException if the file cannot be read or uses another format
     */
    public TSP(String fileName) {
        this(fileName, DEFAULT_LOCAL_SEARCH_SWEEPS);
    }

    /**
     * @param fileName          path to a TSPLIB {@code .tsp} file
     * @param localSearchSweeps number of 2-opt sweeps per offspring; {@code 0} disables local search
     * @throws IllegalArgumentException if the file cannot be read or uses another format
     */
    public TSP(String fileName, int localSearchSweeps) {
        super();
        if (localSearchSweeps < 0) throw new IllegalArgumentException("localSearchSweeps must be >= 0");
        try {
            this.graph = new TSPFileParser(fileName).getGraph();
        } catch (TSPException e) {
            throw new IllegalArgumentException("Cannot load TSP instance '" + fileName + "': " + e.getMessage(), e);
        }
        this.dimension = this.graph.length;
        this.localSearchSweeps = localSearchSweeps;
    }

    /**
     * Decodes the first {@code dimension} genes into a tour: cities sorted by ascending
     * gene value (ties broken by city index, so the result is always a permutation).
     */
    int[] decode(List<Double> genes) {
        Integer[] order = new Integer[dimension];
        for (int i = 0; i < dimension; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble(genes::get));
        int[] tour = new int[dimension];
        for (int i = 0; i < dimension; i++) tour[i] = order[i];
        return tour;
    }

    /** Writes {@code tour} into the first {@code dimension} genes so that {@link #decode} returns it. */
    private void encode(int[] tour, List<Double> genes) {
        for (int pos = 0; pos < tour.length; pos++) genes.set(tour[pos], pos / (double) tour.length);
    }

    /** @return the length of the closed tour */
    long tourLength(int[] tour) {
        long length = 0;
        for (int i = 0; i < tour.length; i++) {
            length += graph[tour[i]][tour[(i + 1) % tour.length]];
        }
        return length;
    }

    /** @return the length of the closed tour encoded by {@code genes} */
    @Override
    public double computeFitness(List<Double> genes) {
        return tourLength(decode(genes));
    }

    /**
     * First-improvement 2-opt: for every pair of non-adjacent edges, reverse the segment
     * between them if that shortens the tour. Runs at most {@code localSearchSweeps} full
     * sweeps and stops early at a local optimum.
     */
    @Override
    public void localSearch(List<Double> genes) {
        if (localSearchSweeps == 0 || dimension < 4) return;
        int[] tour = decode(genes);
        boolean improved = false;
        for (int sweep = 0; sweep < localSearchSweeps; sweep++) {
            boolean improvedThisSweep = false;
            for (int i = 0; i < dimension - 2; i++) {
                int a = tour[i], b = tour[i + 1];
                for (int j = i + 2; j < dimension - (i == 0 ? 1 : 0); j++) {
                    int c = tour[j], d = tour[(j + 1) % dimension];
                    int delta = graph[a][c] + graph[b][d] - graph[a][b] - graph[c][d];
                    if (delta < 0) {
                        reverse(tour, i + 1, j);
                        b = tour[i + 1];
                        improvedThisSweep = true;
                    }
                }
            }
            if (!improvedThisSweep) break;
            improved = true;
        }
        if (improved) encode(tour, genes);
    }

    private static void reverse(int[] tour, int from, int to) {
        while (from < to) {
            int t = tour[from];
            tour[from] = tour[to];
            tour[to] = t;
            from++;
            to--;
        }
    }

    /** No-op: every chromosome decodes to a valid permutation. */
    @Override
    public void repair(List<Double> genes) {
    }

    /** @return always {@code false}: every chromosome decodes to a valid permutation */
    @Override
    public boolean isInfeasible(List<Double> genes) {
        return false;
    }

    @Override
    public int getLenGen() {
        return dimension;
    }
}
