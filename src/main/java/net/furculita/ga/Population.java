package net.furculita.ga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * The shared population of a multitasking run and the factorial-rank bookkeeping of the
 * MFEA.
 *
 * <p>All individuals live in one unified search space: random-key chromosomes whose length
 * is the largest {@link Task#getLenGen()} among the tasks. Ranking follows Algorithm 1 of
 * Gupta, Ong and Feng (2016): per task, the individuals that were evaluated on it are sorted
 * by factorial cost; the others are ranked last. Scalar fitness is {@code 1 / min rank}.
 */
final class Population {
    private final List<Task> tasks;
    private final int size;
    private final int chromosomeLength;
    private final Random random;
    private List<Individual> individuals = new ArrayList<>();

    /**
     * @param tasks  the tasks being optimised; the chromosome length is their largest dimension
     * @param size   population size
     * @param random random source
     */
    Population(List<Task> tasks, int size, Random random) {
        this.tasks = tasks;
        this.size = size;
        this.random = random;
        int max = 0;
        for (Task task : tasks) max = Math.max(max, task.getLenGen());
        this.chromosomeLength = max;
    }

    int size() {
        return size;
    }

    int chromosomeLength() {
        return chromosomeLength;
    }

    List<Individual> individuals() {
        return individuals;
    }

    /**
     * (Re)creates the population with uniformly random chromosomes. Every individual is
     * repaired for every task, evaluated on every task and then ranked.
     */
    void init() {
        individuals = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            List<Double> genes = new ArrayList<>(chromosomeLength);
            for (int j = 0; j < chromosomeLength; j++) genes.add(random.nextDouble());
            repairForAllTasks(genes);
            Individual ind = new Individual(genes, tasks.size());
            for (int t = 0; t < tasks.size(); t++) ind.setCost(t, tasks.get(t).computeFitness(genes));
            individuals.add(ind);
        }
        rank(individuals);
    }

    /**
     * Replaces the first {@code elites.size()} individuals by copies of the given elites
     * (used after a restart so that the best known solution of every task survives).
     */
    void reinsert(List<Individual> elites) {
        for (int i = 0; i < elites.size() && i < individuals.size(); i++) {
            individuals.set(i, elites.get(i).copy());
        }
        rank(individuals);
    }

    /**
     * Repairs the chromosome in place, cycling over the tasks until a full round passes with
     * no task needing a repair (one task's repair can invalidate another task's constraint).
     */
    void repairForAllTasks(List<Double> genes) {
        int cleanInARow = 0;
        for (int i = 0; cleanInARow < tasks.size(); i = (i + 1) % tasks.size()) {
            Task t = tasks.get(i);
            if (t.isInfeasible(genes)) {
                t.repair(genes);
                cleanInARow = 0;
            } else {
                cleanInARow++;
            }
        }
    }

    /**
     * Assigns factorial ranks, skill factor and scalar fitness to every individual of the
     * pool. Individuals without a cost on a task are ranked last ({@code pool.size()}) on
     * that task so that an unevaluated task can never become the skill factor.
     */
    static void rank(List<Individual> pool) {
        if (pool.isEmpty()) return;
        int nTasks = pool.get(0).getCosts().length;
        Integer[] order = new Integer[pool.size()];
        for (int t = 0; t < nTasks; t++) {
            final int task = t;
            int evaluated = 0;
            for (int i = 0; i < pool.size(); i++) {
                if (pool.get(i).isEvaluatedOn(task)) order[evaluated++] = i;
            }
            Arrays.sort(order, 0, evaluated, Comparator.comparingDouble(i -> pool.get(i).getCost(task)));
            for (Individual ind : pool) ind.setRank(task, pool.size());
            for (int r = 0; r < evaluated; r++) pool.get(order[r]).setRank(task, r + 1);
        }
        for (Individual ind : pool) {
            int best = -1, bestRank = Integer.MAX_VALUE;
            for (int t = 0; t < nTasks; t++) {
                if (ind.getRank(t) < bestRank) {
                    bestRank = ind.getRank(t);
                    best = t;
                }
            }
            ind.setSkillFactor(best);
            ind.setScalarFitness(1.0 / bestRank);
        }
    }

    /**
     * Elitist survivor selection: ranks the union of the current population and the
     * offspring, then keeps the {@link #size()} individuals with the highest scalar fitness.
     * Ties are broken randomly so that offspring and parents compete fairly.
     */
    void selectFrom(List<Individual> offspring) {
        List<Individual> pool = new ArrayList<>(individuals.size() + offspring.size());
        pool.addAll(individuals);
        pool.addAll(offspring);
        java.util.Collections.shuffle(pool, random);
        rank(pool);
        pool.sort(Comparator.comparingDouble(Individual::getScalarFitness).reversed());
        individuals = new ArrayList<>(pool.subList(0, Math.min(size, pool.size())));
        rank(individuals);
    }

    /** @return the individual with the lowest cost on {@code task} among those evaluated on it, or {@code null} */
    Individual bestOn(int task) {
        Individual best = null;
        for (Individual ind : individuals) {
            if (ind.isEvaluatedOn(task) && (best == null || ind.getCost(task) < best.getCost(task))) best = ind;
        }
        return best;
    }
}
