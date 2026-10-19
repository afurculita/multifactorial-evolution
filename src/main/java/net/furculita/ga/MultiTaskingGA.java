package net.furculita.ga;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Multifactorial Evolutionary Algorithm (MFEA).
 *
 * <p>Based on "Multifactorial Evolution: Toward Evolutionary Multitasking" by Abhishek
 * Gupta, Yew-Soon Ong and Liang Feng, IEEE Transactions on Evolutionary Computation 20(3),
 * 2016. All tasks share one population in a unified random-key search space. Each
 * generation:
 * <ol>
 *   <li>produces {@code 2 * pairs} offspring by <em>assortative mating</em>: two random
 *       parents undergo simulated binary crossover if they share a skill factor or with the
 *       random mating probability {@code rmp}; otherwise each parent is mutated on its own
 *       with polynomial mutation;</li>
 *   <li>assigns each child a skill factor by <em>vertical cultural transmission</em>
 *       (crossover children imitate a random parent, mutants their parent), repairs it for
 *       that task, applies the task's optional local search and evaluates it on that task
 *       only (<em>selective evaluation</em>);</li>
 *   <li>ranks parents and offspring together on every task and keeps the
 *       {@code populationSize} individuals with the highest scalar fitness;</li>
 *   <li>updates the per-task elites (immutable snapshots) and, after
 *       {@code restartPatience} generations without any elite improving, re-initialises
 *       the population and re-inserts the elites.</li>
 * </ol>
 */
public class MultiTaskingGA {

    /** Receives the best known cost of every task after each generation. */
    @FunctionalInterface
    public interface ProgressListener {
        void onGeneration(int generation, double[] bestCostPerTask);
    }

    /** Random mating probability used by Gupta, Ong and Feng (2016). */
    public static final double DEFAULT_RMP = 0.3;
    /** SBX distribution index used by the paper. */
    public static final double SBX_ETA = 2.0;
    /** Polynomial mutation distribution index used by the paper. */
    public static final double PM_ETA = 5.0;

    private final List<Task> tasks;
    private final double rmp;
    private final int restartPatience;
    private final Random random;
    private final Population population;
    private double mutationRate;
    private ProgressListener listener = (g, best) -> {
    };

    /**
     * @param tasks           tasks to optimise together (all are minimised)
     * @param populationSize  population size
     * @param rmp             random mating probability: chance that parents with different
     *                        skill factors are crossed over instead of mutated separately
     * @param restartPatience generations without improvement of any elite before the
     *                        population is re-initialised; {@code 0} disables restarts
     * @param seed            seed of the random source, for reproducible runs
     */
    public MultiTaskingGA(List<Task> tasks, int populationSize, double rmp, int restartPatience, long seed) {
        if (tasks.isEmpty()) throw new IllegalArgumentException("at least one task is required");
        if (populationSize < 2) throw new IllegalArgumentException("population size must be at least 2");
        if (rmp < 0 || rmp > 1) throw new IllegalArgumentException("rmp must be in [0, 1]");
        this.tasks = List.copyOf(tasks);
        this.rmp = rmp;
        this.restartPatience = restartPatience;
        this.random = new Random(seed);
        this.population = new Population(this.tasks, populationSize, random);
        this.mutationRate = 1.0 / population.chromosomeLength();
    }

    /** Per-gene polynomial mutation probability; defaults to {@code 1 / chromosome length}. */
    public MultiTaskingGA setMutationRate(double mutationRate) {
        if (mutationRate < 0 || mutationRate > 1) throw new IllegalArgumentException("mutation rate must be in [0, 1]");
        this.mutationRate = mutationRate;
        return this;
    }

    /** Called after every generation with the best known cost per task. */
    public MultiTaskingGA setProgressListener(ProgressListener listener) {
        this.listener = Objects.requireNonNull(listener);
        return this;
    }

    /**
     * Runs the algorithm.
     *
     * @param generations number of generations
     * @param pairs       number of parent pairs mated per generation, i.e. {@code 2 * pairs} offspring
     * @return the best individual found for each task, in task order (independent copies;
     *         each carries at least the cost on its own task)
     */
    public List<Individual> run(int generations, int pairs) {
        population.init();
        List<Individual> elites = new ArrayList<>();
        for (int t = 0; t < tasks.size(); t++) elites.add(population.bestOn(t).copy());

        int stall = 0;
        for (int g = 1; g <= generations; g++) {
            List<Individual> offspring = new ArrayList<>(2 * pairs);
            List<Individual> parents = population.individuals();
            for (int p = 0; p < pairs; p++) {
                Individual a = parents.get(random.nextInt(parents.size()));
                Individual b = parents.get(random.nextInt(parents.size()));
                while (b == a) b = parents.get(random.nextInt(parents.size()));
                if (a.getSkillFactor() == b.getSkillFactor() || random.nextDouble() < rmp) {
                    List<Double> c1 = new ArrayList<>(a.genes());
                    List<Double> c2 = new ArrayList<>(b.genes());
                    Operators.sbx(c1, c2, SBX_ETA, random);
                    offspring.add(child(c1, random.nextBoolean() ? a : b));
                    offspring.add(child(c2, random.nextBoolean() ? a : b));
                } else {
                    offspring.add(child(new ArrayList<>(a.genes()), a));
                    offspring.add(child(new ArrayList<>(b.genes()), b));
                }
            }

            population.selectFrom(offspring);

            boolean improved = false;
            for (int t = 0; t < tasks.size(); t++) {
                Individual best = population.bestOn(t);
                if (best != null && best.getCost(t) < elites.get(t).getCost(t)) {
                    elites.set(t, best.copy());
                    improved = true;
                }
            }
            stall = improved ? 0 : stall + 1;
            if (restartPatience > 0 && stall >= restartPatience) {
                population.init();
                population.reinsert(elites);
                stall = 0;
            }

            double[] bestCosts = new double[tasks.size()];
            for (int t = 0; t < tasks.size(); t++) bestCosts[t] = elites.get(t).getCost(t);
            listener.onGeneration(g, bestCosts);
        }

        List<Individual> result = new ArrayList<>(elites.size());
        for (Individual e : elites) result.add(e.copy());
        return result;
    }

    /**
     * Finishes a child: polynomial mutation, skill factor inherited from {@code parent},
     * repair and local search for that task, evaluation on that task only.
     */
    private Individual child(List<Double> genes, Individual parent) {
        Operators.polynomialMutation(genes, mutationRate, PM_ETA, random);
        int skill = parent.getSkillFactor();
        Task task = tasks.get(skill);
        if (task.isInfeasible(genes)) task.repair(genes);
        task.localSearch(genes);
        Individual child = new Individual(genes, tasks.size());
        child.setSkillFactor(skill);
        child.setCost(skill, task.computeFitness(genes));
        return child;
    }
}
