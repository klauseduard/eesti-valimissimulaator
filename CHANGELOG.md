## Unreleased

### Changed

  * Repeated simulations now model poll uncertainty. Each run multiplies every
  party's support by a random factor with a national part shared by all
  districts and a smaller district part, then rescales each district to its
  original total. The spread (0.156 nationally, 0.251 by district, on a log
  scale) is the measured error of the last 2023 polls against the result; a
  test recomputes it from the data. `--ilma-veata` and a switch on the web page
  turn it off. Single runs and the web page's example run use the poll exactly.

  * The web page shows each party's average seats, its 90% range, the share of
  runs with at least one seat and a small histogram of the seat counts. The
  parliament diagram distributes the averages to 101 seats by the largest
  remainder method, and the map shows average seats per district.

  * The files in `examples/` were regenerated with the poll error.

## Unreleased

### Changed

  * The web page's parliament diagram, seat table and map now show the most
  common complete outcome of the 1000 Monte Carlo runs, with its share of the
  runs, instead of a single random run. The explainer, the compensation table
  and the candidates' votes are labelled as an example run with the chosen seed.
  While the runs update, the previous result is dimmed. Repeated simulations
  now also record the candidates elected per district.

  * The project is now a Kotlin Multiplatform build (Gradle 9.8.1, Kotlin
  2.4.21, JDK 21). Allocation, simulation and the data parsers are in
  `src/commonMain` and compile to the JVM and to JavaScript; the command-line
  program is in `src/jvmMain` and behaves as before. All Kotlin tests run on
  the JVM and on Node (`./gradlew allTests`).

  * The allocation records its steps (quotas, the 0.75 rule, candidates below
  10% of the quota, comparison figures, tie-breaks and the fallback rule), so
  the web page can explain how each seat was won.

### Added

  * A web page in `web/` (Svelte, TypeScript, Vite) that runs the same Kotlin
  code in the browser: sliders and a district table for party support, a
  parliament diagram with Monte Carlo ranges from 1000 runs in a Web Worker,
  a district map, a step-by-step explanation of § 62 for each district and for
  the compensation round, and candidate frequencies. It is in Estonian and
  English, and the URL stores the whole state. A GitHub Actions workflow
  builds, tests and deploys it to GitHub Pages.

  * District outlines built from Maa- ja Ruumiamet boundary data (`kaart/`).

  * The official 2023 result as compact JSON (`./gradlew ekspordiRk2023`) and
  the poll presets for the web page (`./gradlew veebiandmed`).

## 2026-10-18 (simulation inputs)

### Changed

  * Votes are simulated from the official 2023 valid votes per district
  (previously the 2019 turnout), and the Norstat data now gives votes to all
  eight parties in the poll, including Erakond Eestimaa Rohelised and Erakond
  Parempoolsed. District names follow § 6 of the Riigikogu Election Act.

  * `run-n-times.sh` and `run-emor-n-times.sh` run all repetitions in one
  process instead of starting Gradle and sleeping one second per run.

  * The files in `examples/` were regenerated with 1000 runs and seed 1.
  `examples/probabilities.txt` cannot be regenerated, because its ERR data
  is no longer in the program; it now carries a note saying so.

### Added

  * `--runs=N` repeats the simulation and writes how often each candidate
  was elected to `runs/<timestamp>-sagedused.txt`.

  * `--reitingud=FILE` selects the Norstat ratings file.

  * Unknown command-line arguments print a usage message.

### Fixed

  * The program creates `runs/` itself instead of crashing.

  * A party without candidates in a district no longer crashes the vote split.

## 2026-10-18 (allocation rules)

### Changed

  * Seat allocation now follows the remaining rules of the Riigikogu Election
  Act: personal mandates for any candidate with at least the simple quota,
  including independents; the 5% national threshold for district and
  compensation mandates; at least 10% of the simple quota for a district
  mandate; at least 5% of the simple quota for a compensation mandate, with
  the fallback to the candidate with the highest share of the quota. District
  mandates without an eligible candidate move to the compensation pool. With
  the official 2023 votes, the program now reproduces all 101 official seats.
  Simulated results change accordingly; the golden files were regenerated.

  * Tie-breaks follow § 62 of the Riigikogu Election Act: equal votes for a
  district mandate go to the candidate higher in the national list (lg 3);
  equal comparison figures go to the party further back in the combined
  district list, which is ordered by registration number (lg 5, § 33 lg 2);
  equal quota shares in the fallback rule go to the candidate higher in the
  list (lg 7). A party whose list is exhausted takes no further compensation
  mandates (lg 8). `Kandidaat` now carries the registration number as its last
  field, so the files in `runs/` gain a `regNr` field at the end of each line.

  * Seat allocation (`jaotaMandaadid`) is now separate from vote simulation
  (`simuleeriValimised`). It takes candidate votes and valid votes per district
  as input and no longer reads or changes global state. Independent candidates
  are no longer treated as a party list.

### Added

  * `--seed=N` command-line option for reproducible runs.

  * Tests: unit tests for the quota and divisor rules, golden files that
  compare seeded simulation output with the output before the refactor, and a
  regression test against the official 2023 result
  (https://opendata.valimised.ee/api/RK_2023/RESULTS.xml).

## 2023-03-04

### Added

  * Fix: make sure no leftover votes remain when dividing party votes between
  candidates.

  * New feature: add ability to pass command-line instruction to use Emor's
  poll data instead of the default Norstat data. (Emor's data is hardcoded
  in a Kotlin file.)

  * Fix: examples/probabilities-2023-03-03.txt [updated with correct
  run data]

## 2023-03-03

### Added

  * New data: pollster data from reitingud.ee --
  src/main/resources/reitingud-2023-03-03.json

  * New calculations: examples/probabilities-2023-03-03.txt [wrong run data though!]



## 2023-03-01

### Added

  * New data: pollster data from reitingud.ee --
  src/main/resources/reitingud-2023-02-27.json

  * New calculations: examples/probabilities-2023-02-27.txt



## 2023-02-23

### Added

  * New feature: Party ratings are now obtained from a JSON file that is derived
  from the output of `reitingud.ee`. This modification should simplify the process
  of updating ratings in the future. Nonetheless, the initial ratings from ERR
  News are still present in Andmed.kt. It is deemed necessary to have the functionality
  to pass the ratings source to the program as a parameter when running it. However,
  please note that this feature has not yet been implemented. There is a significant
  disparity in the data obtained from various pollsters, which in turn leads to a
  notable divergence in the proportion of seats that each party receives.

  * New data: pollster data from reitingud.ee --
  src/main/resources/reitingud-2023-02-23.json
   
  * New calculations: examples/probabilities-2023-02-23.txt

  * TODO: No time for cleanup this time.
