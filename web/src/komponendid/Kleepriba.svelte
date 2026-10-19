<script lang="ts">
  // Kleepuv riba lehe ülaosas: korduste keskmine koosseis ühe 101 kohaga tulbana (enamuse piir 51 märgitud) ja
  // jaotiste nupud. Liugurit liigutades on koosseisu muutus näha ka siis, kui poolring on vaatest väljas.
  // URL-i fragment hoiab lehe olekut, seega kerivad nupud jaotiseni, mitte #ankruga.
  import { onMount } from 'svelte';
  import { erakonnaInfo, järjekorraNr, kontrastTekst } from '../lib/erakonnad';
  import { t } from '../lib/i18n.svelte';
  import { MANDAATE } from '../lib/koosseis';

  let {
    koosseis,
    uueneb,
    tegelikud,
    jaotised,
  }: {
    koosseis: Map<string, number> | null;
    uueneb: boolean;
    tegelikud: boolean;
    jaotised: { id: string; silt: string; ainultKitsas?: boolean }[];
  } = $props();

  /** jaotis on aktiivne, kui selle ülaserv on riba alumisest servast kõrgemal */
  const PIIR_PX = 110;

  let aktiivne = $state('');
  let klõpsatud = '';
  let loend = $state<HTMLUListElement>();
  let tulbaLaius = $state(0);
  /** riba tegelik kõrgus CSS-muutujaks, et teised kleepuvad osad (toetuse paneeli päis) jääksid selle alla */
  let ribaKõrgus = $state(0);
  $effect(() => {
    if (ribaKõrgus > 0) document.documentElement.style.setProperty('--riba-kõrgus', `${ribaKõrgus}px`);
  });

  const lõigud = $derived(
    koosseis === null
      ? []
      : [...koosseis.entries()]
          .filter(([, m]) => m > 0)
          .sort((a, b) => järjekorraNr(a[0]) - järjekorraNr(b[0]))
          .map(([erakond, mandaate]) => ({ erakond, mandaate, info: erakonnaInfo(erakond) })),
  );
  const kirjeldus = $derived(
    lõigud.map((l) => `${l.info.lühend} ${l.mandaate}`).join(', '),
  );
  const silt = (mandaate: number, lühend: string) => {
    const laius = (mandaate / MANDAATE) * tulbaLaius;
    return laius >= 60 ? `${lühend} ${mandaate}` : laius >= 22 ? String(mandaate) : '';
  };

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

  // kitsal ekraanil keritakse aktiivne nupp riba nähtavasse ossa (ainult horisontaalselt)
  $effect(() => {
    const nupp = loend?.querySelector<HTMLElement>(`[data-testid="nav-${aktiivne}"]`);
    if (!loend || !nupp) return;
    const vasak = nupp.offsetLeft - loend.offsetLeft;
    if (vasak < loend.scrollLeft || vasak + nupp.offsetWidth > loend.scrollLeft + loend.clientWidth) {
      loend.scrollTo({ left: vasak - 16 });
    }
  });

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

<div class="kleepriba" bind:offsetHeight={ribaKõrgus}>
  <div class="sisu">
    <div class="koosseis" class:uueneb>
      <span class="pealdis">{tegelikud ? t('riba.tegelik') : t('riba.keskmine')}</span>
      <div
        class="tulp"
        role="img"
        aria-label={`${tegelikud ? t('riba.tegelik') : t('riba.keskmine')}: ${kirjeldus}`}
        bind:clientWidth={tulbaLaius}
        data-testid="koosseisu-riba"
      >
        <span class="lõigud">
        {#each lõigud as l (l.erakond)}
          <span class="lõik" style:flex-grow={l.mandaate} style:background={l.info.värv} title={`${l.info.lühend} ${l.mandaate}`}>
            <span style:color={kontrastTekst(l.info.värv)}>{silt(l.mandaate, l.info.lühend)}</span>
          </span>
        {/each}
        </span>
        {#if lõigud.length > 0}
          <span class="enamus" style:left="{((MANDAATE / 2) / MANDAATE) * 100}%" title={t('parlament.enamus')}></span>
        {/if}
      </div>
    </div>

    <nav aria-label={t('nav.silt')}>
      <ul bind:this={loend}>
        {#each jaotised as j (j.id)}
          <li class:ainult-kitsas={j.ainultKitsas}>
            <button
              class:aktiivne={aktiivne === j.id}
              aria-current={aktiivne === j.id ? 'location' : undefined}
              onclick={() => keri(j.id)}
              data-testid="nav-{j.id}"
            >
              {j.silt}
            </button>
          </li>
        {/each}
      </ul>
    </nav>
  </div>
</div>

<style>
  .kleepriba {
    position: sticky;
    top: 0;
    z-index: 10;
    background: color-mix(in srgb, var(--paber) 94%, transparent);
    backdrop-filter: blur(8px);
    border-bottom: 1px solid var(--joon);
  }

  .sisu {
    max-width: 1320px;
    margin: 0 auto;
    padding: 0.45rem 16px;
    display: grid;
    grid-template-columns: minmax(260px, 1fr) auto;
    gap: 0.3rem 1.5rem;
    align-items: center;
  }

  .koosseis {
    display: flex;
    align-items: center;
    gap: 0.7rem;
    min-width: 0;
    transition: opacity 0.2s;
  }

  .koosseis.uueneb {
    opacity: 0.45;
  }

  .pealdis {
    font-size: 0.72rem;
    font-weight: 600;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: var(--tekst-3);
    white-space: nowrap;
  }

  .tulp {
    position: relative;
    flex: 1;
    height: 24px;
    min-width: 0;
  }

  .lõigud {
    display: flex;
    gap: 2px;
    height: 100%;
    border-radius: 4px;
    overflow: hidden;
  }

  .lõik {
    flex-basis: 0;
    min-width: 2px;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    font-size: 0.72rem;
    font-weight: 650;
    white-space: nowrap;
    transition: flex-grow 0.35s ease;
  }

  .enamus {
    position: absolute;
    top: -5px;
    bottom: -5px;
    width: 2px;
    margin-left: -1px;
    background: var(--tekst);
    box-shadow: 0 0 0 1.5px var(--paber);
    pointer-events: none;
  }

  ul {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    gap: 0.15rem;
    overflow-x: auto;
    scrollbar-width: none;
  }

  button {
    border: 1px solid transparent;
    background: none;
    border-radius: 999px;
    padding: 0.25rem 0.7rem;
    font-size: 0.85rem;
    color: var(--tekst-2);
    white-space: nowrap;
    cursor: pointer;
  }

  button:hover {
    background: var(--pind-2);
    color: var(--tekst);
  }

  button.aktiivne {
    background: var(--tekst);
    border-color: var(--tekst);
    color: var(--paber);
    font-weight: 600;
  }

  .ainult-kitsas {
    display: none;
  }

  /* kitsal ekraanil: tulp ja nupud kahel real, nuppude rida keritav (parem serv hajub, et kerimine oleks näha) */
  @media (max-width: 1099px) {
    .sisu {
      grid-template-columns: minmax(0, 1fr);
      padding: 0.35rem 12px 0.25rem;
    }

    .ainult-kitsas {
      display: list-item;
    }

    ul {
      mask-image: linear-gradient(to right, black calc(100% - 28px), transparent);
      padding-right: 28px;
    }

    .tulp {
      height: 20px;
    }

    .pealdis {
      display: none;
    }
  }
</style>
