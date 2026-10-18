<script lang="ts">
  // Kleepuv jaotiste riba. URL-i fragment hoiab lehe olekut, seega ei kasuta see #ankruid, vaid kerib nupuga
  // jaotiseni. Aktiivne on dokumendi järjekorras viimane jaotis, mille ülaserv on jõudnud riba alla; kõrvuti
  // olevatest jaotistest (sama ülaservaga) jääb aktiivseks see, millele klõpsati.
  import { onMount } from 'svelte';
  import { t } from '../lib/i18n.svelte';

  let { jaotised }: { jaotised: { id: string; silt: string; ainultKitsas?: boolean }[] } = $props();

  /** riba kõrgus koos varuga: jaotis on aktiivne, kui ülaserv on sellest kõrgemal */
  const PIIR_PX = 90;

  let aktiivne = $state('');
  let klõpsatud = '';
  let loend = $state<HTMLUListElement>();

  // kitsal ekraanil keritakse aktiivne nupp riba nähtavasse ossa (ainult horisontaalselt)
  $effect(() => {
    const nupp = loend?.querySelector<HTMLElement>(`[data-testid="nav-${aktiivne}"]`);
    if (!loend || !nupp) return;
    const vasak = nupp.offsetLeft - loend.offsetLeft;
    if (vasak < loend.scrollLeft || vasak + nupp.offsetWidth > loend.scrollLeft + loend.clientWidth) {
      loend.scrollTo({ left: vasak - 16 });
    }
  });

  function keri(id: string) {
    klõpsatud = id;
    aktiivne = id;
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  function uuenda() {
    let valitud = '';
    let valitudÜlaserv = -Infinity;
    for (const j of jaotised) {
      const el = document.getElementById(j.id);
      if (!el || el.offsetParent === null) continue;
      const ülaserv = Math.round(el.getBoundingClientRect().top);
      if (ülaserv > PIIR_PX) continue;
      if (ülaserv > valitudÜlaserv || (ülaserv === valitudÜlaserv && j.id === klõpsatud)) {
        valitud = j.id;
        valitudÜlaserv = ülaserv;
      }
    }
    aktiivne = valitud;
  }

  onMount(() => {
    let kaader = 0;
    const kerimine = () => {
      cancelAnimationFrame(kaader);
      kaader = requestAnimationFrame(uuenda);
    };
    const kasutajaKerib = () => (klõpsatud = '');
    window.addEventListener('scroll', kerimine, { passive: true });
    window.addEventListener('wheel', kasutajaKerib, { passive: true });
    window.addEventListener('touchmove', kasutajaKerib, { passive: true });
    uuenda();
    return () => {
      window.removeEventListener('scroll', kerimine);
      window.removeEventListener('wheel', kasutajaKerib);
      window.removeEventListener('touchmove', kasutajaKerib);
      cancelAnimationFrame(kaader);
    };
  });
</script>

<nav class="jaotised" aria-label={t('nav.silt')}>
  <ul bind:this={loend}>
    {#each jaotised as j (j.id)}
      <li class:ainult-kitsas={j.ainultKitsas}>
        <button class:aktiivne={aktiivne === j.id} aria-current={aktiivne === j.id ? 'location' : undefined} onclick={() => keri(j.id)} data-testid="nav-{j.id}">
          {j.silt}
        </button>
      </li>
    {/each}
  </ul>
</nav>

<style>
  .jaotised {
    min-width: 0;
    position: sticky;
    top: 0;
    z-index: 5;
    margin: -0.25rem 0 0;
    padding: 0.4rem 0;
    background: color-mix(in srgb, var(--pind) 96%, transparent);
    backdrop-filter: blur(6px);
    border-bottom: 1px solid var(--joon);
  }

  ul {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    gap: 0.25rem;
    overflow-x: auto;
    scrollbar-width: none;
  }

  button {
    border: 1px solid transparent;
    background: none;
    border-radius: 999px;
    padding: 0.25rem 0.75rem;
    font-size: 0.88rem;
    color: var(--tekst-2);
    white-space: nowrap;
    cursor: pointer;
  }

  button:hover {
    background: var(--pind-2);
    color: var(--tekst);
  }

  button.aktiivne {
    background: var(--rohk-pind);
    border-color: var(--rohk);
    color: var(--tekst);
    font-weight: 600;
  }

  .ainult-kitsas {
    display: none;
  }

  @media (max-width: 860px) {
    .ainult-kitsas {
      display: list-item;
    }
  }
</style>
