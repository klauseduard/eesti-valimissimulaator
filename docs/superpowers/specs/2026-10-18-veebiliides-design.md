# Web interface for the election simulator — design

Agreed in conversation on 2026-10-18. The owner reviews the implementation
rather than this document, so this file records the decisions, not a plan.

## Scope

The interface is built in five sub-projects. Version 1 consists of the first
three; the last two follow later.

1. Shared core: the allocation and simulation code compiles to the JVM and to
   JavaScript (Kotlin Multiplatform).
2. Map geometry: outlines of the 12 electoral districts from official boundary
   data, committed as one TopoJSON file (`kaart/`).
3. What-if page: change party support, see seats per party and district, and a
   step-by-step explanation of § 62 of the Riigikogu Election Act.
4. Monte Carlo visualizations: seat histograms, convergence of estimates,
   district-level uncertainty. Version 1 only provides the worker and data.
5. Candidate-level models: alternative ways to split party votes among
   candidates. The list-position model stays as the fallback.

## Decisions

- The app replaces the README as the GitHub Pages home page
  (`klauseduard.github.io/eesti-valimissimulaator/`). The README stays on
  GitHub and is linked from the app.
- The interface is in Estonian and English. Proper names (parties, candidates,
  districts) stay in Estonian.
- Party support is changed with national sliders and an expandable 12×8
  district table. A national slider scales the party's share in every district
  by the same factor; the other parties shrink proportionally in each district,
  so each district total stays constant.
- Approach A: Kotlin Multiplatform core with a TypeScript, Svelte and Vite
  front end. D3 is used for geography and scales only.

## Architecture

- `commonMain`: model, allocation, vote simulation, repeated runs, and parsers
  that take text input. The ratings JSON is parsed with kotlinx.serialization.
- `jvmMain`: the command-line program and file I/O; it behaves as before.
- `commonTest`: rule tests, the 2023 regression test and the golden files run
  on the JVM and on Node.
- A small `@JsExport` facade: load data, allocate seats for given support
  numbers, run N simulations; returns plain objects.
- Monte Carlo runs execute in a Web Worker and report progress in batches.
- The browser reads the same data files as the program (candidate lists,
  ratings JSON, the Emor table), copied into the web build by Gradle. The
  official 2023 result is exported to a compact JSON file by a Gradle task.

## Data and map

- Presets: the three Norstat polls (20.02, 27.02, 03.03.2023), the Emor poll,
  and the official 2023 result. The 2023 preset can use the actual candidate
  votes instead of the random split.
- District outlines: official administrative boundaries of Maa- ja Ruumiamet,
  dissolved into the 12 districts with mapshaper, simplified with shared
  borders preserved. The licence is verified before use; OpenStreetMap is the
  fallback. The page footer names the source.
- Projection close to L-EST97 (Lambert conformal conic). Tallinn's three
  districts get an enlarged inset.

## Page

- Top bar: title, preset, language, link to the README.
- Inputs: national sliders with the 5% threshold marked; district table.
- Results: parliament diagram of 101 seats with Monte Carlo ranges; map
  coloured by the party with most seats or by one party's share.
- Explainer: for a chosen district, the steps of § 62 with clause references;
  the compensation round as a table of comparison figures.
- Candidate table: elected candidates with mandate type and election
  frequency, with a note on the list-position model.
- Every change runs one seeded simulation at once; 1000 runs follow in the
  worker after a short pause. The seed is visible and editable.
- The whole state is stored in the URL fragment, so links reproduce the view.

## Language and errors

- `et.json` and `en.json` with identical keys; a test checks this.
- A district total above 100 % is marked and that district is excluded with a
  message; values are never silently rescaled.
- An unusable shared link falls back to the default preset with a message.
- Failed data loading shows the failure and a reload button.
- Worker errors leave single-run results working.
- The Kotlin core throws on impossible input; the facade returns error objects.

## Tests and deployment

- Kotlin tests on both targets; new tests for the facade and slider scaling.
- Vitest for URL state, dictionaries, table validation and the worker protocol.
- One Playwright test: the official 2023 preset with actual votes shows the
  official totals (Reform 37, EKRE 17, Kesk 16, E200 14, SDE 9, Isamaa 8); a
  slider changes the result; a shared link restores the state.
- The map script checks 12 districts, shared borders and file size.
- A GitHub Actions workflow builds, tests and deploys to Pages on every push
  to `main`; pull requests build and test only. The Pages source must be
  switched from the branch to GitHub Actions. The Google verification moves
  to a `<meta>` tag in `index.html`, and `_config.yml` is removed.

## Changes made during implementation

- The three Norstat files are about 2 MB each, because they hold the whole
  reitingud.ee page. A Gradle task (`./gradlew veebiandmed`) runs the shared
  parsers at build time and writes the four poll presets to a 3.4 KB file,
  `build/veebiandmed/toetused.json`. The candidate files and the 2023 result
  are small and are bundled unchanged.
- A district whose support adds up to more than 100 % is not left out of the
  run, because the allocation needs all 12 districts. Instead the page marks
  the district, explains the problem, and keeps showing the last valid result
  until the input is corrected.
- The facade throws a JavaScript `Error` with an Estonian message instead of
  returning error objects; the page catches it and shows the message.
- The facade keeps the parties in the order of the data source. Vote
  simulation draws random numbers party by party, so this keeps the web page
  and the command-line program identical for the same seed.
- Party colours follow the conventional party hues and were tuned with the
  dataviz validator. In light mode, every pair of the six parliamentary
  parties passes the all-pairs check (normal-vision difference at least 15.8,
  colour-blind difference at least 9.2), which the map needs. The narrower
  dark-mode lightness band cannot hold six such colours in these hue
  families (the best set reached 12.8), so both modes use the light set: on
  the dark surface it keeps every pair distinguishable and misses only the
  lightness band for three colours. Every map district also carries the
  winning party's abbreviation, and every chart has a legend or table.
- After the first review, the parliament diagram, the seat table and the
  map show the most common complete outcome of the Monte Carlo runs, which
  always sums to 101 seats. The single seeded run that the design put in the
  main view had looked like the result of the runs. It now appears only as a
  labelled example run in the explainer, the compensation table and the
  candidates' votes.
- The Monte Carlo runs model poll uncertainty, calibrated on the error of the
  last 2023 polls (0.156 nationally and 0.251 by district, on a log scale).
  With the error included, a single complete seat combination rarely repeats,
  so the main view shows each party's average seats with its 90% range, the
  share of runs with at least one seat and a histogram. The parliament
  diagram distributes the averages to 101 seats by the largest remainder
  method.
