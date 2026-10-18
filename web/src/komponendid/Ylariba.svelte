<script lang="ts">
  import { t } from '../lib/i18n.svelte';
  import { RK2023, type Keel, type Olek } from '../lib/olek';
  import { EELSEADISTUSED } from '../lib/simulaator';

  let { olek = $bindable() }: { olek: Olek } = $props();

  const README = 'https://github.com/klauseduard/eesti-valimissimulaator#readme';

  function valiEelseadistus(sündmus: Event) {
    olek.eelseadistus = (sündmus.currentTarget as HTMLSelectElement).value;
    olek.muudatused = {};
    olek.tegelikudHääled = olek.eelseadistus === RK2023;
  }

  const muudetud = $derived(Object.keys(olek.muudatused).length > 0);
</script>

<header>
  <div class="sisu">
    <div class="nimi">
      <h1>{t('pealkiri')}</h1>
      <p class="vaigne vaike">{t('alapealkiri')}</p>
    </div>

    <div class="valikud">
      <label>
        <span class="silt">{t('eelseadistus')}</span>
        <select value={olek.eelseadistus} onchange={valiEelseadistus} data-testid="eelseadistus">
          {#each EELSEADISTUSED as tunnus}
            <option value={tunnus}>{t(`eelseadistus.${tunnus}`)}{muudetud && tunnus === olek.eelseadistus ? ` (${t('muudetud')})` : ''}</option>
          {/each}
        </select>
      </label>

      {#if olek.eelseadistus === RK2023}
        <label class="märkeruut" title={t('tegelikudHääled.selgitus')}>
          <input
            type="checkbox"
            bind:checked={olek.tegelikudHääled}
            disabled={muudetud}
            data-testid="tegelikud-hääled"
          />
          <span>{t('tegelikudHääled')}</span>
        </label>
      {/if}


      <div class="keel" role="group" aria-label={t('keel')}>
        {#each ['et', 'en'] as const as keel (keel)}
          <button
            aria-pressed={olek.keel === keel}
            class:valitud={olek.keel === keel}
            onclick={() => (olek.keel = keel as Keel)}
            lang={keel}
          >{keel.toUpperCase()}</button>
        {/each}
      </div>

      <a href="metoodika.html#k={olek.keel}" class="vaike" data-testid="metoodika-link">{t('metoodika')}</a>
      <a href={README} class="vaike">{t('lähtekood')}</a>
    </div>
  </div>
</header>

<style>
  header {
    background: var(--paber);
  }

  .sisu {
    max-width: 1320px;
    margin: 0 auto;
    padding: 1rem 16px 0.8rem;
    display: flex;
    flex-wrap: wrap;
    gap: 0.8rem 2rem;
    align-items: center;
    justify-content: space-between;
  }

  h1 {
    font-size: 1.7rem;
    margin: 0;
    letter-spacing: -0.02em;
  }

  @media (max-width: 600px) {
    .sisu {
      padding: 0.7rem 12px 0.6rem;
      gap: 0.5rem 1rem;
    }

    h1 {
      font-size: 1.35rem;
    }

    .nimi p {
      display: none;
    }
  }

  .nimi p {
    margin: 0.1rem 0 0;
  }

  .valikud {
    display: flex;
    flex-wrap: wrap;
    gap: 0.6rem 1rem;
    align-items: center;
  }

  label {
    display: flex;
    align-items: center;
    gap: 0.4rem;
  }

  .silt {
    color: var(--tekst-2);
    font-size: 0.9rem;
  }

  .keel {
    display: flex;
  }

  .keel button {
    border-radius: 0;
    padding: 0.3em 0.6em;
  }

  .keel button:first-child {
    border-radius: 6px 0 0 6px;
  }

  .keel button:last-child {
    border-radius: 0 6px 6px 0;
    border-left: none;
  }

  .keel .valitud {
    background: var(--rohk-pind);
    border-color: var(--rohk);
    font-weight: 600;
  }

  button:disabled,
  input:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
</style>
