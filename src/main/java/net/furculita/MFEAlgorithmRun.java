package net.furculita;

import net.furculita.ga.Individual;
import net.furculita.ga.MultiTaskingGA;
import net.furculita.ga.Task;
import net.furculita.tasks.Knapsack;
import net.furculita.tasks.TSP;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Command-line entry point.
 *
 * <pre>
 * java -jar target/multifactorial-evolution-1.0-SNAPSHOT.jar [options] [instance ...]
 *
 *   instance            a TSPLIB .tsp file (TSP task) or a .kp file (knapsack task);
 *                       default: data/knapsack/100/s000..s004.kp and data/tsp/kroA100.tsp
 *   --generations N     number of generations (default 1000)
 *   --population N      population size (default 50)
 *   --pairs N           parent pairs per generation, i.e. 2N offspring (default 50)
 *   --rmp P             random mating probability (default 0.3)
 *   --mutation P        per-gene mutation probability (default 1 / chromosome length)
 *   --restart N         generations without improvement before a restart; 0 = never (default 100)
 *   --local-search N    2-opt sweeps per TSP offspring; 0 = off (default 1)
 *   --seed S            random seed (default 42)
 *   --print-every N     progress line every N generations (default 50)
 * </pre>
 * Run from the repository root so that the relative {@code data/} paths resolve.
 */
public class MFEAlgorithmRun {

    public static void main(String[] args) {
        int generations = 1000, population = 50, pairs = 50, restart = 100, localSearch = TSP.DEFAULT_LOCAL_SEARCH_SWEEPS, printEvery = 50;
        double rmp = MultiTaskingGA.DEFAULT_RMP, mutation = -1;
        long seed = 42;
        List<String> instances = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (!a.startsWith("--")) {
                instances.add(a);
                continue;
            }
            if (a.equals("--help")) {
                usage();
                return;
            }
            if (i + 1 >= args.length) throw usage("missing value for " + a);
            String v = args[++i];
            switch (a) {
                case "--generations" -> generations = Integer.parseInt(v);
                case "--population" -> population = Integer.parseInt(v);
                case "--pairs" -> pairs = Integer.parseInt(v);
                case "--rmp" -> rmp = Double.parseDouble(v);
                case "--mutation" -> mutation = Double.parseDouble(v);
                case "--restart" -> restart = Integer.parseInt(v);
                case "--local-search" -> localSearch = Integer.parseInt(v);
                case "--seed" -> seed = Long.parseLong(v);
                case "--print-every" -> printEvery = Integer.parseInt(v);
                default -> throw usage("unknown option " + a);
            }
        }
        if (instances.isEmpty()) {
            for (int k = 0; k < 5; k++) instances.add(String.format("data/knapsack/100/s%03d.kp", k));
            instances.add("data/tsp/kroA100.tsp");
        }

        List<Task> tasks = new ArrayList<>();
        for (String path : instances) tasks.add(load(path, localSearch));

        for (int t = 0; t < tasks.size(); t++) {
            System.out.printf("task %d: %s (%d genes)%n", t, instances.get(t), tasks.get(t).getLenGen());
        }

        final int every = Math.max(1, printEvery);
        final int total = generations;
        MultiTaskingGA ga = new MultiTaskingGA(tasks, population, rmp, restart, seed)
                .setProgressListener((g, best) -> {
                    if (g % every == 0 || g == total) System.out.println("generation " + g + ": best " + format(best));
                });
        if (mutation >= 0) ga.setMutationRate(mutation);

        long start = System.nanoTime();
        List<Individual> best = ga.run(generations, pairs);
        System.out.printf(Locale.ROOT, "finished in %.1f s%n", (System.nanoTime() - start) / 1e9);

        for (int t = 0; t < best.size(); t++) {
            Individual ind = best.get(t);
            System.out.printf(Locale.ROOT, "task %d (%s): best cost %.0f%n", t, instances.get(t), ind.getCost(t));
            System.out.println("  genes " + ind.getGenes());
        }
    }

    private static Task load(String path, int localSearch) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".tsp")) return new TSP(path, localSearch);
        if (lower.endsWith(".kp")) return new Knapsack(path);
        throw usage("cannot tell the task type of '" + path + "' (expected .tsp or .kp)");
    }

    private static String format(double[] costs) {
        StringBuilder sb = new StringBuilder("[");
        for (int t = 0; t < costs.length; t++) {
            if (t > 0) sb.append(", ");
            sb.append(String.format(Locale.ROOT, "%.0f", costs[t]));
        }
        return sb.append(']').toString();
    }

    private static IllegalArgumentException usage(String problem) {
        System.err.println("error: " + problem);
        usage();
        return new IllegalArgumentException(problem);
    }

    private static void usage() {
        System.err.println("usage: java -jar multifactorial-evolution.jar [--generations N] [--population N] [--pairs N]"
                + " [--rmp P] [--mutation P] [--restart N] [--local-search N] [--seed S] [--print-every N] [instance ...]");
    }
}
