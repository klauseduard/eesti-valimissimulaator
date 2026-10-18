<script lang="ts">
  // Metoodika leht (metoodika.html): eeldused, kontrollid ja see, mida ei modelleerita. Keel tuleb
  // fragmendist #k=et või #k=en, nagu rakenduses.
  import Jalus from './komponendid/Jalus.svelte';
  import En from './metoodika/En.svelte';
  import Et from './metoodika/Et.svelte';
  import { brauseriKeel, i18n, t } from './lib/i18n.svelte';
  import type { Keel } from './lib/olek';

  function keelFragmendist(): Keel {
    const k = new URLSearchParams(location.hash.slice(1)).get('k');
    return k === 'et' || k === 'en' ? k : brauseriKeel();
  }

  i18n.keel = keelFragmendist();

  function valiKeel(keel: Keel) {
    i18n.keel = keel;
    history.replaceState(null, '', `#k=${keel}`);
  }

  $effect(() => {
    document.title = `${t('metoodika.pealkiri')} – ${t('pealkiri')}`;
  });
</script>

<header>
  <div class="sisu">
    <a href="./#k={i18n.keel}" data-testid="tagasi">← {t('pealkiri')}</a>
    <div class="keel" role="group" aria-label={t('keel')}>
      {#each ['et', 'en'] as const as keel (keel)}
        <button aria-pressed={i18n.keel === keel} class:valitud={i18n.keel === keel} onclick={() => valiKeel(keel)} lang={keel}>
          {keel.toUpperCase()}
        </button>
      {/each}
    </div>
  </div>
</header>

<main class="artikkel">
  {#if i18n.keel === 'et'}<Et />{:else}<En />{/if}
</main>

<Jalus />

<style>
  header {
    border-bottom: 1px solid var(--joon);
    background: var(--pind-2);
  }

  .sisu {
    max-width: 46rem;
    margin: 0 auto;
    padding: 0.8rem 16px;
    display: flex;
    flex-wrap: wrap;
    gap: 0.6rem 1rem;
    align-items: center;
    justify-content: space-between;
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

  .artikkel {
    max-width: 46rem;
    margin: 0 auto;
    padding: 1rem 16px 2rem;
    line-height: 1.6;
  }

  .artikkel :global(h1) {
    font-size: 1.6rem;
    margin: 1rem 0 0.6rem;
  }

  .artikkel :global(h2) {
    margin: 2rem 0 0.4rem;
  }

  .artikkel :global(.märkus) {
    border-left: 3px solid var(--joon);
    padding: 0.2rem 0 0.2rem 0.8rem;
    color: var(--tekst-2);
    font-size: 0.9rem;
  }

  .artikkel :global(li) {
    margin: 0.3rem 0;
  }

  .artikkel :global(code) {
    font-size: 0.9em;
  }
</style>
