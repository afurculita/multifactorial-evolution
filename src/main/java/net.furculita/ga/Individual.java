package net.furculita.ga;

import java.util.List;

public class Individual {
    List<Double> gen;
    List<Double> fitnessTask;
    List<Integer> factorial_rank;
    private int skillFactor;
    private double scalarFitness;

    Individual(List<Double> gen, List<Double> fitnessTask) {
        super();
        this.gen = gen;
        this.fitnessTask = fitnessTask;
    }

    Integer getMinFactorialRank() {
        Integer min = 10000000;
        for (Integer tmp : factorial_rank) {
            if (min > tmp) min = tmp;
        }
        return min;
    }

    List<Double> getGen() {
        return gen;
    }

    List<Double> getFitnessTask() {
        return fitnessTask;
    }

    void setFitnessTask(List<Double> fitnessTask) {
        this.fitnessTask = fitnessTask;
    }

    int getSkillFactor() {
        return skillFactor;
    }

    void setSkillFactor(int skillFactor) {
        this.skillFactor = skillFactor;
    }

    double getScalarFitness() {
        return scalarFitness;
    }

    void setScalarFitness(double scalarFitness) {
        this.scalarFitness = scalarFitness;
    }

    List<Integer> getFactorialRank() {
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
