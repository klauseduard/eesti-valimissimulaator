<script lang="ts">
  import { erakonnaInfo, järjekorraNr } from '../lib/erakonnad';
  import { arv, protsent, t } from '../lib/i18n.svelte';
  import { RINGKONNAD, type Tulemus } from '../lib/simulaator';

  let { tulemus, seeme, tegelikudHääled }: { tulemus: Tulemus; seeme: number; tegelikudHääled: boolean } = $props();

  const kehtivaid = $derived(RINGKONNAD.reduce((s, r) => s + r.kehtivaidHääli, 0));
  const ringkondades = $derived(tulemus.valitud.length - tulemus.kompensatsioon.length);
  const künniseÜletanud = $derived(tulemus.erakonnad.filter((e) => e.ületasKünnise));
  const veerud = $derived(
    [...new Set(tulemus.kompensatsioon.flatMap((s) => s.erakonnad))].sort((a, b) => järjekorraNr(a) - järjekorraNr(b)),
  );

  function jagaja(mandaate: number): number {
    return (mandaate + 1) ** 0.9;
  }
</script>

<h2 id="kompensatsioon-pealkiri">{t('kompensatsioon.pealkiri')}</h2>
<p class="näidis vaike">{tegelikudHääled ? t('näidiskordus.tegelikud') : t('näidiskordus', { seeme })}</p>

<ol class="sammud">
  <li>
    <h3>{t('kompensatsioon.künnis.pealkiri')} <span class="viide">{t('viide', { lõige: '3–4' })}</span></h3>
    <p>
      {t('kompensatsioon.künnis', { künnis: arv(kehtivaid * 0.05), kehtivaid: arv(kehtivaid) })}
      {künniseÜletanud.map((e) => `${erakonnaInfo(e.erakond).lühend} (${protsent(e.protsent)})`).join(', ')}.
    </p>
  </li>
  <li>
    <h3>{t('kompensatsioon.arv.pealkiri')} <span class="viide">{t('viide', { lõige: '4' })}</span></h3>
    <p>{t('kompensatsioon.arv', { ringkondades, kompensatsioon: tulemus.kompensatsioon.length })}</p>
  </li>
  <li>
    <h3>{t('kompensatsioon.dhondt.pealkiri')} <span class="viide">{t('viide', { lõige: '5–7' })}</span></h3>
    <p>{t('kompensatsioon.dhondt')}</p>
  </li>
</ol>

{#if tulemus.kompensatsioon.length > 0}
  <div class="tabeli-ümbris">
    <table data-testid="kompensatsiooni-tabel">
      <thead>
        <tr>
          <th scope="col" class="arv">#</th>
          {#each veerud as erakond (erakond)}
            <th scope="col" class="arv" title={erakond}>
              <span class="varv" style:background={erakonnaInfo(erakond).värv}></span>{erakonnaInfo(erakond).lühend}
            </th>
          {/each}
          <th scope="col">{t('kompensatsioon.kandidaat')}</th>
        </tr>
      </thead>
      <tbody>
        {#each tulemus.kompensatsioon as samm, i}
          <tr>
            <td class="arv vaigne">{i + 1}</td>
            {#each veerud as erakond (erakond)}
              {@const j = samm.erakonnad.indexOf(erakond)}
              {#if j < 0}
                <td class="arv vaigne">–</td>
              {:else}
                <td
                  class="arv"
                  class:võitja={samm.erakond === erakond}
                  style:--värv={erakonnaInfo(erakond).värv}
                  title={t('kompensatsioon.võrdlusarv', {
                    hääli: arv(samm.võrdlusarvud[j] * jagaja(samm.jubaMandaate[j])),
                    mandaate: samm.jubaMandaate[j],
                    jagaja: arv(jagaja(samm.jubaMandaate[j]), 3),
                  })}
                >{arv(samm.võrdlusarvud[j])}</td>
              {/if}
            {/each}
            <td>
              <span class="varv" style:background={erakonnaInfo(samm.erakond).värv}></span>
              {samm.kandidaat.nimi}
              <span class="vaigne vaike">({t('kompensatsioon.nimekirjas', { nr: samm.kandidaat.jrkNr, ringkond: samm.kandidaat.ringkond })})</span>
              {#if samm.viik}<span class="märk" title={t('kompensatsioon.viik.selgitus')}>{t('kompensatsioon.viik')}</span>{/if}
              {#if samm.varureegel}<span class="märk" title={t('kompensatsioon.varureegel.selgitus')}>{t('kompensatsioon.varureegel')}</span>{/if}
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
{/if}

<style>
  .näidis {
    background: var(--rohk-pind);
    border-radius: 6px;
    padding: 0.35rem 0.6rem;
    margin: 0 0 0.6rem;
  }

  .sammud {
    padding-left: 1.3rem;
    margin: 0 0 0.8rem;
    display: grid;
    gap: 0.6rem;
  }

  .sammud h3 {
    margin-bottom: 0.2rem;
  }

  .sammud p {
    margin: 0.2rem 0;
  }

  .viide {
    font-weight: 400;
    font-size: 0.8rem;
    color: var(--tekst-3);
    margin-left: 0.3rem;
  }

  table {
    font-size: 0.85rem;
    width: 100%;
  }

  td:last-child {
    white-space: nowrap;
  }

  td.võitja {
    font-weight: 700;
    box-shadow: inset 3px 0 0 var(--värv);
    background: color-mix(in srgb, var(--värv) 14%, transparent);
  }

  .märk {
    font-size: 0.75rem;
    background: var(--rohk-pind);
    border-radius: 4px;
    padding: 0 0.3em;
    margin-left: 0.25em;
  }
</style>
