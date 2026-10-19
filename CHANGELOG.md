> **Note on AI authorship.** The entries dated 2026 and the "Unreleased"
> entries were written by AI (Claude by Anthropic, in Claude Code). The
> entries dated 2023 were written by the author.

## Unreleased

### Changed

  * The support panel has a district map in its header, pinned while the party
  rows scroll. Pointing at a district column highlights the district on the
  map, and pointing at a district on the map highlights its column.

  * Pointing at a district cell in the support panel shows a small map of
  Estonia with that district highlighted (Tallinn's districts in an enlarged
  inset), together with its name. The poll-error switch now explains the
  error with an example: the error is relative to a party's own support, so
  for the largest party it states the range that holds about two thirds of
  the runs.

  * District support: pointing at or focusing a district cell shows the
  district's name above the cell and in the column header, and highlights the
  column. A new row "Others" shows the share of the valid votes left for
  other lists and independent candidates, nationally and per district, and
  turns red where the parties' support exceeds 100%; it replaces the total
  row.

  * Dark mode has its own party colours. The light colours were too light (REF,
  E200) or too dark (EKRE) for a dark background. The dark set keeps each
  party's hue where possible and passes the palette validator; to keep the
  three blues apart, EKRE is violet, E200 turquoise and KESK a darker green in
  dark mode. A "Simulate again" button with the seed is now also at the top of
  the results panel, which stays in view; the one in the calculation section
  remains.

  * Redesign of the web page. The top of the page is a console: the party
  support panel on the left and the results on the right, which stay in view
  on wide screens while the sliders move. Each party row in the panel shows its
  support in all 12 districts as shaded cells that can be edited in place,
  replacing the district table that was hidden in a narrow sidebar and needed
  sideways scrolling. A sticky bar shows the average composition of the 101
  seats with the majority mark, together with the section buttons. The report
  sections below have numbered rules instead of card boxes. Headings use Source
  Serif 4 and the interface uses Inter, both self-hosted. The compensation and
  district calculation tables have compact forms for narrow screens, and a test
  checks that no table is clipped at 390, 1024 or 1440 px.

  * Second design pass. Coalitions are shown as range strips on a shared seat
  axis: the 90% range of the coalition's seats, the median and the 51-seat
  line, with the majority probability beside them. The closest seats are two
  aligned tables with the votes in a numeric column. A sticky bar of section
  buttons navigates the long page. The candidate section has a segmented
  control for the vote split, a count of near-certain and borderline
  candidates and a filter for borderline ones (elected in 5–95% of runs); on
  phones its table drops the example-run columns. Two phone bugs are fixed:
  the page was 604 px wide on a 390 px screen, and the link to the party
  support inputs replaced the state in the URL and showed a broken-link
  warning.

  * Uncertainty: the seat table leads with the 90% range and an integer median
  instead of a mean with one decimal, which suggested a precision the model does
  not have; the summary sentence and the coalition list follow. A methodology
  page (`metoodika.html`, Estonian and English) states the data, the poll
  aggregate, the error model, the candidate split, the allocation rules, which
  checks are out of sample and which are not, and what is not modelled. Its
  numbers come from the generated model files; `kandidaadimudel.json` now also
  holds the candidate test scores.

  * Candidate votes: a party's votes in a district are now split among its
  candidates by a model calibrated on the 2023 candidate votes. A candidate's
  expected share falls with the position in the district list and is larger
  for the top five of the party's national list: in a list of ten, the first
  candidate gets 34% on average, or 47% if in the national top five. Shares
  vary around the expectation by a Dirichlet distribution. In a leave-one-district-out test,
  fixed beforehand in `docs/kandidaatide-mudel.md`, it named 51.4 of the 75
  candidates elected at the district stage correctly, against 44.9 for the
  previous split (first candidate half of the votes on average, the next half
  of the rest). The previous split stays available: `--jaotus=lihtne` on the
  command line and a selector on the web page. The example files in
  `examples/` were regenerated.

  * File names: the poll download script moved out of the resources (it was
  packed into the JAR) to `skriptid/laadi_kusitlused.py`; files with diacritics
  in their names got ASCII names (`KusitluseViga.kt`, `taustasimulatsioon.ts`,
  `Ylariba.svelte`); the catch-all `Funktsioonid.kt` was split into the files
  that use its functions, and `jagadaArvNiksJuhuslikuksOsaks` is now
  `jagaJuhuslikeksOsadeks`. Identifiers inside the code keep their Estonian names.

  * Interface revision: a summary sentence states the largest party, its range
  and how often it was largest; the parliament diagram marks the majority
  boundary; the seat histograms share one axis so spreads can be compared; a
  new chart shows the average elected per district as stacked bars, since the
  winner-coloured map hides how seats are split; the seed control moved from
  the header to the example run; method texts fold into disclosures; the
  compensation table shows ten rows by default; on phones the results come
  before the inputs; cards are flatter.

  * A poll aggregate combines the latest polls of all pollsters: a time-weighted
  mean per pollster, then equal weights per pollster, so repeated polls by one
  source do not count as independent. It is the web page's default preset
  (3 March 2023). Trust weights by past pollster accuracy were tested and were
  not more accurate, so they are not used.

  * The poll error per preset now comes from a leave-one-election-out backtest
  of pollster accuracy in 2011–2023 (polls from Wikipedia, see
  `docs/tagasitestimine.md`): the error common to all pollsters plus the
  pollster's house effect. The aggregate's national error is about 0.11 against
  0.16 for a single Norstat poll. The district part (0.251) is still measured
  on 2023 only. The model choices and adoption criteria were committed before
  the backtest ran (`docs/kusitluste-koond.md`).

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

  * Coalitions: the share of runs in which each minimal coalition reaches 51
  seats, with its average seat count, and a builder for any combination of
  parties. Only seat counts are considered.

  * Closest seats: how many votes the runner-up lacked for the last compensation
  mandate, and the district seats closest to being won or lost in the example
  run, with a note on when an extra district seat changes a party's total.

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
