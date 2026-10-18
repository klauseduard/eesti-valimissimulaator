<script lang="ts">
  // Koalitsioonide enamuse tõenäosus korduvates simulatsioonides (ainult kohtade arv) ja koalitsiooni koostaja.
  import { erakonnaInfo, järjekorraNr } from '../lib/erakonnad';
  import { arv, protsent, t } from '../lib/i18n.svelte';
  import { ENAMUS, enamus, koalitsioonid } from '../lib/koalitsioonid';
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

  const järjestatud = (erakonnad: string[]) => [...erakonnad].sort((a, b) => järjekorraNr(a) - järjekorraNr(b));
  const nimi = (erakonnad: string[]) => järjestatud(erakonnad).map((e) => erakonnaInfo(e).lühend).join(' + ');
</script>

<h2 id="koalitsioonid-pealkiri">{t('koalitsioonid.pealkiri')}</h2>
<p class="vaike vaigne">{t('koalitsioonid.selgitus', { enamus: ENAMUS })}</p>

{#if !andmed}
  <p class="vaigne">{t('parlament.alus.arvutan')}</p>
{:else}
  <ol class="loetelu" class:uueneb={!olek.tegelikudHääled && !kokkuvõteKehtib} data-testid="koalitsioonid">
    {#each loetelu as k (k.erakonnad.join('+'))}
      <li>
        <span class="liikmed">
          {#each järjestatud(k.erakonnad) as e (e)}<span class="varv" style:background={erakonnaInfo(e).värv}></span>{/each}
          <span>{nimi(k.erakonnad)}</span>
        </span>
        <span class="riba" aria-hidden="true"><span style:width="{k.tõenäosus * 100}%"></span></span>
        <span class="arv">{protsent(k.tõenäosus * 100, 0)}</span>
        <span class="arv vaigne vaike" title={t('koalitsioonid.keskmine')}>{arv(k.keskmine, 0)}</span>
      </li>
    {/each}
  </ol>

  <fieldset class="koostaja">
    <legend class="vaike">{t('koalitsioonid.koosta')}</legend>
    <div class="valikud">
      {#each valikud as v (v.e)}
        <label class="vaike">
          <input type="checkbox" checked={valitud.includes(v.e)} onchange={() => lülita(v.e)} data-testid="koalitsioon-{erakonnaInfo(v.e).lühend}" />
          <span class="varv" style:background={erakonnaInfo(v.e).värv}></span>{erakonnaInfo(v.e).lühend}
        </label>
      {/each}
    </div>
    <p class="vaike" data-testid="valitud-koalitsioon">
      {#if valituTulemus}
        {t('koalitsioonid.valitud', {
          koalitsioon: nimi(valitud.slice().sort((a, b) => järjekorraNr(a) - järjekorraNr(b))),
          keskmine: arv(valituTulemus.keskmine, 0),
          tõenäosus: protsent(valituTulemus.tõenäosus * 100, 0),
        })}
      {:else}
        <span class="vaigne">{t('koalitsioonid.vali')}</span>
      {/if}
    </p>
  </fieldset>
{/if}

<style>
  .loetelu {
    list-style: none;
    padding: 0;
    margin: 0.4rem 0 0.8rem;
    display: grid;
    gap: 0.3rem;
    transition: opacity 0.2s;
  }

  .uueneb {
    opacity: 0.5;
  }

  li {
    display: grid;
    grid-template-columns: minmax(11rem, 1.6fr) minmax(3rem, 1fr) 3.2rem 2.6rem;
    gap: 0.6rem;
    align-items: center;
  }

  .liikmed {
    display: flex;
    align-items: center;
    gap: 0.15rem;
    font-weight: 600;
    font-size: 0.9rem;
  }

  .liikmed .varv {
    margin-right: 0;
  }

  .liikmed span:last-child {
    margin-left: 0.35rem;
  }

  .riba {
    height: 8px;
    border-radius: 4px;
    background: var(--pind-3);
    overflow: hidden;
  }

  .riba span {
    display: block;
    height: 100%;
    background: var(--tekst-2);
    border-radius: 4px;
  }

  .arv {
    text-align: right;
    font-variant-numeric: tabular-nums;
  }

  .koostaja {
    border: 1px solid var(--joon);
    border-radius: 8px;
    padding: 0.5rem 0.75rem 0.3rem;
    margin: 0;
  }

  .valikud {
    display: flex;
    flex-wrap: wrap;
    gap: 0.3rem 0.9rem;
  }

  .valikud label {
    display: flex;
    align-items: center;
    gap: 0.25rem;
  }

  .koostaja p {
    margin: 0.4rem 0 0.2rem;
  }
</style>
