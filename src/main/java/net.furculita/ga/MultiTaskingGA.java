package net.furculita.ga;

import java.util.ArrayList;
import java.util.Random;

/**
 * Based on "Multifactorial Evolution: Towards Evolutionary
 * Multitasking", by Abhishek Gupta, Yew-Soon Ong, and Liang Feng
 */
public class MultiTaskingGA {
    private Population population;
    private int timeResetPopulation;
    private double pOfMutation;
    private ArrayList<Task> tasks;
    private static final double LIMIT = 10000000000.0;
    private static final int ITERATIONS = 1000;

    public MultiTaskingGA(ArrayList<Task> tasks, int numOfInd, double pOfMutation, int timeResetPopulation) {
        this.tasks = tasks;
        this.timeResetPopulation = timeResetPopulation;
        this.pOfMutation = pOfMutation;
        population = new Population(numOfInd, tasks);
    }

    public void run(int nN) {
        ArrayList<Individual> bestSolution = new ArrayList<>();
        Random r = new Random();

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

            ArrayList<Individual> individuals = population.getIndividuals();
            ArrayList<Individual> children = new ArrayList<>();

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
    }

    private void reComputeFitnessTaskForChild(ArrayList<Individual> children) {
        for (Individual child : children) {
            ArrayList<Double> fT = child.getFitnessTask();
            for (int j = 0; j < tasks.size(); j++)
                if (fT.get(j) == LIMIT) {
                    Task t = tasks.get(j);
                    fT.set(j, t.computeFitness(child.gen));
                }
        }
    }

    private ArrayList<Individual> crossOver(Individual a, Individual b) {
        ArrayList<Individual> children = new ArrayList<>();
        ArrayList<Integer> fR = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) fR.add(population.getIndividuals().size() + 1);
        Random r = new Random();

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
        double rand = Math.random();

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

        rand = Math.random();
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
        ind2.setFactorial_rank(fR);
        children.add(ind2);

        return children;
    }

    private Individual mutation(Individual a) {
        Random r = new Random();
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

    private void selection() {
        population.getIndividuals().sort((i1, i2) -> {
            Double di1 = i1.getScalarFitness();
            Double di2 = i2.getScalarFitness();
            return di1.compareTo(di2);
        });

        ArrayList<Individual> newIndividuals = new ArrayList<>();
        for (int i = 0; i < population.nIndividual; i++) {
            newIndividuals.add(population.getIndividuals().get(i));
        }

        population.setIndividuals(newIndividuals);
    }
}
