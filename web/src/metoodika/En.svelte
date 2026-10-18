<script lang="ts">
  import { arv } from '../lib/i18n.svelte';
  import { mudeliArvud as m } from '../lib/mudel';
  import { DOKUMENDID as d } from './lingid';
</script>

<p class="märkus" data-testid="ai-märkus">
  Note on AI authorship. The text of this page was written by AI (Claude by Anthropic, in Claude Code) in October
  2026. The error sizes and the numbers of the candidate split come from files that the program generates.
</p>

<h1>Methodology</h1>

<p>
  The simulator does not forecast the election result. It allocates the seats under the Riigikogu Election Act as if
  the poll support were the election result, and it repeats this 1000 times with a random poll error. The main results
  are therefore ranges and probabilities: the 90% range and the median of a party's seats, and how often a party or a
  coalition passes a given boundary. The hemicycle shows the average composition over the runs; it is not the result
  of any single run. All data are those of the 2023 election, so the simulator currently describes the 2023 election
  as it looked from the last polls.
</p>

<h2>Data</h2>
<p>
  The candidates and their order in the district lists, the number of seats per district and the number of valid
  votes per district come from the official data of the 2023 election. Party support by district comes from the
  Norstat data on reitingud.ee (eight parties) and from the March poll of Kantar Emor (six parties). The poll aggregate
  and the estimate of pollster accuracy use national polls from 2011 to 2023, taken from Wikipedia. Parties without
  support in the data and independent candidates receive no votes.
</p>

<h2>Poll aggregate</h2>
<p>
  The default preset is a poll aggregate that combines the latest polls of all pollsters, so that repeated polls by one
  pollster do not count as independent polls. For each pollster, the program computes a weighted mean of its polls in
  the 60 days before the date. A poll's weight grows with its sample size and halves every 14 days, and a poll counts
  as available 3 days after its fieldwork ended. Each pollster then receives an equal weight. Weights by the past
  accuracy of each pollster were tested, but they were not more accurate in the backtest, so they are not used. The
  aggregate gives the national support. The split by district comes from the Norstat data of 27 February, scaled so
  that each party's national support equals the aggregate. The model choices and the criteria for adopting them were
  recorded before the backtest ran (<a href={d.koond}>description</a>,
  <a href={d.tagasitest}>backtest report</a>).
</p>

<h2>Poll error</h2>
<p>
  Each run multiplies a party's support by a random factor with a mean of 1. The factor has two parts. The national
  part is the same for a party in all districts, because a poll usually errs in the same direction for a party
  everywhere. The district part is drawn separately in each district. Each district's party support is then scaled
  back to its original total, because underestimating one party means overestimating the others.
</p>
<p>
  The size of the national part is estimated from the polls and results of four elections (2011, 2015, 2019 and 2023).
  It consists of the pollster's house effect, which is the
  pollster's systematic lean, and an error common to all pollsters (typically ±{m.ühineViga}%). The typical error is
  ±{m.vigaKoond}% of a party's support for the aggregate, ±{m.vigaNorstat}% for a single Norstat poll and
  ±{m.vigaEmor}% for the Emor poll. The district part (±{m.vigaRingkond}%) is measured on the 2023 election, because
  district results exist in open data only for that year. The error applies to polls taken up to 14 days before the
  election. The error for longer horizons was unreliable in the backtest, because one event decided most of it: the
  surge of Vabaerakond in 2015.
</p>

<h2>Votes to candidates</h2>
<p>
  A party's votes in a district are split among its candidates at random, but a candidate's expected share depends on
  the position in the district list. In a list of ten, the first candidate gets {m.esimene}% of the party's votes on
  average. If that candidate is in the top five of the party's national list, the expected share is
  {m.esimeneEsiviisikus}%. In each run the shares vary around the expectation by a Dirichlet distribution. The
  parameters of the split are fitted to the candidate votes of 2023.
</p>
<p>
  In a test where each district in turn was left out of the fit, this split named on average
  {arv(m.kalibreeritud, 1)} of the {m.kohti} candidates elected at the district level correctly. The simulator's
  original simple split, in which the first candidate gets half of the party's votes on average, named
  {arv(m.lihtne, 1)} correctly. The simple split can be chosen next to the candidate table. The model and the test were
  recorded before the test ran (<a href={d.kandidaadid}>description</a>, <a href={d.kandidaaditest}>test report</a>).
</p>

