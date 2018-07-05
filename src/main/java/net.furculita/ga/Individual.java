package net.furculita.ga;

import java.util.ArrayList;

public class Individual {
    ArrayList<Double> gen;
    ArrayList<Double> fitnessTask;
    ArrayList<Integer> factorial_rank;
    private int skillFactor;
    private double scalarFitness;

    Individual(ArrayList<Double> gen, ArrayList<Double> fitnessTask) {
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

    ArrayList<Double> getGen() {
        return gen;
    }

    ArrayList<Double> getFitnessTask() {
        return fitnessTask;
    }

    void setFitnessTask(ArrayList<Double> fitnessTask) {
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

    ArrayList<Integer> getFactorial_rank() {
        return factorial_rank;
    }

    void setFactorial_rank(ArrayList<Integer> factorial_rank) {
        this.factorial_rank = factorial_rank;
    }

    @Override
    public String toString() {
        return "Individual [gen=" + gen + ", fitnessTask=" + fitnessTask
                + ", factorialRank=" + factorial_rank + ", skillFactor="
                + skillFactor + ", scalarFitness=" + scalarFitness + "]";
    }
}
