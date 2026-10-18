> **Note on AI authorship.** This document was written by AI (Claude by
> Anthropic, in Claude Code) in October 2026.

# Poll aggregation and backtest: decisions fixed before the backtest

This file records the choices of the poll aggregation model and the criteria
for adopting its parts. They were written and committed before the first
backtest was run, so the backtest cannot have been tuned to them. Any later
change is listed at the end with its reason.

## Data

- National polls for the Riigikogu elections of 2011–2023 from Wikipedia
  (`src/jvmMain/resources/kusitlused/`), with pollster, fieldwork dates and
  sample size where known. Official national results from the same folder.
- District-level results exist in open data only for 2023, so the district
  part of the error model cannot be re-estimated and stays as measured on
  2023 (in-sample).

## Preparing a poll

1. Each row is renormalized over the listed parties and "others" to 100%.
   Rows whose shares sum to less than 95 or more than 105 before
   normalization are reported.
2. The poll date is the midpoint of the fieldwork; if only one date is known,
   that date.
3. A poll may enter a forecast for date T only if its fieldwork ended at
   least 3 days before T (publication lag).
4. Overlapping fieldwork of the same pollster (for example Norstat's
   four-week windows released weekly) would count the same respondents
   several times. Per pollster, polls are taken from the newest backwards,
   and a poll whose fieldwork overlaps a poll already taken is skipped.
5. Effective sample size: the reported size, capped at 2000, divided by a
   design effect of 1.5. An unknown size counts as 1000.

## Model

On a log scale (the log of a party's share):

1. **Per pollster:** the weighted mean of its polls whose date lies within
   60 days before T, with weights proportional to the effective sample size
   times exp(−age · ln 2 / 14 days), that is a half-life of 14 days. Its
   sampling variance follows from the delta method, (1 − p) / (n · p), with
   p floored at 1%.
2. **Combining pollsters**, two variants:
   - *equal weights* ("one pollster, one vote"): the mean of the pollster
     means;
   - *trust weights*: weights 1 / (sampling variance + the pollster's
     house-effect variance), where the house-effect variance of each
     pollster is estimated from other elections.
   Repeated polls by one pollster reduce only its sampling variance, never
   its house-effect variance.
3. **Error of the aggregate:** the variance of the combined mean plus a
   common error shared by all pollsters (an industry-wide miss, for example
   a late swing).
4. **Variance components** are estimated jointly per forecast horizon from
   the differences d = (pollster mean − log result), over elections and
   parties with a result of at least 3%: the spread between pollsters in one
   election estimates the house-effect variance; the mean difference per
   election and party, minus the pollster part, estimates the common error.
   Each pollster's own house-effect variance is shrunk towards the pooled
   value with a weight equal to 6 observations (about one election). Pollster
   names are normalized (TNS Emor and Emor are Kantar Emor).
5. Pollster-specific bias per party is not used; with so few elections it
   would be noise.

## Backtest

- Leave-one-election-out: the parameters used for election E are estimated
  from the other elections only.
- Forecast horizons: 3, 30 and 90 days before election day.
- Parties scored: those with a result of at least 3% that were polled.
- Methods compared:
  1. the last single Norstat poll (the method the page used so far);
  2. the latest poll of each pollster, equal weights;
  3. the aggregate with equal weights;
  4. the aggregate with trust weights.
- Metrics: mean absolute error in percentage points; coverage of the 90%
  predictive interval, reported as counts (for example 19 of 22); log score
  of the result under the predictive distribution (normal on the log scale).

## Adoption criteria

- **Trust weights** replace equal weights only if their mean absolute error,
  averaged over elections at the 3-day horizon, is lower than that of equal
  weights. Otherwise equal weights are used, and the report says so.
- **The common error** from the leave-one-out estimate replaces the current
  in-sample national error (0.156) if the 90% intervals of the chosen method
  cover between 80% and 98% of the results; otherwise the report says the
  error model failed, and the larger of the two values is used.
- **Correlations between parties' errors** are adopted only if a model with
  estimated correlations gives a better leave-one-out log score than
  independent errors. With four elections this is not expected.
- If a part fails its criterion, the aggregator and the backtest are still
  published, and the report says what failed.

## Changes after the backtest

None yet.
