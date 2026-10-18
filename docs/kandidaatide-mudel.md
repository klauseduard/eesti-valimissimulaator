> **Note on AI authorship.** This document was written by AI (Claude by
> Anthropic, in Claude Code) in October 2026.

# Candidate vote model: decisions fixed before the test

The simulator splits a party's votes in a district among its candidates. So
far the split is "stick-breaking": the first candidate in the district list
gets a uniform random share of the votes (one half on average), the next a
uniform random share of the rest, and so on. This file fixes a calibrated
alternative and the test that decides between them. It was committed before
the test was run.

## What the 2023 data shows

Mean share of a list's votes by position in the district list, over 105
party lists in 2023 (the registration number order equals the district list
order):

| Position | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|---|---|---|---|---|---|---|---|---|---|---|
| 2023 mean | 0.458 | 0.188 | 0.102 | 0.078 | 0.051 | 0.043 | 0.038 | 0.040 | 0.038 | 0.030 |
| Stick-breaking mean | 0.500 | 0.250 | 0.125 | 0.063 | 0.031 | 0.016 | 0.008 | 0.004 | 0.002 | 0.001 |

The first position is close, but the real tail is much fatter. Candidates in
the first five places of the party's national list got 0.53 at district
position 1, other first-placed candidates 0.42.

## Model

For a list with candidates at district positions p = 1 … n:

- weight w(p) = exp(−b · (p − 1)) + c, multiplied by (1 + d) if the
  candidate is among the first five of the party's national list;
- expected share m(p) = w(p) / Σ w;
- the shares in one run are drawn from a Dirichlet distribution with
  parameters κ · m(p), so κ sets how much a list varies around its
  expectation.

The parameters b, c, d and κ are fitted by maximum likelihood of the
Dirichlet distribution on the observed 2023 shares. Vote counts are smoothed
by adding 0.5 to each candidate, so that zero votes have a finite logarithm.
Lists with one candidate are left out.

## Test

- Leave one district out: the parameters are fitted on the other 11
  districts and applied to the held-out one.
- In the held-out district, each party list keeps its actual 2023 vote total
  and its actual number of seats won at the district stage (personal and
  district mandates). The model splits the votes among the candidates; the
  seats go to the candidates with the most votes who have at least 10% of
  the simple quota. The score is the number of these candidates who were
  actually elected at the district stage.
- 200 runs per district and model; the reported score is the mean over runs,
  summed over the 12 districts. The maximum is 75, the number of seats won at
  the district stage in 2023.
- Compared: stick-breaking (the current method) and the fitted model.

## Adoption criterion

The fitted model becomes the default if its mean score is higher than that
of stick-breaking by at least one candidate. Otherwise stick-breaking stays
the default and the report says so. Either way, both methods remain
available as options.

## Changes after the test

The test ran as fixed above; the report is `docs/kandidaatide-mudeli-test.md`.
The fitted model named 51.4 of 75 candidates correctly, stick-breaking 44.9,
so the fitted model became the default. The parameters fitted on all 12
districts are b = 0.783, c = 0.1527, d = 0.706 and κ = 27.83. The test
improved the score in each of the 12 districts, by 0.1 to 1.0 candidates.

Two details were settled during implementation and did not change the model
or the criterion. Integer votes are obtained from the drawn shares by the
largest remainder method, so that a list's votes add up exactly. The gamma
draws for the Dirichlet distribution use the Marsaglia–Tsang method with the
simulator's seeded random generator, so the command line and the web page give
the same result for the same seed.

The remaining limitation is the one the model was not meant to remove: it uses
only list positions, so a popular candidate low on the list is still rarely
elected in the simulation.
