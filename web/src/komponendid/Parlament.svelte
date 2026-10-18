<script lang="ts">
  import Jaotus from './Jaotus.svelte';
  import Seeme from './Seeme.svelte';
  import { erakonnaInfo, järjekorraNr } from '../lib/erakonnad';
  import { arv, protsent, t } from '../lib/i18n.svelte';
  import type { Olek } from '../lib/olek';
  import type { Kokkuvõte } from '../lib/protokoll';
  import { eelseadistuseViga, type Tulemus } from '../lib/simulaator';
  import { keskmineKoosseis, MANDAATE } from '../lib/koosseis';
  import { statistika } from '../lib/statistika';

  let {
    tulemus,
    kokkuvõte,
    kokkuvõteKehtib,
    mcKäib,
    mcViga,
    kordi,
    olek = $bindable(),
  }: {
    tulemus: Tulemus;
    kokkuvõte: Kokkuvõte | null;
    kokkuvõteKehtib: boolean;
    mcKäib: boolean;
    mcViga: string | null;
    kordi: number;
    olek: Olek;
  } = $props();

  const READ = 6;

  interface Koht {
    x: number;
    y: number;
  }

  /**
   * Kohtade asukohad poolringidel: igas reas on kohti raadiusega võrdeliselt, kohad järjestatakse
   * nurga järgi vasakult paremale, et iga erakonna kohad oleksid üks sektor.
   */
  function kohad(): { kohad: Koht[]; raadius: number } {
    const sisemine = 0.42;
    const raadiused = Array.from({ length: READ }, (_, i) => sisemine + ((1 - sisemine) * i) / (READ - 1));
    const summa = raadiused.reduce((a, b) => a + b, 0);
    const arvud = raadiused.map((r) => Math.round((MANDAATE * r) / summa));
    arvud[READ - 1] += MANDAATE - arvud.reduce((a, b) => a + b, 0);
    const kõik: { x: number; y: number; nurk: number; r: number }[] = [];
    raadiused.forEach((r, i) => {
      for (let k = 0; k < arvud[i]; k++) {
        const nurk = Math.PI - (Math.PI * k) / (arvud[i] - 1);
        kõik.push({ x: r * Math.cos(nurk), y: -r * Math.sin(nurk), nurk, r });
      }
    });
    kõik.sort((a, b) => b.nurk - a.nurk || a.r - b.r);
    return { kohad: kõik, raadius: ((1 - sisemine) / (READ - 1)) * 0.42 };
  }

  const paigutus = kohad();
  const tegelikud = $derived(olek.tegelikudHääled);
  /** eelseadistuse küsitluse viga; tüüpiline suhteline viga protsentides (üks standardhälve) */
  const eelseadistuseVeaga = $derived(eelseadistuseViga(olek.eelseadistus)[0] > 0);
  const tüüpilineViga = $derived(Math.round((Math.exp(eelseadistuseViga(olek.eelseadistus)[0]) - 1) * 100));
  const kordustest = $derived(!tegelikud && kokkuvõte !== null && kokkuvõte.kordi > 0);

  /** erakonna mandaatide arv igas korduses (ka nende erakondade jaoks, kes kordagi mandaati ei saanud) */
  function korduseMandaadid(erakond: string): number[] {
    if (!kokkuvõte) return [];
    const i = kokkuvõte.erakonnad.indexOf(erakond);
    return i < 0 ? Array<number>(kokkuvõte.kordi).fill(0) : kokkuvõte.mandaadid[i];
  }

  const read = $derived(
    tulemus.erakonnad.map((e) => {
      const mandaadid = kordustest ? korduseMandaadid(e.erakond) : [];
      const stat = mandaadid.length > 0 ? statistika(mandaadid) : null;
      return {
        e,
        info: erakonnaInfo(e.erakond),
        mandaadid,
        stat,
        mandaadiga: mandaadid.length > 0 ? mandaadid.filter((m) => m > 0).length / mandaadid.length : null,
      };
    }),
  );

  /** diagrammi kohad: tegelike häältega selle tulemuse järgi, muidu keskmised mandaadid 101 kohaks jagatuna */
  const diagrammiMandaadid = $derived.by((): Map<string, number> | null => {
    if (tegelikud) return new Map(tulemus.erakonnad.map((e) => [e.erakond, e.mandaate]));
    if (!kordustest || !kokkuvõte) return null;
    return keskmineKoosseis(tulemus.erakonnad.map((e) => e.erakond), kokkuvõte);
  });

  const järjestatud = $derived(
    [...read].sort((a, b) =>
      tegelikud
        ? b.e.mandaate - a.e.mandaate || b.e.hääli - a.e.hääli
        : (b.stat?.keskmine ?? 0) - (a.stat?.keskmine ?? 0) || b.e.hääli - a.e.hääli,
    ),
  );

  /** iga koha erakond püsivas järjekorras */
  const kohtadeErakonnad = $derived(
    diagrammiMandaadid === null
      ? []
      : [...diagrammiMandaadid.entries()]
          .filter(([, m]) => m > 0)
          .sort((a, b) => järjekorraNr(a[0]) - järjekorraNr(b[0]))
          .flatMap(([e, m]) => Array<string>(m).fill(e)),
  );

  /** ühine telg jaotuse diagrammidele: suurim mandaatide arv kordustes, ümardatuna 10 ülespoole */
  const telg = $derived(Math.max(10, Math.ceil(Math.max(0, ...read.map((r) => r.stat?.max ?? 0)) / 10) * 10));
  const teljeJaotised = $derived(Array.from({ length: telg / 10 + 1 }, (_, i) => i * 10));

  /** kokkuvõtte lause: suurim erakond (keskmise järgi), selle vahemik ja mediaan ning kui sageli see oli suurim */
  const kokkuvõtteLause = $derived.by((): string | null => {
    if (tegelikud) {
      const e = [...tulemus.erakonnad].sort((a, b) => b.mandaate - a.mandaate)[0];
      return t('parlament.lause.tegelikud', { erakond: e.erakond, mandaate: e.mandaate });
    }
    if (!kordustest || !kokkuvõte || !kokkuvõteKehtib) return null;
    const suurim = [...read].filter((r) => r.stat).sort((a, b) => b.stat!.keskmine - a.stat!.keskmine)[0];
    if (!suurim?.stat) return null;
    const kordi = kokkuvõte.kordi;
    let suurimKordi = 0;
    for (let k = 0; k < kordi; k++) {
      const oma = suurim.mandaadid[k];
      if (kokkuvõte.mandaadid.every((m) => m[k] <= oma)) suurimKordi += 1;
    }
    return t('parlament.lause', {
      erakond: suurim.e.erakond,
      mediaan: suurim.stat.mediaan,
      p05: suurim.stat.p05,
      p95: suurim.stat.p95,
      osakaal: protsent((suurimKordi / kordi) * 100, 0),
    });
  });

  let esiletõstetud = $state<string | null>(null);
