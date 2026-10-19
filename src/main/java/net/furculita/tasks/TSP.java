package net.furculita.tasks;

import net.furculita.ga.Task;
import net.furculita.util.TSPException;
import net.furculita.util.TSPFileParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Symmetric travelling salesman problem read from a TSPLIB file.
 *
 * <p>Decoding uses the random-key rule: cities are visited in ascending order of their gene
 * value, so any chromosome is a valid permutation and the task has no constraints to
 * repair. The fitness is the length of the closed tour.
 */
public class TSP extends Task {
    private int[][] graph;

    /** Window weights used to fold a chromosome longer than {@link #dimension}. */
    private final double[] window = {Math.random(), Math.random()};

    /**
     * @param fileName path to a TSPLIB {@code .tsp} file with {@code EUC_2D} or {@code GEO}
     *                 edge weights
     * @throws IllegalArgumentException if the file cannot be read or uses another format
     */
    public TSP(String fileName) {
        super();

        try {
            TSPFileParser parser = new TSPFileParser(fileName);
            this.graph = parser.getGraph();
            this.dimension = this.graph.length;
        } catch (TSPException e) {
            throw new IllegalArgumentException("Cannot load TSP instance '" + fileName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Decodes a chromosome into a tour: city {@code i} is visited at the position of its gene
     * value in the ascending sort of all gene values.
     */
    private List<Integer> decode(List<Double> tx) {
        List<Double> x = new ArrayList<>();
        List<Double> tmp_tx = new ArrayList<>(tx);

        if (tmp_tx.size() > dimension) {
            int stride = getStride(tx, tmp_tx, window);
            for (int k = 0; k < dimension; k++) {
                int i = k * stride;
                double tmp_x = 0;
                for (int j = 0; j < window.length; j++) {
                    tmp_x += tmp_tx.get(i + j) * window[j];
                }
                x.add(tmp_x);
            }
        } else {
            x = tmp_tx;
        }

        List<Integer> lA = new ArrayList<>();
        Double ts[] = new Double[dimension];
        for (int i = 0; i < dimension; i++) {
            ts[i] = x.get(i);
        }

        Arrays.sort(ts);
        for (int i = 0; i < dimension; i++) {
            lA.add(x.indexOf(ts[i]));
        }

        return lA;
    }

    /** @return the length of the closed tour encoded by {@code ind} */
    @Override
    public Double computeFitness(List<Double> ind) {
        List<Integer> xx = decode(ind);
        double c = 0;
        for (int i = 0; i < xx.size() - 1; i++) {
            c += this.graph[xx.get(i)][xx.get(i + 1)];
        }
        c += this.graph[xx.get(xx.size() - 1)][xx.get(0)];

        return c;
    }

    /** No-op: every chromosome decodes to a valid permutation. */
    @Override
    public void makeIndividualVail(List<Double> ind) {
    }

    /** @return always {@code false}: every chromosome decodes to a valid permutation */
    @Override
    public boolean checkIndividualVail(List<Double> ind) {
        return false;
    }

    @Override
    public int getLenGen() {
        return dimension;
    }
}
