# Multifactorial Evolution

A Java implementation of the **Multifactorial Evolutionary Algorithm (MFEA)**, an
evolutionary-multitasking method that optimises several combinatorial problems at once
with a single population. The repository ships two task types, the symmetric travelling
salesman problem (TSP) and the 0/1 knapsack problem, together with TSPLIB and knapsack
benchmark instances.

Evolutionary computation traditionally solves one optimisation problem at a time. In
multi-echelon supply chain networks, however, several permutation-based combinatorial
optimisation problems (TSP, job-shop scheduling, ...) coexist, and it is desirable to
solve them together. Multifactorial optimisation (MFO) treats every task as a factor
influencing the evolution of one population, so that useful genetic material found for one
task can transfer implicitly to the others.

## How the algorithm works

The implementation follows Gupta, Ong and Feng (2016):

| Concept | Where |
| --- | --- |
| **Unified search space.** Every individual is a random-key chromosome, a list of doubles in `[0, 1)` whose length is the largest task dimension. Each task decodes the chromosome into its own representation: the TSP visits cities in ascending key order, the knapsack selects items whose key rounds to 1. | `Task`, `TSP`, `Knapsack` |
| **Factorial cost, factorial rank, skill factor, scalar fitness.** An individual's cost on each task, its rank per task, the task it is best at, and `1 / min(rank)`, the single value used to compare individuals across tasks. | `Individual`, `Population` |
| **Assortative mating.** Two random parents are crossed over (one-point crossover) if they share a skill factor or with probability `1 - pOfMutation`; otherwise each is mutated on its own (one random gene is resampled). | `MultiTaskingGA.run` |
| **Vertical cultural transmission and selective evaluation.** A crossover child inherits the skill factor of a random parent, a mutant that of its parent, and is evaluated on that task only. | `MultiTaskingGA.crossOver`, `mutation` |
| **Elitist survivor selection.** Parents and offspring compete; the `numOfInd` individuals with the highest scalar fitness survive. | `MultiTaskingGA.selection` |
| **Restart.** If no task improves for `timeResetPopulation` generations, the population is re-initialised; the best individual per task is kept outside the population. | `MultiTaskingGA.run` |

Infeasible chromosomes (an over-weight knapsack selection) are repaired in place before
evaluation, cycling over all tasks until every constraint holds.

## Project layout

```
src/main/java/net/furculita/
  MFEAlgorithmRun.java      example entry point (5 knapsacks + kroA100)
  ga/MultiTaskingGA.java    the MFEA loop: mating, offspring, selection
  ga/Population.java        population, repair, factorial ranks
  ga/Individual.java        chromosome + multifactorial bookkeeping
  ga/Task.java              task contract
  tasks/TSP.java            TSP task (random-key decoding)
  tasks/Knapsack.java       0/1 knapsack task (rounding decoding + greedy repair)
  util/TSPFileParser.java   TSPLIB reader (EUC_2D and GEO)
src/test/java/              JUnit 5 tests
data/tsp/                   TSPLIB instances (*.tsp) and optimal tours (*.opt.tour)
data/knapsack/{50,100}/     knapsack instances with 50 and 100 items
```

## Building and running

Requirements: JDK 21 or newer and Maven 3.9 or newer (both enforced by the build).

```bash
mvn verify                       # compile, run the tests, build the jar
java -jar target/multifactorial-evolution-1.0-SNAPSHOT.jar
mvn exec:java                    # same, through Maven
```

Run from the repository root: instance paths in `MFEAlgorithmRun` are relative to it.
The program prints, for every generation and task, the factorial cost vector of the
individual ranked first on that task, then the best individual found per task.

To optimise other instances or change the parameters, edit `MFEAlgorithmRun`:

```java
List<Task> tasks = List.of(new Knapsack("data/knapsack/100/s000.kp"),
                           new TSP("data/tsp/kroA100.tsp"));
// population size, mutation probability for cross-task pairs, restart patience
MultiTaskingGA ga = new MultiTaskingGA(tasks, 50, 0.1, 50);
List<Individual> best = ga.run(50);   // 50 parent pairs per generation
```

The number of generations is the constant `MultiTaskingGA.ITERATIONS` (1000).

## Data formats

**TSP** instances are standard [TSPLIB](http://comopt.ifi.uni-heidelberg.de/software/TSPLIB95/)
files. Only `EDGE_WEIGHT_TYPE` `EUC_2D` (rounded Euclidean distance) and `GEO`
(geographical distance) are supported; the parser applies the TSPLIB rounding rules, so
tour lengths match the published optima (for example `berlin52` = 7542, `ulysses16` = 6859).
Instances in `EXPLICIT`, `ATT` or `CEIL_2D` format are rejected with an error. Reported
fitness is the length of the closed tour.

**Knapsack** instances are plain text, whitespace separated:

```
<number of items>
<capacity>
<weight> <value>       (one line per item)
```

Fitness is the negated total value of the selected items, so that every task is
minimised.

## Tests

`mvn test` runs the JUnit 5 suite: TSPLIB parsing checked against published optimal tours,
TSP and knapsack decoding, feasibility and repair, and two short end-to-end runs of the
algorithm (including tasks of different dimension sharing one population).

## Known limitations and suggested improvements

See [REVIEW.md](REVIEW.md) for a review of the implementation against the papers, the
bugs fixed in the latest revision and a prioritised list of improvements.

## References

1. A. Gupta, Y.-S. Ong and L. Feng, "Multifactorial Evolution: Toward Evolutionary
   Multitasking," *IEEE Transactions on Evolutionary Computation*, vol. 20, no. 3,
   pp. 343–357, June 2016. doi:10.1109/TEVC.2015.2458037
2. Y. Yuan, Y.-S. Ong, A. Gupta, P. S. Tan and H. Xu, "Evolutionary multitasking in
   permutation-based combinatorial optimization problems: Realization with TSP, QAP, LOP,
   and JSP," *2016 IEEE Region 10 Conference (TENCON)*, Singapore, 2016, pp. 3157–3164.
   doi:10.1109/TENCON.2016.7848632