</script>

<h2 id="parlament-pealkiri">{t('parlament.pealkiri')}</h2>
<p class="lause" data-testid="kokkuvõtte-lause">{kokkuvõtteLause ?? '\u00a0'}</p>
<p class="vaike vaigne alapealkiri" data-testid="parlamendi-alus">
  {#if tegelikud}
    {t('parlament.alus.tegelikud')}
  {:else if kordustest && kokkuvõte && kokkuvõteKehtib}
    {olek.viga && eelseadistuseVeaga ? t('parlament.alus.veaga', { kordi: kokkuvõte.kordi }) : t('parlament.alus.veata', { kordi: kokkuvõte.kordi })}
  {:else}
    {t('parlament.alus.arvutan')}
  {/if}
</p>

{#if !tegelikud}
  <div class="juhtimine vaike">
    {#if eelseadistuseVeaga}
      <label class="lüliti">
        <input type="checkbox" bind:checked={olek.viga} data-testid="küsitluse-viga" />
        <span>{t('parlament.viga', { protsent: tüüpilineViga })}</span>
      </label>
    {/if}
    <Seeme bind:olek nupuSilt={t('seeme.uuesti')} testid="-ülal" />
  </div>
{/if}

<div class="tulemusala">
<svg
  viewBox="-1.06 -1.14 2.12 1.2"
  role="img"
  aria-labelledby="parlament-pealkiri parlament-kirjeldus"
  class="poolring"
  class:uueneb={!tegelikud && !kokkuvõteKehtib}
>
  <!-- enamuse piir: 51. koht on poolringi keskel -->
  <line x1="0" y1="-1.035" x2="0" y2="-1.075" class="enamuse-joon" />
  <text x="0" y="-1.085" text-anchor="middle" class="enamuse-silt">51</text>
  <desc id="parlament-kirjeldus">
    {järjestatud.filter((r) => (diagrammiMandaadid?.get(r.e.erakond) ?? 0) > 0).map((r) => `${r.e.erakond}: ${diagrammiMandaadid?.get(r.e.erakond)}`).join(', ')}
  </desc>
  {#each paigutus.kohad as koht, i}
    {@const erakond = kohtadeErakonnad[i]}
    <circle
      cx={koht.x}
      cy={koht.y}
      r={paigutus.raadius}
      fill={erakond ? erakonnaInfo(erakond).värv : 'var(--pind-3)'}
      class:tuhm={esiletõstetud !== null && esiletõstetud !== erakond}
    >
      <title>{erakond}</title>
    </circle>
  {/each}
  <text x="0" y="-0.12" text-anchor="middle" class="kokku">{MANDAATE}</text>
  <text x="0" y="-0.02" text-anchor="middle" class="enamus">{t('parlament.enamus')}</text>
</svg>

<div class="tabeli-ümbris kitsas">
  <table class="erakonnad">
    <thead>
      <tr>
        <th scope="col">{t('erakond')}</th>
        {#if tegelikud}
          <th scope="col" class="arv">{t('parlament.kokku')}</th>
          <th scope="col" class="arv" title={t('parlament.ringkonnamandaadid.selgitus')}>{t('parlament.ringkonnamandaadid')}</th>
          <th scope="col" class="arv" title={t('parlament.kompensatsioon.selgitus')}>{t('parlament.kompensatsioon')}</th>
        {:else}
          <th scope="col" class="arv" title={t('parlament.vahemik.selgitus', { kordi })}>{t('parlament.vahemik')}</th>
          <th scope="col" class="arv" title={t('parlament.mediaan.selgitus')}>{t('parlament.mediaan')}</th>
          <th scope="col" class="arv mandaadiga" title={t('parlament.mandaadiga.selgitus')}>{t('parlament.mandaadiga')}</th>
          <th scope="col" class="jaotuse-veerg">
            <span class="peidetud">{t('parlament.jaotus')}</span>
            <svg viewBox="0 0 140 14" preserveAspectRatio="none" aria-hidden="true" class="telg">
              {#each teljeJaotised as j}
                <text x={(j / telg) * 140} y="10" text-anchor={j === 0 ? 'start' : j === telg ? 'end' : 'middle'}>{j}</text>
              {/each}
            </svg>
          </th>
        {/if}
        {#if tegelikud}
          <th scope="col" class="arv" title={t('parlament.hääled.selgitus')}>{t('parlament.hääled')}</th>
        {/if}
      </tr>
    </thead>
    <tbody>
      {#each järjestatud as r (r.e.erakond)}
        <tr
          onmouseenter={() => (esiletõstetud = (diagrammiMandaadid?.get(r.e.erakond) ?? 0) > 0 ? r.e.erakond : null)}
          onmouseleave={() => (esiletõstetud = null)}
          class:alla={tegelikud ? !r.e.ületasKünnise : (r.mandaadiga ?? 0) === 0}
          data-testid="erakond-{r.info.lühend}"
        >
          <th scope="row">
            <span class="varv" style:background={r.info.värv}></span>
            <abbr title={r.e.erakond}>{r.info.lühend}</abbr>
          </th>
          {#if tegelikud}
            <td class="arv kokku-veerg" data-testid="mandaate-{r.info.lühend}">{r.e.mandaate}</td>
            <td class="arv">{r.e.ringkonnamandaate}</td>
            <td class="arv">{r.e.kompensatsioonimandaate}</td>
          {:else}
            <td
              class="arv kokku-veerg"
              title={r.stat ? t('parlament.vahemik.täpsem', { min: r.stat.min, max: r.stat.max }) : ''}
              data-testid="vahemik-{r.info.lühend}"
            >
              {#if r.stat}{r.stat.p05 === r.stat.p95 ? r.stat.p05 : `${r.stat.p05}–${r.stat.p95}`}{:else}…{/if}
            </td>
            <td class="arv vaigne" data-testid="mandaate-{r.info.lühend}">{r.stat ? r.stat.mediaan : '…'}</td>
            <td class="arv vaigne mandaadiga">{r.mandaadiga === null ? '…' : protsent(r.mandaadiga * 100, 0)}</td>
            <td class="jaotuse-veerg">
              {#if r.stat && r.stat.max > 0}
                <Jaotus
                  väärtused={r.mandaadid}
                  värv={r.info.värv}
                  p05={r.stat.p05}
                  p95={r.stat.p95}
                  mediaan={r.stat.mediaan}
                  {telg}
                  silt={t('parlament.jaotus.silt', { erakond: r.info.lühend, min: r.stat.min, max: r.stat.max })}
                />
              {/if}
            </td>
          {/if}
          {#if tegelikud}<td class="arv">{protsent(r.e.protsent)}</td>{/if}
        </tr>
      {/each}
    </tbody>
  </table>
</div>
</div>

<p class="vaike vaigne mc" aria-live="polite">
  {#if tegelikud}
    {t('mc.tegelikud')}
  {:else if mcViga}
    <span class="viga">{t('mc.viga', { teade: mcViga })}</span>
  {:else if mcKäib}
    <progress max={kordi} value={kokkuvõte?.kordi ?? 0}></progress>
    {t('mc.käib', { tehtud: kokkuvõte?.kordi ?? 0, kordi })}
  {/if}
</p>

{#if !tegelikud && kokkuvõte}
  <details class="vaike meetod">
    <summary>{t('meetod.kuidas')}</summary>
    <p>{olek.viga && eelseadistuseVeaga ? t('mc.valmis.veaga', { kordi: kokkuvõte.kordi }) : t('mc.valmis.veata', { kordi: kokkuvõte.kordi })}</p>
  </details>
{/if}

<style>
  /* konsoolis on pealkiri väike silt; tegelik pealkiri on kokkuvõtte lause */
  h2 {
    font: 600 0.72rem/1.2 var(--sans);
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: var(--tekst-3);
    margin-bottom: 0.5rem;
  }

  .juhtimine {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem 1.2rem;
    align-items: center;
    justify-content: space-between;
    margin: 0.2rem 0 0.4rem;
  }

  .lause {
    font-family: var(--serif);
    font-size: 1.3rem;
    line-height: 1.3;
    font-weight: 500;
    margin: 0 0 0.35rem;
    min-height: 1.3em;
    text-wrap: balance;
  }

  .enamuse-joon {
    stroke: var(--tekst-3);
    stroke-width: 0.006;
  }

  .enamuse-silt {
    font-size: 0.05px;
    fill: var(--tekst-3);
  }

  .telg {
    display: block;
    width: var(--jaotuse-laius, 140px);
    height: 14px;
    overflow: visible;
  }

  /* kitsal kaardil: jaotus väiksem ja „Mandaadiga" peidetud (sama info on jaotuses) */
  @container kaart (max-width: 520px) {
    .erakonnad {
      --jaotuse-laius: 96px;
    }

    .mandaadiga {
      display: none;
    }

    .jaotuse-veerg {
      width: 100px;
    }
  }

  /* väga kitsal kaardil jääb alles ainult tabel; vahemik on ka numbrina olemas */
  @container kaart (max-width: 400px) {
    .jaotuse-veerg {
      display: none;
    }
  }

  .telg text {
    font-size: 10px;
    fill: var(--tekst-3);
    font-weight: 400;
  }

  .meetod summary {
    cursor: pointer;
    color: var(--tekst-2);
  }

  .meetod p {
    margin: 0.4rem 0 0;
    color: var(--tekst-2);
  }

  .alapealkiri {
    margin: -0.3rem 0 0.4rem;
    min-height: 1.4em;
  }

  .tulemusala {
    display: grid;
    gap: 1rem 1.5rem;
    align-items: center;
  }

  /* laial kaardil on poolring ja tabel kõrvuti */
  @container kaart (min-width: 680px) {
    .tulemusala {
      grid-template-columns: minmax(260px, 1fr) minmax(0, 1.15fr);
    }
  }

  /* konsoolis on tabelile vähem ruumi: „Mandaadiga" peidetakse (sama info on jaotuses) */
  @container kaart (min-width: 680px) and (max-width: 900px) {
    .mandaadiga {
      display: none;
    }
  }

  .poolring {
    width: 100%;
    max-width: 440px;
    display: block;
    margin: 0 auto;
  }

  .uueneb {
    opacity: 0.45;
  }

  circle {
    stroke: var(--pind);
    stroke-width: 0.008;
    transition: opacity 0.15s, fill 0.3s;
  }

  .tuhm {
    opacity: 0.2;
  }

  .kokku {
    font-size: 0.2px;
    font-weight: 600;
    fill: var(--tekst);
  }

  .enamus {
    font-size: 0.065px;
    fill: var(--tekst-2);
  }

  .erakonnad {
    width: 100%;
    font-size: 0.92rem;
  }

  .erakonnad th[scope='row'] {
    white-space: nowrap;
  }

  .erakonnad abbr {
    text-decoration: none;
    font-weight: 600;
  }

  .lüliti {
    display: flex;
    gap: 0.4rem;
    align-items: center;
    margin-bottom: 0.4rem;
  }

  /* kitsas kaart: pealkirjad võivad murduda, et jaotuse veerg mahuks ära */
  .erakonnad thead th {
    white-space: normal;
    vertical-align: bottom;
  }

  .erakonnad th,
  .erakonnad td {
    padding: 0.3em 0.4em;
  }

  .jaotuse-veerg {
    width: 144px;
    vertical-align: middle;
  }

  .kokku-veerg {
    font-weight: 600;
  }

  tr.alla td,
  tr.alla abbr {
    color: var(--tekst-3);
  }

  .mc {
    margin: 0.6rem 0 0;
    min-height: 1.5em;
  }

  .mc progress {
    vertical-align: middle;
    margin-right: 0.4rem;
  }

  progress {
    width: 8rem;
  }

  .viga {
    color: var(--viga);
  }
</style>
