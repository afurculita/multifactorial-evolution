# Implementation review

Review of the MFEA implementation against Gupta, Ong & Feng (2016) and Yuan et al. (2016),
with the changes made in this revision and a prioritised list of further improvements.

## Summary

The code implements the right concepts (unified random-key space, factorial rank, skill
factor, scalar fitness, assortative mating, vertical cultural transmission, selective
evaluation), but the original revision had two defects in survivor selection that turned
the search into a near-random walk, plus several smaller correctness problems. Those are
fixed here. What remains are design choices that limit solution quality and clarity; they
are listed below with concrete suggestions rather than changed, so the author can decide.

## Measured effect of the fixes

Default configuration in `MFEAlgorithmRun` (5 × 100-item knapsacks + `kroA100`, population
50, 100 offspring per generation, 1000 generations). Knapsack optima computed by dynamic
programming; the `kroA100` optimum is the published 21,282. One run each, so treat the
numbers as indicative.

| Task | Optimum | Original best | Fixed best |
| --- | ---: | ---: | ---: |
| knapsack s000 | 32,066 | 26,107 (81%) | 28,860 (90%) |
| knapsack s001 | 35,046 | 21,742 (62%) | 26,372 (75%) |
| knapsack s002 | 33,615 | 21,215 (63%) | 27,858 (83%) |
| knapsack s003 | 37,517 | 23,169 (62%) | 26,207 (70%) |
| knapsack s004 | 43,873 | 25,874 (59%) | 28,787 (66%) |
| TSP kroA100 (tour length, lower is better) | 21,282 | 137,273 (6.5×) | 58,900 (2.8×) |

A random `kroA100` tour is about 170,000 long, so the original run never left the random
regime. The fixed run clearly optimises but is still far from optimal; see "Suggested
improvements" for why.

## Changes made in this revision

### Build and tooling

| Item | Before | After |
| --- | --- | --- |
| Packaging | `pom` (Maven compiled nothing; `mvn package` produced no classes or jar) | `jar` with a `Main-Class` manifest |
| Java | source/target 1.8 | `release` 21 (latest LTS supported by the pinned toolchain; the enforcer requires JDK 21+ and Maven 3.9+) |
| Source layout | `src/main/java/net.furculita/...` (directory named after the package, with a dot) | standard `src/main/java/net/furculita/...` |
| Dependencies | `commons-math3` 3.6.1 and `moeaframework` 2.12, neither imported anywhere | removed; JUnit Jupiter 6.1.3 (test scope) added |
| Plugins | unpinned defaults, compiler plugin without version | pinned to the latest stable Maven 3.x releases: compiler 3.16.0, surefire 3.6.0, jar 3.5.1, resources 3.5.0, clean 3.5.0, install 3.2.0, deploy 3.2.0, enforcer 3.6.3, exec 3.6.4 |
| CI | none | GitHub Actions workflow (`actions/checkout@v7`, `actions/setup-java@v6`, Temurin 21, `mvn verify`) |
| IDE files | `.idea` pinned to JDK 1.8 / language level 9 with stale library entries | language level 21, test source root, stale libraries dropped |

The Maven 4.0.0 beta plugin line and MOEA Framework 5.1 are the newest artifacts on
Maven Central; they were not adopted because the project runs on Maven 3.9 and does not
use MOEA Framework.

### Bugs fixed

1. **Survivor selection kept the worst individuals.** `MultiTaskingGA.selection` sorted by
   scalar fitness ascending and kept the first `numOfInd`, i.e. the lowest scalar fitness.
   Now sorts descending.
2. **Children's scalar fitness used integer division.** `Population.add` computed
   `1 / minRank` with an `Integer`, so every child not ranked first got 0. Combined with
   (1) the survivors were essentially the non-elite children, which is why the search was
   random. Now `1.0 / minRank`.
3. **TSP fitness omitted the closing edge.** The tour length ignored the return from the
   last city to the first, so values were not comparable with TSPLIB optima (berlin52's
   optimal tour measured 7496 instead of 7542). Fixed.
