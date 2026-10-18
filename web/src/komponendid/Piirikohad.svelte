<script lang="ts">
  // Kõige napimad kohad näidiskorduses: viimase kompensatsioonimandaadi piir ning ringkonnamandaadid, millest
  // puudus kõige vähem hääli või mis saadi kõige väiksema varuga.
  import { erakonnaInfo } from '../lib/erakonnad';
  import { arv, t } from '../lib/i18n.svelte';
  import type { Olek } from '../lib/olek';
  import { häältPuudu, kompensatsiooniPiir } from '../lib/piirikohad';
  import { RINGKONNAD, type Tulemus } from '../lib/simulaator';

  let { tulemus, olek = $bindable() }: { tulemus: Tulemus; olek: Olek } = $props();

  const NÄITA = 5;

  const viimane = $derived(
    tulemus.kompensatsioon.length > 0 ? kompensatsiooniPiir(tulemus.kompensatsioon[tulemus.kompensatsioon.length - 1]) : null,
  );

  const kompensatsioone = $derived(new Map(tulemus.erakonnad.map((e) => [e.erakond, e.kompensatsioonimandaate])));

  /** iga ringkonna ja erakonna kohta: puuduvad hääled järgmisest kohast ja varu viimase saadud koha piirist */
  const ringkonnad = $derived(
    tulemus.ringkonnad.flatMap((r) =>
      r.erakonnad.map((e) => {
        const täis = Math.floor(e.kvoote);
        const piir = e.lisamandaat ? täis + 0.75 : täis; // viimase saadud koha piir kvootides
        return {
          ringkond: r.ringkond,
          erakond: e.erakond,
          puudu: häältPuudu(e.hääli, r.lihtkvoot),
          varu: e.mandaate > 0 ? Math.floor(e.hääli - piir * r.lihtkvoot) : null,
          mandaate: e.mandaate,
        };
      }),
    ),
  );
  const lähimadSaamata = $derived([...ringkonnad].sort((a, b) => a.puudu - b.puudu).slice(0, NÄITA));
  const napimadSaadud = $derived(
    ringkonnad.filter((x) => x.varu !== null).sort((a, b) => a.varu! - b.varu!).slice(0, NÄITA),
  );

  /** lisanduv ringkonnamandaat arvatakse kompensatsioonimandaatidest maha, kui neid erakonnal on */
  const koguarvMuutub = (erakond: string) => (kompensatsioone.get(erakond) ?? 0) === 0;
  const ringkonnaNimi = (nr: number) => RINGKONNAD[nr - 1].nimetus;
  const märkusVaja = $derived(lähimadSaamata.some((x) => koguarvMuutub(x.erakond)));
</script>

<h2 id="piirikohad-pealkiri">{t('piirikohad.pealkiri')}</h2>
<p class="vaike vaigne">{olek.tegelikudHääled ? t('piirikohad.selgitus.tegelikud') : t('piirikohad.selgitus', { seeme: olek.seeme })}</p>

{#if viimane}
  <p class="esile" data-testid="viimane-kompensatsioon">
    {t('piirikohad.viimane', {
      võitja: erakonnaInfo(viimane.võitja).lühend,
      järgmine: erakonnaInfo(viimane.järgmine).lühend,
      puudu: arv(viimane.puudu),
    })}
  </p>
{/if}

{#snippet tabel(pealkiri: string, veerg: string, read: typeof ringkonnad, hääled: (x: (typeof ringkonnad)[number]) => number, koht: (x: (typeof ringkonnad)[number]) => number, testid: string, märgi: boolean)}
  <div>
    <h3>{pealkiri}</h3>
    <table class="vaike" data-testid={testid}>
      <thead>
        <tr>
          <th scope="col">{t('erakond')}</th>
          <th scope="col">{t('ringkond')}</th>
          <th scope="col" class="arv">{t('piirikohad.koht')}</th>
          <th scope="col" class="arv">{veerg}</th>
        </tr>
      </thead>
      <tbody>
        {#each read as x (x.ringkond + x.erakond)}
          <tr>
            <th scope="row"><span class="varv" style:background={erakonnaInfo(x.erakond).värv}></span>{erakonnaInfo(x.erakond).lühend}</th>
            <td class="ringkond">
              <button class="link" onclick={() => (olek.ringkond = x.ringkond)} title={ringkonnaNimi(x.ringkond)}>
                <span class="nr">{x.ringkond}</span>{ringkonnaNimi(x.ringkond)}
              </button>
            </td>
            <td class="arv vaigne">{koht(x)}.{#if märgi && koguarvMuutub(x.erakond)}<span class="märk" title={t('piirikohad.koguarv')}>*</span>{/if}</td>
            <td class="arv hääli">{arv(hääled(x))}</td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
{/snippet}

<div class="kaks">
  {@render tabel(t('piirikohad.saamata'), t('piirikohad.puudu.veerg'), lähimadSaamata, (x) => x.puudu, (x) => x.mandaate + 1, 'lähimad-saamata', true)}
  {@render tabel(t('piirikohad.saadud'), t('piirikohad.varu.veerg'), napimadSaadud, (x) => x.varu ?? 0, (x) => x.mandaate, 'napimad-saadud', false)}
</div>

<p class="vaike vaigne märkused">
  {t('piirikohad.kuskohalt')}{#if märkusVaja}{' '}<span class="märk">*</span> {t('piirikohad.koguarv')}.{/if}
</p>

<details class="vaike">
  <summary>{t('meetod.kuidas')}</summary>
  <p class="vaigne">{t('piirikohad.meetod')}</p>
</details>

<style>
  .esile {
    border-left: 3px solid var(--rohk);
    background: var(--rohk-pind);
    border-radius: 0 6px 6px 0;
    padding: 0.5rem 0.8rem;
    margin: 0.6rem 0 0.8rem;
  }

  .kaks {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 24rem), 1fr));
    gap: 0.8rem 1.4rem;
  }

  h3 {
    font-size: 0.9rem;
    color: var(--tekst-2);
    margin: 0.2rem 0 0.2rem;
  }

  table {
    width: 100%;
    table-layout: fixed;
  }

  th:nth-child(1) {
    width: 5.2rem;
  }

  th:nth-child(3) {
    width: 3rem;
  }

  th:nth-child(4) {
    width: 4.2rem;
  }

  tbody th {
    font-weight: 600;
    white-space: nowrap;
  }

  .ringkond {
    overflow: hidden;
  }

  .link {
    background: none;
    border: none;
    padding: 0;
    text-align: left;
    cursor: pointer;
    width: 100%;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    color: var(--tekst-2);
    font: inherit;
  }

  .link:hover {
    color: var(--rohk);
    text-decoration: underline;
  }

  .nr {
    display: inline-block;
    min-width: 1.5em;
    color: var(--tekst-3);
    font-variant-numeric: tabular-nums;
  }

  .arv {
    text-align: right;
    font-variant-numeric: tabular-nums;
  }

  .hääli {
    font-weight: 600;
  }

  .märk {
    color: var(--rohk);
    font-weight: 700;
  }

  .märkused {
    margin: 0.6rem 0 0;
  }

  summary {
    cursor: pointer;
    color: var(--tekst-2);
    margin-top: 0.6rem;
  }

  /* kitsas kaart: ringkonna nimi murdub mitmele reale, et see oleks loetav */
  @container kaart (max-width: 520px) {
    .link {
      white-space: normal;
      display: flex;
      gap: 0.1rem;
    }

    .nr {
      flex: none;
    }

    td,
    tbody th {
      vertical-align: top;
    }
  }

  details p {
    margin: 0.4rem 0 0;
  }
</style>
