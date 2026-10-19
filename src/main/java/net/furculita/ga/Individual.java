package net.furculita.ga;

import java.util.List;

/**
 * A member of the multitasking population.
 *
 * <p>Following Gupta, Ong and Feng (2016) an individual carries, besides its random-key
 * chromosome, the multifactorial bookkeeping the algorithm needs:
 * <ul>
 *   <li><b>factorial cost</b> ({@code fitnessTask}): its objective value on every task.
 *       Tasks the individual was not evaluated on hold {@code MultiTaskingGA.LIMIT};</li>
 *   <li><b>factorial rank</b>: its 1-based position in the population when sorted by
 *       factorial cost on each task;</li>
 *   <li><b>skill factor</b>: the index of the task on which it ranks best;</li>
 *   <li><b>scalar fitness</b>: {@code 1 / min(factorial rank)}, the single value used
 *       for survivor selection across tasks.</li>
 * </ul>
 */
public class Individual {
    List<Double> gen;
    List<Double> fitnessTask;
    List<Integer> factorial_rank;
    private int skillFactor;
    private double scalarFitness;

    /**
     * @param gen         the random-key chromosome
     * @param fitnessTask the factorial cost on every task, or {@code null} if not yet known
     */
    Individual(List<Double> gen, List<Double> fitnessTask) {
        super();
        this.gen = gen;
        this.fitnessTask = fitnessTask;
    }

    /** @return the best (smallest) factorial rank over all tasks */
    Integer getMinFactorialRank() {
        Integer min = 10000000;
        for (Integer tmp : factorial_rank) {
            if (min > tmp) min = tmp;
        }
        return min;
    }

    /** @return the random-key chromosome */
    public List<Double> getGen() {
        return gen;
    }

    /** @return the factorial cost on every task (see class comment) */
    public List<Double> getFitnessTask() {
        return fitnessTask;
    }

    void setFitnessTask(List<Double> fitnessTask) {
        this.fitnessTask = fitnessTask;
    }

    /** @return the index of the task on which this individual ranks best */
    public int getSkillFactor() {
        return skillFactor;
    }

    void setSkillFactor(int skillFactor) {
        this.skillFactor = skillFactor;
    }

    /** @return {@code 1 / min(factorial rank)}; higher is better */
    public double getScalarFitness() {
        return scalarFitness;
    }

    void setScalarFitness(double scalarFitness) {
        this.scalarFitness = scalarFitness;
    }

    /** @return the 1-based rank on every task */
    public List<Integer> getFactorialRank() {
        return factorial_rank;
    }

    void setFactorial_rank(List<Integer> factorial_rank) {
        this.factorial_rank = factorial_rank;
    }

    @Override
    public String toString() {
        return "Individual [gen=" + gen + ", fitnessTask=" + fitnessTask
                + ", factorialRank=" + factorial_rank + ", skillFactor="
                + skillFactor + ", scalarFitness=" + scalarFitness + "]";
    }
}