4. **GEO distances were wrong.** The degree part of `DDD.MM` coordinates was taken with
   `Math.floor` (TSPLIB uses integer truncation, which differs for negative coordinates)
   and the result was rounded (TSPLIB truncates). The optimal tours of ulysses16,
   ulysses22 and gr96 now measure exactly 6859, 7013 and 55209 as published; before, they
   measured 6756, 6912 and 54688.
5. **Unsupported TSPLIB formats failed silently.** An `EXPLICIT`/`ATT`/`CEIL_2D` file
   parsed to zero cities and crashed later with an index error. The parser now throws a
   `TSPException` naming the format. 23 of the 144 shipped `.tsp` files are in those
   formats and were never usable.
6. **Mixing tasks of different dimension crashed.** The sliding-window folding in
   `TSP.decode` and `Knapsack.decode` iterated by position up to the padded length and
   read past the end (`IndexOutOfBoundsException` for a 50-item knapsack with a 100-city
   TSP). The loops now produce exactly `dimension` windows.
7. **TSP fitness was non-deterministic for folded chromosomes.** `TSP.decode` drew a new
   random window on every call, so the same chromosome evaluated to a different tour each
   time (observed 130,133 vs 128,147 for one genotype). The window is now a per-task field,
   as it already was for `Knapsack`.
8. **Parse errors exited the JVM or deferred a NullPointerException.** `TSP` called
   `System.exit(0)` and `Knapsack` only printed the stack trace. Both constructors now throw
   `IllegalArgumentException`.
9. **Smaller items.** One `Random` instance instead of a new one per crossover/mutation
   call mixed with `Math.random()`; the two crossover children no longer share the same
   rank list object; file handles are closed with try-with-resources; `run` returns the
   best individual per task instead of only printing it (callers that ignored the `void`
   result are unaffected).

### Documentation and tests

* README rewritten: algorithm overview mapped to classes, build/run instructions, data
  formats, parameters, references with DOIs instead of private Paperpile links.
* Javadoc on every class and on the non-trivial methods.
* 14 JUnit tests: TSPLIB parsing against published optima, decoding, feasibility and
  repair, rejection of bad input, and two end-to-end runs (same-dimension tasks, and a TSP
  plus a knapsack of different dimension).

## Suggested improvements (not implemented)

Ordered by expected impact on solution quality and on the fidelity to the papers.

### 1. Make selective evaluation real and rank the combined population once

`MultiTaskingGA.run` calls `reComputeFitnessTaskForChild` after selection, which evaluates
every child (including the ones that were just discarded) on every task. That removes the
whole point of selective evaluation, which is the main source of MFEA's efficiency, and
costs `(T - 1) × offspring` extra evaluations per generation.

Also, parents keep the scalar fitness computed in the previous generation (ranks within
the old population of N), while children get ranks within the combined population of
N + offspring (`Population.add`). The two are not comparable, and `Population.add` is
O(N²) per child because it rebuilds a sorted list for each one.

Suggestion, following the paper's Algorithm 1:

```
combined = parents + offspring
for each task j:
    sort the individuals that have a cost on j (others are ranked last)
    assign factorial rank on j
scalar fitness = 1 / min_j rank_j
keep the best N by scalar fitness
```

That is O(T · M log M) per generation, uses the `LIMIT` placeholder as intended (never
evaluate a child on a task that is not its skill factor), and is exactly the ranking the
paper describes.

### 2. Use operators suited to the representation

One-point crossover on random keys and resampling a single gene are weak search operators
for permutations: a one-point cut keeps the relative order of keys on each side but the
join scrambles the tour at the cut. Options, in increasing fidelity to the papers:

* **Keep random keys** (continuous unified space, as in Gupta et al.): use simulated
  binary crossover (SBX, η ≈ 2) and polynomial mutation (η ≈ 5) as the paper does, plus a
  small per-gene mutation probability instead of exactly one gene.
