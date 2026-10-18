## Unreleased

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

## 2026-10-18

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
