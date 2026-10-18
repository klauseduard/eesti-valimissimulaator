<script lang="ts">
  // Koalitsioonide enamuse tõenäosus korduvates simulatsioonides (ainult kohtade arv) ja koalitsiooni koostaja.
  import { erakonnaInfo, järjekorraNr } from '../lib/erakonnad';
  import { arv, protsent, t } from '../lib/i18n.svelte';
  import { ENAMUS, enamus, koalitsioonid, type Enamus } from '../lib/koalitsioonid';
  import Vahemikuriba from './Vahemikuriba.svelte';
  import type { Olek } from '../lib/olek';
  import type { Kokkuvõte } from '../lib/protokoll';
  import type { Tulemus } from '../lib/simulaator';

  let {
    tulemus,
    kokkuvõte,
    kokkuvõteKehtib,
    olek,
  }: { tulemus: Tulemus; kokkuvõte: Kokkuvõte | null; kokkuvõteKehtib: boolean; olek: Olek } = $props();

  const NÄITA = 8;

  /** erakonnad ja nende kohad igas korduses; tegelike häältega on üks „kordus" */
  const andmed = $derived.by((): { erakonnad: string[]; mandaadid: number[][] } | null => {
    if (olek.tegelikudHääled) {
      const e = tulemus.erakonnad.filter((x) => x.mandaate > 0);
      return { erakonnad: e.map((x) => x.erakond), mandaadid: e.map((x) => [x.mandaate]) };
    }
    if (!kokkuvõte || kokkuvõte.kordi === 0) return null;
    return { erakonnad: kokkuvõte.erakonnad, mandaadid: kokkuvõte.mandaadid };
  });

  const loetelu = $derived(andmed ? koalitsioonid(andmed.erakonnad, andmed.mandaadid).slice(0, NÄITA) : []);
  const valikud = $derived(
    andmed
      ? andmed.erakonnad
          .map((e, i) => ({ e, i, keskmine: andmed.mandaadid[i].reduce((a, b) => a + b, 0) / andmed.mandaadid[i].length }))
          .filter((x) => x.keskmine >= 0.5)
          .sort((a, b) => järjekorraNr(a.e) - järjekorraNr(b.e))
      : [],
  );

  let valitud = $state<string[]>([]);
  const valituTulemus = $derived(
    andmed && valitud.length > 0 ? enamus(andmed.mandaadid, valitud.map((e) => andmed.erakonnad.indexOf(e)).filter((i) => i >= 0)) : null,
  );

  function lülita(erakond: string) {
    valitud = valitud.includes(erakond) ? valitud.filter((e) => e !== erakond) : [...valitud, erakond];
  }

  /** ühine telg: kõigi näidatud vahemike ja enamuse piiri ümber, viie kaupa ümardatud */
  const telg = $derived.by(() => {
    const kõik = [...loetelu, ...(valituTulemus ? [valituTulemus] : [])];
    const algus = Math.floor(Math.min(ENAMUS - 5, ...kõik.map((k) => k.p05)) / 5) * 5;
    const lõpp = Math.ceil(Math.max(ENAMUS + 5, ...kõik.map((k) => k.p95)) / 5) * 5;
    const jaotised = [];
    for (let v = Math.ceil(algus / 10) * 10; v <= lõpp; v += 10) if (Math.abs(v - ENAMUS) > 3) jaotised.push(v);
    return { algus, lõpp, jaotised };
  });
  const asend = (v: number) => `${((v - telg.algus) / (telg.lõpp - telg.algus)) * 100}%`;

  const vahemik = (e: Enamus) => (e.p05 === e.p95 ? String(e.p05) : `${e.p05}–${e.p95}`);
  const silt = (erakonnad: string[], e: Enamus) =>
    t('koalitsioonid.silt', { koalitsioon: nimi(erakonnad), vahemik: vahemik(e), mediaan: e.mediaan });

  const järjestatud = (erakonnad: string[]) => [...erakonnad].sort((a, b) => järjekorraNr(a) - järjekorraNr(b));
  const nimi = (erakonnad: string[]) => järjestatud(erakonnad).map((e) => erakonnaInfo(e).lühend).join(' + ');
</script>

<h2 id="koalitsioonid-pealkiri">{t('koalitsioonid.pealkiri')}</h2>
<p class="vaike vaigne">{t('koalitsioonid.selgitus', { enamus: ENAMUS })}</p>