* **Move to permutation chromosomes** (as in Yuan et al. for PCOPs): a unified permutation
  of length `max D_j`, with ordered crossover (OX) or PMX and swap/insertion mutation, and
  task-specific decoding (TSP: the permutation restricted to its cities; knapsack: greedy
  fill in permutation order). Yuan et al. report that this unified representation plus a
  modified survivor selection makes multitasking effective on TSP/QAP/LOP/JSP.
* Add a cheap local search (2-opt for TSP) to the skill-factor task of each child; the
  papers' baselines are memetic.

### 3. Handle differing task dimensions by truncation, not window folding

The paper's unified random-key space simply lets task `j` read the first `D_j` keys.
The custom sliding-window scheme (`Task.getStride`, the `window` arrays) is undocumented
in the papers, adds hidden state (`Knapsack.stride` is set as a side effect of `decode`
and consumed by `makeIndividualVail`), writes identical constants into several genes
during repair (which can create duplicate keys, see item 5) and is hard to reason about.
Truncation needs no padding, no stride and no per-task random weights.

### 4. Keep elites across restarts and stop mutating them

`population.init()` discards the whole population after `timeResetPopulation` generations
without improvement, including the current best of every task. The best individuals are
only remembered in `bestSolution`, but as live references: their `factorial_rank` lists
are later mutated by `Population.add`, so the ranks printed at the end are stale (one
printed best showed `[1, 1, 1, 1, 5, 70]`). Store immutable snapshots (copy the genes and
costs) and re-insert the per-task elites into the new population after a restart.

### 5. Replace `indexOf` decoding in `TSP.decode`

`x.indexOf(ts[i])` is O(n²) and returns the same city twice when two keys are equal, which
yields an invalid tour (a city visited twice, another skipped) with no error. Sort an index
array by key instead:

```java
Integer[] order = IntStream.range(0, dimension).boxed().toArray(Integer[]::new);
Arrays.sort(order, Comparator.comparingDouble(keys::get));
```

### 6. Expose the parameters the papers use

* `pOfMutation` is the complement of the paper's random mating probability `rmp`:
  `pOfMutation = 0.1` means cross-task crossover 90% of the time, whereas the paper uses
  `rmp = 0.3`. Rename it to `rmp` with the paper's semantics.
* `ITERATIONS = 1000` is a constant; make the generation count (or an evaluation budget)
  a parameter of `run`.
* Accept a random seed so experiments are reproducible; the task windows also use
  `Math.random()`.
* Replace per-generation `System.out` printing with a progress callback or a logger, and
  move the instance list and parameters of `MFEAlgorithmRun` to command-line arguments.

### 7. Naming and API

* `checkIndividualVail` returns `true` for an *infeasible* chromosome; `Vail` is a typo for
  valid. Rename to `isInfeasible`/`repair` (or `isFeasible` with inverted logic).
* `Individual` exposes mutable internal lists; a record-like immutable individual with a
  `withRanks(...)` style update avoids the aliasing bugs above.
* Use `double[]` instead of `List<Double>` for chromosomes and costs; the current code
  boxes every gene and copies the list on every decode. `Knapsack.checkIndividualVail`,
  `getWeight` and `computeFitness` each decode the chromosome again.

### 8. Broader TSPLIB support and experiment tooling

* Support `EXPLICIT` edge-weight matrices (`FULL_MATRIX`, `UPPER_ROW`, `LOWER_DIAG_ROW`,
  ...), `ATT` and `CEIL_2D` so that all 144 shipped instances are usable.
* Add a small experiment runner: multiple seeds, CSV output of best-so-far per generation,
  and a comparison against single-task runs of the same GA, which is the experiment the
  papers use to show the benefit of multitasking.
* If a mature GA toolbox is wanted, MOEA Framework 5.1 (the latest release of the
  dependency the original POM declared but never used) provides SBX, PM, permutation
  operators and TSPLIB readers; otherwise keep the project dependency-free as it is now.
