package net.furculita.tasks;

import net.furculita.ga.Task;
import net.furculita.util.TSPException;
import net.furculita.util.TSPFileParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TSP extends Task {
    private int[][] graph;

    public TSP(String fileName) {
        super();

        TSPFileParser parser;

        try {
            parser = new TSPFileParser(fileName);
            this.graph = parser.getGraph();
            this.dimension = this.graph.length;
        } catch (TSPException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private List<Integer> decode(List<Double> tx) {
        List<Double> x = new ArrayList<>();
        List<Double> tmp_tx = new ArrayList<>(tx);

        if (tmp_tx.size() > dimension) {
            double window[] = {Math.random(), Math.random()};
            int stride = getStride(tx, tmp_tx, window);
            for (int i = 0; i < tmp_tx.size(); i += stride) {
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

    @Override
    public Double computeFitness(List<Double> ind) {
        List<Integer> xx = decode(ind);
        double c = 0;
        for (int i = 0; i < xx.size() - 1; i++) {
            c += this.graph[xx.get(i)][xx.get(i + 1)];
        }

        return c;
    }

    @Override
    public void makeIndividualVail(List<Double> ind) {
    }

    @Override
    public boolean checkIndividualVail(List<Double> ind) {
        return false;
    }

    @Override
    public int getLenGen() {
        return dimension;
    }
}
