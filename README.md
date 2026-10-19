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

The implementation follows Algorithm 1 of Gupta, Ong and Feng (2016):

| Concept | Where |
| --- | --- |
| **Unified search space.** Every individual is a random-key chromosome, a list of doubles in `[0, 1]` whose length is the largest task dimension. A task of dimension `D` reads the first `D` genes: the TSP visits cities in ascending key order, the knapsack selects items whose key is above 0.5. | `Task`, `TSP`, `Knapsack` |
| **Factorial cost, factorial rank, skill factor, scalar fitness.** An individual's cost on each task it was evaluated on, its rank per task (unevaluated tasks rank last), the task it is best at, and `1 / min(rank)`, the single value used to compare individuals across tasks. | `Individual`, `Population.rank` |
| **Assortative mating.** Two random parents undergo simulated binary crossover (SBX, η = 2) if they share a skill factor or with the random mating probability `rmp`; otherwise each is mutated on its own. Every child also receives polynomial mutation (η = 5, per-gene probability `1 / length`). | `MultiTaskingGA.run`, `Operators` |
| **Vertical cultural transmission and selective evaluation.** A crossover child imitates the skill factor of a random parent, a mutant that of its parent. The child is repaired for that task, improved by the task's optional local search, and evaluated on that task only. | `MultiTaskingGA.child` |
| **Local search.** TSP offspring get a bounded first-improvement 2-opt pass whose result is written back into the genes (Lamarckian learning). The knapsack has no local search. | `TSP.localSearch` |
| **Elitist survivor selection.** Parents and offspring are ranked together on every task once per generation; the `populationSize` individuals with the highest scalar fitness survive. | `Population.selectFrom` |
| **Elites and restarts.** The best individual of every task is kept as an immutable snapshot. If no elite improves for `restartPatience` generations, the population is re-initialised and the elites are re-inserted. | `MultiTaskingGA.run` |

Infeasible chromosomes (an over-weight knapsack selection) are repaired in place. The
initial population is repaired and evaluated for every task; offspring only for their
skill-factor task.

## Project layout

```
src/main/java/net/furculita/
  MFEAlgorithmRun.java      command-line entry point
  ga/MultiTaskingGA.java    the MFEA loop: mating, offspring, elites, restarts
  ga/Population.java        population, factorial ranking, survivor selection
  ga/Operators.java         SBX crossover and polynomial mutation on [0, 1]
  ga/Individual.java        chromosome + multifactorial bookkeeping (read-only outside the package)
  ga/Task.java              task contract: fitness, feasibility, repair, optional local search
  tasks/TSP.java            TSP task (random-key decoding, 2-opt local search)
  tasks/Knapsack.java       0/1 knapsack task (threshold decoding, greedy repair)
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

Run from the repository root: the default instance paths are relative to it. Without
arguments the program optimises five 100-item knapsacks (`data/knapsack/100/s000..s004.kp`)
together with the 100-city TSP `kroA100`, prints the best known cost of every task every
50 generations and finally the best individual per task.

```
java -jar target/multifactorial-evolution-1.0-SNAPSHOT.jar [options] [instance ...]

  instance            a TSPLIB .tsp file (TSP task) or a .kp file (knapsack task)
  --generations N     number of generations (default 1000)
  --population N      population size (default 50)
  --pairs N           parent pairs per generation, i.e. 2N offspring (default 50)
  --rmp P             random mating probability (default 0.3, as in the paper)
  --mutation P        per-gene mutation probability (default 1 / chromosome length)
  --restart N         generations without improvement before a restart; 0 = never (default 100)
  --local-search N    2-opt sweeps per TSP offspring; 0 = off (default 1)
  --seed S            random seed; the same seed reproduces the same run (default 42)
  --print-every N     progress line every N generations (default 50)
```

For example, two TSPs of different size in one population:

```bash
java -jar target/multifactorial-evolution-1.0-SNAPSHOT.jar --seed 1 data/tsp/berlin52.tsp data/tsp/eil101.tsp
```

From Java:

```java
List<Task> tasks = List.of(new Knapsack("data/knapsack/100/s000.kp"),
                           new TSP("data/tsp/kroA100.tsp"));
MultiTaskingGA ga = new MultiTaskingGA(tasks, 50, 0.3, 100, 42L)   // population, rmp, restart patience, seed
        .setProgressListener((generation, bestCosts) -> { /* ... */ });
List<Individual> best = ga.run(1000, 50);                          // generations, parent pairs per generation
double tourLength = best.get(1).getCost(1);
```

### Results on the default configuration

Single runs, seed 42, about 2 seconds. Knapsack optima were computed
by dynamic programming; the `kroA100` optimum is the published 21,282.

| Task | Optimum | Best found |
| --- | ---: | ---: |
| knapsack s000 | 32,066 | 32,066 |
| knapsack s001 | 35,046 | 34,958 |
| knapsack s002 | 33,615 | 33,481 |
| knapsack s003 | 37,517 | 37,498 |
| knapsack s004 | 43,873 | 42,599 |
| TSP kroA100 (tour length) | 21,282 | 21,546 |

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
TSP and knapsack decoding, feasibility and repair, the SBX and polynomial mutation bounds,
factorial ranking with unevaluated tasks, 2-opt never worsening a tour, and short end-to-end
runs (elites never regress across restarts, the same seed reproduces the same result, tasks
of different dimension share one population, burma14 reaches its published optimum).

## Known limitations and suggested improvements

See [REVIEW.md](REVIEW.md) for a review of the implementation against the papers, the
bugs fixed, the improvements implemented since and the remaining suggestions.

## References

1. A. Gupta, Y.-S. Ong and L. Feng, "Multifactorial Evolution: Toward Evolutionary
   Multitasking," *IEEE Transactions on Evolutionary Computation*, vol. 20, no. 3,
   pp. 343–357, June 2016. doi:10.1109/TEVC.2015.2458037
2. Y. Yuan, Y.-S. Ong, A. Gupta, P. S. Tan and H. Xu, "Evolutionary multitasking in
   permutation-based combinatorial optimization problems: Realization with TSP, QAP, LOP,
   and JSP," *2016 IEEE Region 10 Conference (TENCON)*, Singapore, 2016, pp. 3157–3164.
   doi:10.1109/TENCON.2016.7848632
