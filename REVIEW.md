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

## Measured results

Default configuration (5 × 100-item knapsacks + `kroA100`, population 50, 100 offspring per
generation, 1000 generations). Knapsack optima computed by dynamic programming; the
`kroA100` optimum is the published 21,282. Single runs; "fixed" is the first revision with
the selection bugs fixed, "current" the revision that implements the suggestions below
(seed 42, rmp 0.3, one 2-opt sweep per TSP offspring).

| Task | Optimum | Original | Fixed | Current | Current, no 2-opt |
| --- | ---: | ---: | ---: | ---: | ---: |
| knapsack s000 | 32,066 | 26,107 | 28,860 | **32,066** | 31,464 |
| knapsack s001 | 35,046 | 21,742 | 26,372 | 34,958 | 34,635 |
| knapsack s002 | 33,615 | 21,215 | 27,858 | 33,481 | 33,158 |
| knapsack s003 | 37,517 | 23,169 | 26,207 | 37,498 | 37,073 |
| knapsack s004 | 43,873 | 25,874 | 28,787 | 42,599 | **43,873** |
| TSP kroA100 (lower is better) | 21,282 | 137,273 | 58,900 | 21,546 | 59,642 |
| wall time | | 60 s | 10 s | 1.8 s | 1.1 s |

A random `kroA100` tour is about 170,000 long, so the original run never left the random
regime. The "no 2-opt" column shows that SBX, polynomial mutation and the corrected
ranking account for the knapsack results and roughly halve the TSP gap on their own; the
bounded 2-opt pass closes the rest. Other seeds give the same picture (seed 7: all five
knapsacks within 1% of optimal, kroA100 = 21,376).

## Changes made in the first revision

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

## Improvements implemented since the review

The first six suggestions of the original review were implemented in the second revision (the "Current" column above).

1. **Selective evaluation and one ranking per generation.** Offspring are evaluated on
   their skill-factor task only and keep `Individual.NOT_EVALUATED` elsewhere. Parents and
   offspring are ranked together once per generation (`Population.rank`, O(T · M log M));
   unevaluated tasks rank last so they can never become the skill factor. Ties in scalar
   fitness are broken by a seeded shuffle, so offspring and parents compete fairly.
2. **Operators suited to random keys, plus local search.** One-point crossover and
   single-gene resampling were replaced by bounded SBX (η = 2) and polynomial mutation
   (η = 5, per-gene rate 1 / length), the operators of Gupta et al. TSP offspring receive a
   bounded first-improvement 2-opt sweep that is written back into the genes
   (`TSP.localSearch`; `new TSP(file, sweeps)` or `--local-search` controls it, 0 disables).
3. **Truncation instead of window folding.** A task of dimension `D` reads the first `D`
   genes. `Task.getStride`, the per-task random windows and the `stride` side effect are
   gone; decoding is deterministic and a chromosome longer than the task evaluates exactly
   like its first `D` genes (tested).
4. **Immutable elites that survive restarts.** The best of every task is kept as an
   independent copy (`Individual.copy()`), re-inserted after a restart, and `Individual`
   no longer exposes mutable internals. A test checks that the best known cost never
   regresses across 200 generations with restarts every 5.
5. **`TSP.decode` without `indexOf`.** Cities are sorted by key through an index array;
   equal keys no longer produce a city visited twice (tested).
6. **The paper's parameters.** `rmp` replaces the inverted `pOfMutation`; the generation
   count, number of pairs, mutation rate and seed are parameters; progress goes to a
   `ProgressListener` instead of `System.out`; `MFEAlgorithmRun` takes instances and all
   parameters from the command line. Runs are reproducible from the seed (tested).

Also done along the way: `checkIndividualVail`/`makeIndividualVail` renamed to
`isInfeasible`/`repair`; knapsack selection uses a strict `> 0.5` threshold so that a gene
at the upper bound 1.0 can still be repaired; the knapsack reads weights and values once
instead of decoding the chromosome three times per evaluation.

## Remaining suggestions (not implemented)

### Naming and API

* `Individual` genes are still `List<Double>`; `double[]` would avoid boxing on every
  operator and evaluation. The run time is already small (about 2 s for the default
  configuration), so this is a clean-up rather than a need.
* `Task.dimension`/`capacity` are protected fields; a constructor-injected immutable
  design would be cleaner.

### Broader TSPLIB support and experiment tooling

* Support `EXPLICIT` edge-weight matrices (`FULL_MATRIX`, `UPPER_ROW`, `LOWER_DIAG_ROW`,
  ...), `ATT` and `CEIL_2D` so that all 144 shipped instances are usable (23 are rejected
  today).
* Add a small experiment runner: multiple seeds, CSV output of best-so-far per generation,
  and a comparison against single-task runs of the same GA, which is the experiment the
  papers use to show the benefit of multitasking. With the seed parameter and the
  progress listener in place this is now a thin layer.
* The permutation-based unified representation of Yuan et al. (2016), with ordered
  crossover and swap mutation, remains an alternative worth comparing against the random-key
  representation for larger TSP instances.
* If a mature GA toolbox is wanted, MOEA Framework 5.1 (the latest release of the
  dependency the original POM declared but never used) provides SBX, PM, permutation
  operators and TSPLIB readers; otherwise keep the project dependency-free as it is now.
