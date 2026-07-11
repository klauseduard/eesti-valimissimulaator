## 2026-07-11

### Added

  * New feature: JavaFX bar chart of the final seat distribution, shown only
  with the `--gui` flag so batch runs stay headless (#1, #2).

  * New feature: per-run polling error sampled from a normal distribution and
  added to every party's district support, tunable with `--sigma=` (#4).
  Previously party-level results were identical on every run.

  * New feature: votes are now split between a party's district candidates
  with an explicit, tunable front-load model (weight `decay^(i-1)` at list
  position i, `--decay=` flag) instead of an accidental order-dependent
  geometric decay (#3).

### Fixed

  * Apply the 5% national election threshold to district list and
  compensation mandates (#5).

  * Require at least 10% of the district's simple quota for a candidate to
  win a district list mandate (#6).

  * Mandate conservation: the 0.75 round-up rule can no longer over-allocate
  a district, and mandates a party cannot fill flow into the compensation
  round instead of vanishing; the total is always 101 (#9).

  * Validate national-list position matching: unmatched candidates now abort
  the run with a clear error instead of silently jumping the compensation
  queue (independent candidates are exempt) (#7).

  * Break compensation-mandate ties per the law's intent instead of
  arbitrarily; parties with no remaining candidates no longer crash the
  compensation round (#8).

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
