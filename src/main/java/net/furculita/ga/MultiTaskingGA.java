package net.furculita.ga;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Multifactorial Evolutionary Algorithm (MFEA).
 *
 * <p>Based on "Multifactorial Evolution: Toward Evolutionary Multitasking" by Abhishek
 * Gupta, Yew-Soon Ong and Liang Feng, IEEE Transactions on Evolutionary Computation 20(3),
 * 2016. All tasks share one population in a unified random-key search space. Each
 * generation:
 * <ol>
 *   <li>tracks the best individual per task and re-initialises the population after
 *       {@code timeResetPopulation} generations without any improvement;</li>
 *   <li>produces {@code 2 * nN} offspring by <em>assortative mating</em>: two random parents
 *       undergo one-point crossover if they share a skill factor or with probability
 *       {@code 1 - pOfMutation}, otherwise each is mutated on its own;</li>
 *   <li>assigns each child a skill factor by <em>vertical cultural transmission</em>
 *       (crossover children copy a random parent, mutants copy their parent) and evaluates
 *       it on that task only;</li>
 *   <li>keeps the {@code numOfInd} individuals with the highest scalar fitness.</li>
 * </ol>
 * The generation count is fixed by {@link #ITERATIONS}.
 */
public class MultiTaskingGA {
    private Population population;
    private int timeResetPopulation;
    private double pOfMutation;
    private List<Task> tasks;
    private final Random random = new Random();

    /** Placeholder factorial cost for a task an individual has not been evaluated on. */
    private static final double LIMIT = 10000000000.0;

    /** Number of generations of a {@link #run(int)}. */
    private static final int ITERATIONS = 1000;

    /**
     * @param tasks               tasks to optimise together (all are minimised)
     * @param numOfInd            population size
     * @param pOfMutation         probability that two parents with different skill factors are
     *                            mutated instead of crossed over; {@code 1 - pOfMutation} plays
     *                            the role of the paper's random mating probability {@code rmp}
     * @param timeResetPopulation generations without improvement on any task before the
     *                            population is re-initialised
     */
    public MultiTaskingGA(List<Task> tasks, int numOfInd, double pOfMutation, int timeResetPopulation) {
        this.tasks = tasks;
        this.timeResetPopulation = timeResetPopulation;
        this.pOfMutation = pOfMutation;
        population = new Population(numOfInd, tasks);
    }

    /**
     * Runs {@link #ITERATIONS} generations and prints the best factorial cost of every task
     * at each generation.
     *
     * @param nN number of parent pairs mated per generation, i.e. {@code 2 * nN} offspring
     * @return the best individual found for each task, in task order
     */
    public List<Individual> run(int nN) {
        ArrayList<Individual> bestSolution = new ArrayList<>();
        Random r = random;

        population.init();

        for (int i = 0; i < tasks.size(); i++)
            bestSolution.add(population.getIndividuals().get(i));

        int changeBest = 0;

        for (int i = 0; i < ITERATIONS; i++) {
            for (int ii = 0; ii < tasks.size(); ii++) {
                Individual ind = population.getIndividualBestOfTask(ii);
                if (bestSolution.get(ii).fitnessTask.get(ii) > ind.getFitnessTask().get(ii)) {
                    changeBest = 0;
                    bestSolution.set(ii, ind);
                }

                System.out.println(i + ":" + ii + ": " + ind.getFitnessTask());
            }

            changeBest++;
            if (changeBest >= timeResetPopulation) {
                population.init();
                changeBest = 0;
            }

            List<Individual> individuals = population.getIndividuals();
            List<Individual> children = new ArrayList<>();

            for (int j = 0; j < nN; j++) {
                Individual a = individuals.get(r.nextInt(individuals.size()));
                Individual b = individuals.get(r.nextInt(individuals.size()));
                while (a == b) b = individuals.get(r.nextInt(individuals.size()));
                int ta = a.getSkillFactor();
                int tb = b.getSkillFactor();
                double t = r.nextDouble();

                if ((ta == tb) || (t > pOfMutation)) {
                    children.addAll(crossOver(a, b));
                } else {
                    Individual ia = mutation(a);
                    Individual ib = mutation(b);
                    children.add(ia);
                    children.add(ib);
                }
            }

            population.add(children);
            selection();
            reComputeFitnessTaskForChild(children);
            population.updateRankPopulation();
        }

        System.out.println("Solution:");
        for (Individual aBestSolution : bestSolution) {
            System.out.println(aBestSolution);
        }

        return bestSolution;
    }

    /** Fills in the factorial cost of the tasks a child was not evaluated on. */
    private void reComputeFitnessTaskForChild(List<Individual> children) {
        for (Individual child : children) {
            List<Double> fT = child.getFitnessTask();
            for (int j = 0; j < tasks.size(); j++)
                if (fT.get(j) == LIMIT) {
                    Task t = tasks.get(j);
                    fT.set(j, t.computeFitness(child.gen));
                }
        }
    }

    /**
     * One-point crossover producing two children. Each child inherits the skill factor of a
     * random parent and is evaluated on that task only.
     */
    private ArrayList<Individual> crossOver(Individual a, Individual b) {
        ArrayList<Individual> children = new ArrayList<>();
        ArrayList<Integer> fR = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) fR.add(population.getIndividuals().size() + 1);
        Random r = random;

        int t = r.nextInt(a.getGen().size() - 1);
        ArrayList<Double> cb = new ArrayList<>();
        ArrayList<Double> ca = new ArrayList<>();
        for (int i = 0; i < t; i++) {
            ca.add(a.getGen().get(i));
            cb.add(b.getGen().get(i));
        }
        for (int i = t; i < a.getGen().size(); i++) {
            ca.add(b.getGen().get(i));
            cb.add(a.getGen().get(i));
        }

        if (population.checkIndividualVail(ca))
            population.makeIndividualVail(ca);

        Individual ind = new Individual(ca, null);
        double rand = r.nextDouble();

        if (rand < 0.5) {
            ind.setSkillFactor(a.getSkillFactor());
        } else {
            ind.setSkillFactor(b.getSkillFactor());
        }
        ArrayList<Double> fitnessTa = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++)
            if (i != ind.getSkillFactor())
                fitnessTa.add(LIMIT);
            else
                fitnessTa.add(tasks.get(i).computeFitness(ca));

        ind.setFitnessTask(fitnessTa);
        ind.setFactorial_rank(fR);
        children.add(ind);

        if (population.checkIndividualVail(cb))
            population.makeIndividualVail(cb);

        Individual ind2 = new Individual(cb, null);

        rand = r.nextDouble();
        if (rand < 0.5) {
            ind2.setSkillFactor(a.getSkillFactor());
        } else {
            ind2.setSkillFactor(b.getSkillFactor());
        }

        fitnessTa = new ArrayList<>();

        for (int i = 0; i < tasks.size(); i++)
            if (i != ind2.getSkillFactor())
                fitnessTa.add(LIMIT);
            else
                fitnessTa.add(tasks.get(i).computeFitness(cb));

        ind2.setFitnessTask(fitnessTa);
        ind2.setFactorial_rank(new ArrayList<>(fR));
        children.add(ind2);

        return children;
    }

    /**
     * Uniform mutation of a single random gene. The child keeps its parent's skill factor
     * and is evaluated on that task only.
     */
    private Individual mutation(Individual a) {
        Random r = random;
        ArrayList<Integer> fR = new ArrayList<>();

        for (int i = 0; i < tasks.size(); i++)
            fR.add(population.getIndividuals().size() + 1);

        int t = r.nextInt(a.getGen().size());
        ArrayList<Double> c = new ArrayList<>(a.getGen());

        c.set(t, r.nextDouble());

        if (population.checkIndividualVail(c))
            population.makeIndividualVail(c);

        Individual ind = new Individual(c, null);

        ind.setSkillFactor(a.getSkillFactor());

        ArrayList<Double> fitnessTa = new ArrayList<>();

        for (int i = 0; i < tasks.size(); i++)
            if (i != ind.getSkillFactor())
                fitnessTa.add(LIMIT);
            else
                fitnessTa.add(tasks.get(i).computeFitness(c));

        ind.setFitnessTask(fitnessTa);
        ind.setFactorial_rank(fR);

        return ind;
    }

    /**
     * Elitist survivor selection: keeps the {@code numOfInd} individuals with the
     * <em>highest</em> scalar fitness out of parents and offspring.
     */
    private void selection() {
        population.getIndividuals().sort((i1, i2) ->
                Double.compare(i2.getScalarFitness(), i1.getScalarFitness()));

        ArrayList<Individual> newIndividuals = new ArrayList<>();
        for (int i = 0; i < population.nIndividual; i++) {
            newIndividuals.add(population.getIndividuals().get(i));
        }

        population.setIndividuals(newIndividuals);
    }
}
