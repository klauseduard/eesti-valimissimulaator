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
  const ringkonnaNimi = (nr: number) => `${nr}. ${RINGKONNAD[nr - 1].nimetus}`;
</script>

<h2 id="piirikohad-pealkiri">{t('piirikohad.pealkiri')}</h2>
<p class="vaike vaigne">{olek.tegelikudHääled ? t('piirikohad.selgitus.tegelikud') : t('piirikohad.selgitus', { seeme: olek.seeme })}</p>

{#if viimane}
  <p data-testid="viimane-kompensatsioon">
    {t('piirikohad.viimane', {
      võitja: erakonnaInfo(viimane.võitja).lühend,
      järgmine: erakonnaInfo(viimane.järgmine).lühend,
      puudu: arv(viimane.puudu),
    })}
  </p>
{/if}

<p class="vaike vaigne">{t('piirikohad.kuskohalt')}</p>

<div class="kaks">
  <div>
    <h3>{t('piirikohad.saamata')}</h3>
    <ul data-testid="lähimad-saamata">
      {#each lähimadSaamata as x (x.ringkond + x.erakond)}
        <li>
          <button class="link" onclick={() => (olek.ringkond = x.ringkond)}>
            <span class="varv" style:background={erakonnaInfo(x.erakond).värv}></span>
            <strong>{erakonnaInfo(x.erakond).lühend}</strong>
            {t('piirikohad.puudu', { hääli: arv(x.puudu), kohti: x.mandaate + 1 })}
            <span class="vaigne vaike">{ringkonnaNimi(x.ringkond)}{koguarvMuutub(x.erakond) ? ` · ${t('piirikohad.koguarv')}` : ''}</span>
          </button>
        </li>
      {/each}
    </ul>
  </div>
  <div>
    <h3>{t('piirikohad.saadud')}</h3>
    <ul>
      {#each napimadSaadud as x (x.ringkond + x.erakond)}
        <li>
          <button class="link" onclick={() => (olek.ringkond = x.ringkond)}>
            <span class="varv" style:background={erakonnaInfo(x.erakond).värv}></span>
            <strong>{erakonnaInfo(x.erakond).lühend}</strong>
            {t('piirikohad.varu', { hääli: arv(x.varu ?? 0), kohti: x.mandaate })}
            <span class="vaigne vaike">{ringkonnaNimi(x.ringkond)}</span>
          </button>
        </li>
      {/each}
    </ul>
  </div>
</div>

<details class="vaike">
  <summary>{t('meetod.kuidas')}</summary>
  <p class="vaigne">{t('piirikohad.meetod')}</p>
</details>

<style>
  .kaks {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
    gap: 0.5rem 1.2rem;
  }

  h3 {
    font-size: 0.9rem;
    color: var(--tekst-2);
    margin: 0.4rem 0 0.3rem;
  }

  ul {
    list-style: none;
    padding: 0;
    margin: 0;
    display: grid;
    gap: 0.25rem;
  }

  .link {
    background: none;
    border: none;
    padding: 0.15rem 0.2rem;
    text-align: left;
    border-radius: 6px;
    cursor: pointer;
    line-height: 1.35;
    width: 100%;
  }

  .link:hover {
    background: var(--pind-2);
  }

  .link .vaike {
    display: block;
    padding-left: 1.1em;
  }

  summary {
    cursor: pointer;
    color: var(--tekst-2);
    margin-top: 0.6rem;
  }

  details p {
    margin: 0.4rem 0 0;
  }
</style>