<h2>Seat allocation</h2>
<p>
  The seats are allocated in three stages under §&nbsp;62 of the Riigikogu Election Act. The simple quota is the number of
  valid votes in a district divided by the number of seats in the district.
</p>
<ol>
  <li>
    A candidate whose votes reach the simple quota is elected with a personal mandate. This applies also to independent
    candidates and to candidates of parties below the 5% threshold.
  </li>
  <li>
    District mandates go to parties whose national support is at least 5%. A party receives as many seats as its votes
    contain simple quotas, and one more if the remainder is at least 0.75 of the quota. The seats go to the party's
    candidates with the most votes who have at least 10% of the simple quota. Seats left unfilled become compensation
    mandates.
  </li>
  <li>
    Compensation mandates are divided among the parties above the 5% threshold by a modified d'Hondt method with the
    divisors 1, 2<sup>0.9</sup>, 3<sup>0.9</sup> and so on, taking into account the district mandates a party already
    has. A seat goes to the party's highest-placed candidate on the national list who is not yet elected and has at
    least 5% of the simple quota.
  </li>
</ol>
<p>
  In a tie between candidates, the order of the national list decides. In a tie between parties, the seat goes to the
  party whose candidates are further back in the combined list of candidates (§&nbsp;62 subsection 5 of the Act). The
  sections "How the seats in a district were allocated" and "Compensation mandates" of the application show each step
  for one run.
</p>

<h2>Repeated runs</h2>
<p>
  The application runs 1000 repetitions in the background. Each run first moves the support by a poll error. It then
  computes a party's votes in a district from the support percentage and the 2023 valid votes, splits the votes among
  the candidates and allocates the seats. From the runs the application shows the following results:
</p>
<ul>
  <li>the 90% range of a party's seats (5th to 95th percentile), the median and the distribution over the runs;</li>
  <li>the share of runs in which a party won at least one seat;</li>
  <li>
    how often a coalition has at least 51 seats, which is only a count of seats and not an assessment of whether the
    coalition is politically possible;
  </li>
  <li>how often each candidate was elected.</li>
</ul>
<p>
  The example run is a single run with the chosen seed and without poll error. It explains the steps of the seat
  allocation, but it is not the most probable result.
</p>

<h2>Checks</h2>
<p>
  A test checks the allocation rules: with the actual candidate votes of 2023 it reproduces the official result
  exactly, with the same 101 candidates and the same mandate types. This checks the rules, not the uncertainty.
</p>
<p>
  A backtest checks the national part of the poll error: for each election, the parameters were estimated only from
  the other elections. Three days before the election, the actual vote share fell inside the aggregate's 90% interval
  in 22 of 23 cases (parties with a result of at least 3% in the elections of 2011 to 2023). A
  second check compares the spread of the simulation with the spread from the backtest and requires them to differ by
  at most 15%.
</p>
<p>
  All parts of the simulation, the national and the district error as well as the candidate split, were estimated on
  data that include the 2023 election that the simulator simulates. For this reason, the fact that the actual 2023 seats of all six parties fell inside the simulated 90% ranges does not
  show that the model is calibrated. It is only a check of plausibility.
</p>

<h2>What is not modelled</h2>
<ul>
  <li>
    Turnout: the number of valid votes in each district is that of 2023. Changes in turnout, and differences between
    parties in turnout, are not modelled, although the simple quota depends on turnout directly.
  </li>
  <li>The personal popularity of candidates: the split of votes depends only on the position in the list.</li>
  <li>
    New elections: the seats per district and the candidate lists are those of 2023. Seats are divided among the
    districts anew for each election (§&nbsp;7 of the Act), so the simulator does not yet support the 2027 election.
  </li>
  <li>Polls taken more than 14 days before the election: there is no reliable error estimate for them.</li>
  <li>
    Correlation between the errors of parties: voters move between similar parties, but a model that estimated this
    correlation was not better in the backtest. The error of each party is therefore drawn separately, and the only
    link between the parties comes from scaling each district's support back to its original total.
  </li>
  <li>
    Undecided respondents: the poll percentages are treated as the support among decided voters; how undecided
    respondents would vote is not modelled.
  </li>
  <li>New and merging parties.</li>
</ul>

<h2>Further information</h2>
<p>
  The <a href={d.readme}>README</a> describes the program and its command line. The allocation rules are in the
  <a href="https://www.riigiteataja.ee/akt/124052024010">Riigikogu Election Act</a>.
</p>
