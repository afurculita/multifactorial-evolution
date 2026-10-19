package net.furculita.ga;

import java.util.List;
import java.util.Random;

/**
 * Real-coded variation operators for random-key chromosomes bounded to {@code [0, 1]}:
 * simulated binary crossover (SBX) and polynomial mutation (PM), as used by Gupta, Ong and
 * Feng (2016) in the unified continuous search space. Both follow Deb and Agrawal's
 * bounded formulation.
 */
final class Operators {
    private static final double EPS = 1e-14;

    private Operators() {
    }

    /**
     * Simulated binary crossover of two parents into two children (in place on the child
     * lists, which must be copies of the parents).
     *
     * @param c1  first child, initially a copy of the first parent
     * @param c2  second child, initially a copy of the second parent
     * @param eta distribution index; small values (2) spread children far from the parents
     * @param r   random source
     */
    static void sbx(List<Double> c1, List<Double> c2, double eta, Random r) {
        for (int i = 0; i < c1.size(); i++) {
            if (r.nextDouble() > 0.5) continue;
            double y1 = c1.get(i), y2 = c2.get(i);
            if (Math.abs(y1 - y2) <= EPS) continue;
            if (y1 > y2) {
                double t = y1;
                y1 = y2;
                y2 = t;
            }
            double u = r.nextDouble();
            double lo = boundedChild(y1, y2, u, eta, y1 - 0.0, true);
            double hi = boundedChild(y1, y2, u, eta, 1.0 - y2, false);
            lo = clamp(lo);
            hi = clamp(hi);
            if (r.nextBoolean()) {
                c1.set(i, lo);
                c2.set(i, hi);
            } else {
                c1.set(i, hi);
                c2.set(i, lo);
            }
        }
    }

    private static double boundedChild(double y1, double y2, double u, double eta, double distToBound, boolean lower) {
        double beta = 1.0 + 2.0 * distToBound / (y2 - y1);
        double alpha = 2.0 - Math.pow(beta, -(eta + 1.0));
        double betaq = u <= 1.0 / alpha
                ? Math.pow(u * alpha, 1.0 / (eta + 1.0))
                : Math.pow(1.0 / (2.0 - u * alpha), 1.0 / (eta + 1.0));
        return lower
                ? 0.5 * ((y1 + y2) - betaq * (y2 - y1))
                : 0.5 * ((y1 + y2) + betaq * (y2 - y1));
    }

    /**
     * Polynomial mutation in place.
     *
     * @param genes       chromosome to mutate
     * @param probability per-gene mutation probability (typically {@code 1 / length})
     * @param eta         distribution index; small values (5) allow larger perturbations
     * @param r           random source
     */
    static void polynomialMutation(List<Double> genes, double probability, double eta, Random r) {
        double mutPow = 1.0 / (eta + 1.0);
        for (int i = 0; i < genes.size(); i++) {
            if (r.nextDouble() > probability) continue;
            double y = genes.get(i);
            double delta1 = y;        // (y - lower) / (upper - lower) with bounds [0, 1]
            double delta2 = 1.0 - y;  // (upper - y) / (upper - lower)
            double u = r.nextDouble();
            double deltaq;
            if (u < 0.5) {
                double xy = 1.0 - delta1;
                double val = 2.0 * u + (1.0 - 2.0 * u) * Math.pow(xy, eta + 1.0);
                deltaq = Math.pow(val, mutPow) - 1.0;
            } else {
                double xy = 1.0 - delta2;
                double val = 2.0 * (1.0 - u) + 2.0 * (u - 0.5) * Math.pow(xy, eta + 1.0);
                deltaq = 1.0 - Math.pow(val, mutPow);
            }
            genes.set(i, clamp(y + deltaq));
        }
    }

    static double clamp(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }
}
