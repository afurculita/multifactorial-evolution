package net.furculita.tasks;

import net.furculita.ga.Task;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Knapsack extends Task {
    private int weights[], prices[];
    private double weightedPrices[];
    private int priceWeightLineNumbers[];
    private double window[] = {Math.random(), Math.random(), Math.random(), Math.random()};
    private int stride;

    public Knapsack(String fileName) {
        super();

        parseFile(fileName);
    }

    private double windowMax() {
        double max = -1;
        for (double aWindow : window) {
            if (aWindow > max) {
                max = aWindow;
            }
        }
        return max;
    }

    private void sort(double a[], int na, int vt[]) {
        for (int i = 0; i < na - 1; i++)
            for (int j = i + 1; j < na; j++)
                if (a[i] > a[j]) {
                    double tmp = a[i];
                    a[i] = a[j];
                    a[j] = tmp;
                    int tmp2 = vt[i];
                    vt[i] = vt[j];
                    vt[j] = tmp2;
                }
    }

    private void parseFile(String fileName) {
        try {
            Scanner fileIn = new Scanner(new File(fileName));
            dimension = fileIn.nextInt();
            capacity = fileIn.nextInt();
            weightedPrices = new double[dimension];
            weights = new int[dimension];
            prices = new int[dimension];

            priceWeightLineNumbers = new int[dimension];
            for (int i = 0; i < dimension; i++)
                priceWeightLineNumbers[i] = i;

            for (int i = 0; i < dimension; i++) {
                weights[i] = fileIn.nextInt();
                prices[i] = fileIn.nextInt();
                weightedPrices[i] = prices[i] * 1.0 / weights[i];
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        sort(weightedPrices, dimension, priceWeightLineNumbers);
    }

    @Override
    public Double computeFitness(List<Double> ind) {
        List<Integer> x = decode(ind);
        double res = 0;
        for (int i = 0; i < x.size(); i++)
            res -= prices[i] * x.get(i);

        return res;
    }

    @Override
    public boolean checkIndividualVail(List<Double> ind) {
        List<Integer> x = decode(ind);
        double res = 0;
        for (int i = 0; i < x.size(); i++)
            res += weights[i] * x.get(i);

        return !(res <= capacity);
    }

    private Double getWeight(List<Double> ind) {
        List<Integer> x = decode(ind);
        double res = 0;
        for (int i = 0; i < x.size(); i++)
            res += weights[i] * x.get(i);
        return res;
    }

    public void makeIndividualVail(List<Double> x) {
        List<Integer> x_decode = decode(x);
        Double wx = getWeight(x);
        int i = 0;
        if (x.size() > dimension) {
            while (wx > capacity) {
                if (x_decode.get(priceWeightLineNumbers[i]) == 1) {
                    wx = wx - weights[priceWeightLineNumbers[i]];
                    for (int k = 0; k < window.length; k++) {
                        x.set(priceWeightLineNumbers[i] * stride + k, 0.5 / (window.length * windowMax()));
                    }
                }
                i++;
            }
        } else {
            while (wx > capacity) {
                if (x_decode.get(priceWeightLineNumbers[i]) == 1) {
                    wx = wx - weights[priceWeightLineNumbers[i]];
                    x.set(priceWeightLineNumbers[i], x.get(priceWeightLineNumbers[i]) - 0.5);
                }
                i++;
            }
        }
    }

    private List<Integer> decode(List<Double> x) {
        List<Double> tmp_x = new ArrayList<>(x);

        List<Integer> kp = new ArrayList<>();
        if (tmp_x.size() > dimension) {
            stride = getStride(x, tmp_x, window);

            for (int i = 0; i < tmp_x.size(); i += stride) {
                double tmp = 0;
                for (int j = 0; j < window.length; j++) {
                    tmp += tmp_x.get(i + j) * window[j];
                }
                if (tmp > 1) tmp /= 10;
                kp.add((int) Math.round(tmp));
            }
        } else {
            for (int i = 0; i < dimension; i++)
                kp.add((int) Math.round(tmp_x.get(i)));
        }

        return kp;
    }

    @Override
    public int getLenGen() {
        return dimension;
    }
}
