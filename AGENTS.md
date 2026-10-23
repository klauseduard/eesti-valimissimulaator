> **Note on AI authorship.** This file was written by AI (Claude by Anthropic, in Claude Code) in October 2026 under the direction of Klaus-Eduard Runnel, who set the conventions it records.

# Conventions for coding agents

This repository is the Riigikogu election simulator: a Kotlin Multiplatform allocation engine (the real
seat rules, RKVS § 62), a command-line program, and a Svelte web page in `web/` that uses the engine
compiled to JavaScript.

## Code and language

- Identifiers, file names and code comments are in Estonian throughout (Kotlin and the web code). Keep
  that; do not introduce English names into existing modules. User-facing texts exist in Estonian and
  English (`web/src/lib/tekstid.ts` and the methodology page).
- Documents written by AI carry an authorship note at the top that names the parts written by the author
  (see README.md and CHANGELOG.md). The CHANGELOG gets an entry under "Unreleased" for every change.
- Several files have CRLF line endings (README.md, CHANGELOG.md, parts of `src/`): preserve the line
  endings of the file you edit.
- Writing style for prose follows the author's global instructions: conclusion first, plain sentences,
  no filler phrases.

## Correctness

- The engine must reproduce the official 2023 result exactly from the actual votes; the golden tests
  (`KuldsedFailidTest`, `Rk2023Test`, `KorduvKuldneTest`) guard this. A change that alters them needs an
  explanation in the pull request.
- Anything that is a model rather than the law (poll error, candidate vote split, backtests) is
  documented in `docs/` with its numbers and limits.
- Never commit the candidates' personal data file (`ELECTION_CANDIDATES.xml`) or other personal data; the
  committed inputs are the public candidate lists and the official result.

## Public interface for other programs

- The JavaScript build (`./gradlew jsBrowserProductionLibraryDistribution`, output in
  `build/dist/js/productionLibrary/`) is consumed by separate game projects that vendor it. The exported
  API in `src/jsMain/kotlin/Veebiliides.kt` is therefore a public interface: add rather than rename, and
  note changes in the CHANGELOG.

## Workflow

- Work on a branch, open a pull request, let CI (`.github/workflows/sait.yml`) pass, then fast-forward
  merge to `main` and push; the push deploys the page to GitHub Pages.
- Before any push all suites pass: `./gradlew allTests jsBrowserProductionLibraryDistribution`, then in
  `web/`: `npm run check`, `npm test`, `npm run build`, `npm run e2e`. A local pre-push hook enforces this.
- Commit and push in separate commands (the hook checks the tree before the command runs).
