<script lang="ts">
  import { erakonnaInfo, järjekorraNr } from '../lib/erakonnad';
  import { arv, protsent, t } from '../lib/i18n.svelte';
  import type { Kokkuvõte } from '../lib/protokoll';
  import type { Tulemus } from '../lib/simulaator';
  import { statistika } from '../lib/statistika';

  let {
    tulemus,
    kokkuvõte,
    mcKäib,
    mcViga,
    kordi,
    tegelikudHääled,
  }: {
    tulemus: Tulemus;
    kokkuvõte: Kokkuvõte | null;
    mcKäib: boolean;
    mcViga: string | null;
    kordi: number;
    tegelikudHääled: boolean;
  } = $props();

  const MANDAATE = 101;
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

  const erakonnad = $derived(
    [...tulemus.erakonnad].sort((a, b) => b.mandaate - a.mandaate || b.hääli - a.hääli),
  );

  /** iga koha erakond püsivas järjekorras */
  const kohtadeErakonnad = $derived(
    [...tulemus.erakonnad]
      .filter((e) => e.mandaate > 0)
      .sort((a, b) => järjekorraNr(a.erakond) - järjekorraNr(b.erakond))
      .flatMap((e) => Array<string>(e.mandaate).fill(e.erakond)),
  );

  function vahemik(erakond: string) {
    if (!kokkuvõte) return null;
    const i = kokkuvõte.erakonnad.indexOf(erakond);
    const mandaadid = i < 0 ? Array<number>(kokkuvõte.kordi).fill(0) : kokkuvõte.mandaadid[i];
    return statistika(mandaadid);
  }

  let esiletõstetud = $state<string | null>(null);
</script>

<h2 id="parlament-pealkiri">{t('parlament.pealkiri')}</h2>

<svg viewBox="-1.06 -1.06 2.12 1.12" role="img" aria-labelledby="parlament-pealkiri parlament-kirjeldus" class="poolring">
  <desc id="parlament-kirjeldus">
    {erakonnad.filter((e) => e.mandaate > 0).map((e) => `${e.erakond}: ${e.mandaate}`).join(', ')}
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
        <th scope="col" class="arv">{t('parlament.kokku')}</th>
        <th scope="col" class="arv" title={t('parlament.vahemik.selgitus', { kordi })}>{t('parlament.vahemik')}</th>
        <th scope="col" class="arv">{t('parlament.hääled')}</th>
        <th scope="col" class="arv" title={t('parlament.ringkonnamandaadid.selgitus')}>{t('parlament.ringkonnamandaadid')}</th>
        <th scope="col" class="arv" title={t('parlament.kompensatsioon.selgitus')}>{t('parlament.kompensatsioon')}</th>
      </tr>
    </thead>
    <tbody>
      {#each erakonnad as e (e.erakond)}
        {@const info = erakonnaInfo(e.erakond)}
        {@const v = vahemik(e.erakond)}
        <tr
          onmouseenter={() => (esiletõstetud = e.mandaate > 0 ? e.erakond : null)}
          onmouseleave={() => (esiletõstetud = null)}
          class:alla={!e.ületasKünnise}
          data-testid="erakond-{info.lühend}"
        >
          <th scope="row">
            <span class="varv" style:background={info.värv}></span>
            <abbr title={e.erakond}>{info.lühend}</abbr>
            {#if !e.ületasKünnise}<span class="märkus vaike vaigne">{t('parlament.allaKünnise')}</span>{/if}
          </th>
          <td class="arv kokku-veerg" data-testid="mandaate-{info.lühend}">{e.mandaate}</td>
          <td class="arv vaigne" title={v ? t('parlament.vahemik.täpsem', { min: v.min, max: v.max, keskmine: arv(v.keskmine, 1) }) : ''}>
            {#if tegelikudHääled}
              –
            {:else if v}
              {v.p05 === v.p95 ? v.p05 : `${v.p05}–${v.p95}`}
            {:else}
              …
            {/if}
          </td>
          <td class="arv">{protsent(e.protsent)}</td>
          <td class="arv">{e.ringkonnamandaate}</td>
          <td class="arv">{e.kompensatsioonimandaate}</td>
        </tr>
      {/each}
    </tbody>
  </table>
</div>

<p class="vaike vaigne mc" aria-live="polite">
  {#if tegelikudHääled}
    {t('mc.tegelikud')}
  {:else if mcViga}
    <span class="viga">{t('mc.viga', { teade: mcViga })}</span>
  {:else if mcKäib}
    <progress max={kordi} value={kokkuvõte?.kordi ?? 0}></progress>
    {t('mc.käib', { tehtud: kokkuvõte?.kordi ?? 0, kordi })}
  {:else if kokkuvõte}
    {t('mc.valmis', { kordi: kokkuvõte.kordi })}
  {/if}
</p>

<style>
  .poolring {
    width: 100%;
    max-width: 520px;
    display: block;
    margin: 0 auto 0.8rem;
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

  .märkus {
    display: block;
    font-weight: 400;
    padding-left: 1.1em;
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
    display: flex;
    gap: 0.5rem;
    align-items: center;
  }

  progress {
    width: 8rem;
  }

  .viga {
    color: var(--viga);
  }
</style>