{#if !andmed}
  <p class="vaigne">{t('parlament.alus.arvutan')}</p>
{:else}
  <div class="tabel" class:uueneb={!olek.tegelikudHääled && !kokkuvõteKehtib}>
    <div class="rida päis vaike vaigne" aria-hidden="true">
      <span>{t('koalitsioonid.veerg.koalitsioon')}</span>
      <span class="telg">
        {#each telg.jaotised as j (j)}<span class="jaotis" style:left={asend(j)}>{j}</span>{/each}
        <span class="jaotis enamus" style:left={asend(ENAMUS - 0.5)}>{ENAMUS}</span>
      </span>
      <span class="arv">{t('koalitsioonid.veerg.enamus')}</span>
    </div>
    <ol data-testid="koalitsioonid">
      {#each loetelu as k (k.erakonnad.join('+'))}
        <li class="rida">
          <span class="liikmed">
            {#each järjestatud(k.erakonnad) as e (e)}<span class="varv" style:background={erakonnaInfo(e).värv}></span>{/each}
            <span class="nimi">{nimi(k.erakonnad)}</span>
          </span>
          <span class="strip">
            <Vahemikuriba p05={k.p05} p95={k.p95} mediaan={k.mediaan} algus={telg.algus} lõpp={telg.lõpp} piir={ENAMUS} silt={silt(k.erakonnad, k)} />
            <span class="vahemik-arv vaike vaigne">{vahemik(k)}</span>
          </span>
          <span class="arv tõenäosus">{protsent(k.tõenäosus * 100, 0)}</span>
        </li>
      {/each}
    </ol>
  </div>

  <fieldset class="koostaja">
    <legend class="vaike">{t('koalitsioonid.koosta')}</legend>
    <div class="valikud">
      {#each valikud as v (v.e)}
        <label class="vaike" class:valitud={valitud.includes(v.e)}>
          <input type="checkbox" checked={valitud.includes(v.e)} onchange={() => lülita(v.e)} data-testid="koalitsioon-{erakonnaInfo(v.e).lühend}" />
          <span class="varv" style:background={erakonnaInfo(v.e).värv}></span>{erakonnaInfo(v.e).lühend}
        </label>
      {/each}
    </div>
    <div class="vaike valitud-tulemus" data-testid="valitud-koalitsioon">
      {#if valituTulemus}
        <p>
          {t('koalitsioonid.valitud', {
            koalitsioon: nimi(valitud),
            vahemik: vahemik(valituTulemus),
            mediaan: valituTulemus.mediaan,
            tõenäosus: protsent(valituTulemus.tõenäosus * 100, 0),
          })}
        </p>
        <Vahemikuriba
          p05={valituTulemus.p05}
          p95={valituTulemus.p95}
          mediaan={valituTulemus.mediaan}
          algus={telg.algus}
          lõpp={telg.lõpp}
          piir={ENAMUS}
          silt={silt(valitud, valituTulemus)}
        />
      {:else}
        <p class="vaigne">{t('koalitsioonid.vali')}</p>
      {/if}
    </div>
  </fieldset>

  <details class="vaike">
    <summary>{t('meetod.kuidas')}</summary>
    <p class="vaigne">{t('koalitsioonid.meetod', { arv: NÄITA })}</p>
  </details>
{/if}

<style>
  .tabel {
    transition: opacity 0.2s;
    margin: 0.6rem 0 0.9rem;
  }

  .uueneb {
    opacity: 0.5;
  }

  ol {
    list-style: none;
    padding: 0;
    margin: 0;
  }

  .rida {
    display: grid;
    grid-template-columns: minmax(9.5rem, 13rem) minmax(0, 1fr) 3rem;
    gap: 0.2rem 0.9rem;
    align-items: center;
    padding: 0.3rem 0;
    border-bottom: 1px solid var(--pind-3);
  }

  .rida.päis {
    border-bottom: 1px solid var(--joon);
    padding-bottom: 0.2rem;
  }

  .telg {
    position: relative;
    height: 1.2em;
  }

  .jaotis {
    position: absolute;
    transform: translateX(-50%);
    font-variant-numeric: tabular-nums;
  }

  .jaotis.enamus {
    color: var(--rohk);
    font-weight: 600;
  }

  .liikmed {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.15rem;
    font-weight: 600;
    font-size: 0.88rem;
    min-width: 0;
  }

  .liikmed .varv {
    margin-right: 0;
  }

  .nimi {
    margin-left: 0.3rem;
  }

  .strip {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 3.4rem;
    gap: 0.5rem;
    align-items: center;
  }

  .vahemik-arv {
    font-variant-numeric: tabular-nums;
    text-align: right;
  }

  .päis .telg {
    margin-right: 3.9rem;
  }

  .arv {
    text-align: right;
    font-variant-numeric: tabular-nums;
  }

  .tõenäosus {
    font-weight: 600;
  }

  .koostaja {
    border: 1px solid var(--joon);
    border-radius: 8px;
    padding: 0.5rem 0.75rem 0.6rem;
    margin: 0 0 0.4rem;
  }

  .valikud {
    display: flex;
    flex-wrap: wrap;
    gap: 0.35rem;
  }

  .valikud label {
    display: flex;
    align-items: center;
    gap: 0.3rem;
    padding: 0.15rem 0.55rem 0.15rem 0.35rem;
    border: 1px solid var(--joon);
    border-radius: 999px;
    cursor: pointer;
  }

  .valikud label.valitud {
    border-color: var(--rohk);
    background: var(--rohk-pind);
  }

  .valikud input {
    margin: 0;
  }

  .valitud-tulemus p {
    margin: 0.5rem 0 0.3rem;
  }

  summary {
    cursor: pointer;
    color: var(--tekst-2);
  }

  details p {
    margin: 0.4rem 0 0;
  }

  /* kitsas kaart: nimi ja tõenäosus ühel real, riba nende all kogu laiuses */
  @container kaart (max-width: 520px) {
    .rida {
      grid-template-columns: minmax(0, 1fr) 3rem;
    }

    .rida .strip {
      grid-column: 1 / -1;
      grid-row: 2;
    }

    .rida.päis .telg {
      grid-column: 1 / -1;
      grid-row: 2;
    }
  }
</style>
