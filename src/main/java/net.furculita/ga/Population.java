package net.furculita.ga;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class Population {
    int nIndividual;
    private int nTask;
    private int lenGen;
    private List<Individual> individuals = null;
    private List<Task> tasks;

    Population(int n, List<Task> tasks) {
        this.nIndividual = n;
        this.nTask = tasks.size();
        this.tasks = tasks;
        int max = 0;
        int min = Integer.MAX_VALUE;
        for (Task task : tasks) {
            if (task.getLenGen() > max) max = task.getLenGen();
            if (task.getLenGen() < min) min = task.getLenGen();
        }
        lenGen = max;
    }

    void init() {
        Random r = new Random();
        individuals = new ArrayList<>();

        for (int i = 0; i < nIndividual; i++) {
            ArrayList<Double> g = new ArrayList<>();
            for (int j = 0; j < lenGen; j++) {
                g.add(r.nextDouble());
            }
            if (checkIndividualVail(g))
                makeIndividualVail(g);

            ArrayList<Double> fitnessTa = new ArrayList<>();
            for (Task task : tasks) {
                fitnessTa.add(task.computeFitness(g));
            }

            Individual ind = new Individual(g, fitnessTa);
            individuals.add(ind);
        }

        updateRankPopulation();
    }

    boolean checkIndividualVail(ArrayList<Double> ind) {
        for (Task t : tasks) {
            if (t.checkIndividualVail(ind)) {
                return true;
            }
        }

        return false;
    }

    void makeIndividualVail(ArrayList<Double> ind) {
        int i = 0;
        int xd = 0;
        while (true) {
            Task t = tasks.get(i);
            if (t.checkIndividualVail(ind)) {
                xd = 0;
                t.makeIndividualVail(ind);
            } else {
                xd++;
            }
            if (xd >= tasks.size()) {
                break;
            }
            i = (i + 1) % tasks.size();
        }
    }

    void updateRankPopulation() {
        ArrayList<ArrayList<Individual>> rankInTask = new ArrayList<>();

        for (int i = 0; i < nTask; i++) {
            ArrayList<Individual> lstIndividualInTask = new ArrayList<>();
            rankInTask.add(lstIndividualInTask);
        }

        for (int i_in = 0; i_in < nIndividual; i_in++) {
            Individual ind = individuals.get(i_in);
            for (int i = 0; i < nTask; i++) {
                ArrayList<Individual> lstIndividualInTask = rankInTask.get(i);
                boolean check = true;
                for (int j = 0; j < lstIndividualInTask.size(); j++) {
                    if (lstIndividualInTask.get(j).getFitnessTask().get(i) > ind.getFitnessTask().get(i)) {
                        lstIndividualInTask.add(j, ind);
                        check = false;
                        break;
                    }
                }
                if (check) {
                    lstIndividualInTask.add(ind);
                }
                rankInTask.set(i, lstIndividualInTask);
            }
        }

        for (int i = 0; i < nIndividual; i++) {
            Individual ind = individuals.get(i);
            ArrayList<Integer> factorial_rank = new ArrayList<>();
            int min_rank = nIndividual + 2;
            int task_rank_min = -1;
            for (int j = 0; j < nTask; j++) {
                int rankJ = rankInTask.get(j).indexOf(ind) + 1;
                factorial_rank.add(rankJ);
                if (rankJ < min_rank) {
                    min_rank = rankJ;
                    task_rank_min = j;
                }
            }

            ind.setFactorial_rank(factorial_rank);
            ind.setSkillFactor(task_rank_min);
            ind.setScalarFitness(1.0 / (min_rank));
        }

    }

    void add(List<Individual> offsprings) {
        individuals.addAll(offsprings);

        for (int in = 0; in < offsprings.size(); in++) {
            Individual child = offsprings.get(in);
            int child_task = child.getSkillFactor();

            List<Individual> rankInTask = countRank(child_task);
            int index = -1;
            for (int j = 0; j < rankInTask.size(); j++) {
                if (rankInTask.get(j).getFitnessTask().get(child_task) > child.getFitnessTask().get(child_task)) {
                    index = j;
                    break;
                }
            }
            if (index > -1)
                for (int j = index; j < rankInTask.size(); j++) {
                    Individual tmp = rankInTask.get(j);
                    List<Integer> rank = tmp.getFactorialRank();
                    rank.set(child_task, rank.get(child_task) + 1);
                    tmp.setFactorial_rank(rank);
                }
            else {
                index = rankInTask.size();
            }
            ArrayList<Integer> facRankInd = new ArrayList<>();
            for (int ii = 0; ii < nTask; ii++)
                facRankInd.add(individuals.size() + 1);
            facRankInd.set(child_task, index + 1);
            child.setFactorial_rank(facRankInd);
            offsprings.set(in, child);
        }

        for (Individual ind : offsprings) {
            ind.setScalarFitness(1 / (ind.getMinFactorialRank()));
        }
    }

    private List<Individual> countRank(int task) {
        List<Individual> lstIndividualInTask = new ArrayList<>();

        for (Individual ind : individuals) {
            boolean check = true;
            for (int j = 0; j < lstIndividualInTask.size(); j++) {
                if (lstIndividualInTask.get(j).getFitnessTask().get(task)
                        > ind.getFitnessTask().get(task)) {
                    lstIndividualInTask.add(j, ind);
                    check = false;
                    break;
                }
            }
            if (check) {
                lstIndividualInTask.add(ind);
            }
        }

        return lstIndividualInTask;
    }

    Individual getIndividualBestOfTask(int task) {
        Individual best = null;
        for (Individual individual : individuals) if (individual.factorial_rank.get(task) == 1) best = individual;
        return best;
    }

    List<Individual> getIndividuals() {
        return individuals;
    }

    void setIndividuals(ArrayList<Individual> individuals) {
        this.individuals = individuals;
    }
}
