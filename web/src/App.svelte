<script lang="ts">
  import { onDestroy } from 'svelte';
  import Ylariba from './komponendid/Ylariba.svelte';
  import ToetuseSisend from './komponendid/ToetuseSisend.svelte';
  import Parlament from './komponendid/Parlament.svelte';
  import Kaart from './komponendid/Kaart.svelte';
  import Ringkonnad from './komponendid/Ringkonnad.svelte';
  import Koalitsioonid from './komponendid/Koalitsioonid.svelte';
  import Piirikohad from './komponendid/Piirikohad.svelte';
  import Selgitus from './komponendid/Selgitus.svelte';
  import Kompensatsioon from './komponendid/Kompensatsioon.svelte';
  import Kandidaadid from './komponendid/Kandidaadid.svelte';
  import Jalus from './komponendid/Jalus.svelte';
  import Kleepriba from './komponendid/Kleepriba.svelte';
  import SammHaaval from './komponendid/SammHaaval.svelte';
  import { keskmineKoosseis } from './lib/koosseis';
  import { andmeTekstid } from './lib/andmed';
  import { ERAKONNAD } from './lib/erakonnad';
  import { brauseriKeel, i18n, t } from './lib/i18n.svelte';
  import { dekodeeri, kodeeri, type Olek, type Teadaolevad } from './lib/olek';
  import type { Kokkuvõte, SõnumTöölisele, SõnumTöölselt } from './lib/protokoll';
  import {
    EELSEADISTUSED, VAIKIMISI_EELSEADISTUS, eelseadistuseViga, jaota, kontrolliToetust, olekuToetus, type Tulemus,
  } from './lib/simulaator';

  /** Monte Carlo korduste arv ja partii suurus (pärast iga partiid uuenevad vahemikud) */
  const KORDI = 1000;
  const PARTII = 100;
  /** paus pärast viimast muudatust, enne kui taustal korduvad simulatsioonid algavad */
  const OOTEAEG_MS = 150;

  const teadaolevad: Teadaolevad = {
    eelseadistused: EELSEADISTUSED,
    lühendid: Object.values(ERAKONNAD).map((e) => e.lühend),
    vaikimisiEelseadistus: VAIKIMISI_EELSEADISTUS,
  };

  const algus = dekodeeri(location.hash, teadaolevad, brauseriKeel());
  let olek = $state<Olek>(algus.olek);
  let lingiVead = $state<string[]>(algus.vead);

  $effect(() => {
    i18n.keel = olek.keel;
  });

  // olek URL-i fragmenti; replaceState ei lisa iga liuguri liigutust brauseri ajalukku
  $effect(() => {
    const fragment = kodeeri(olek);
    if (location.hash !== fragment) history.replaceState(null, '', fragment);
  });

  // kui kasutaja kleebib aadressiribale uue lingi
  function fragmendiMuutus() {
    if (location.hash === kodeeri(olek)) return;
    const uus = dekodeeri(location.hash, teadaolevad, olek.keel);
    olek = uus.olek;
    lingiVead = uus.vead;
  }

  const toetus = $derived(olekuToetus(olek));
  const sisendiVead = $derived(kontrolliToetust(toetus));

  // üks simulatsioon kohe pärast iga muudatust
  const arvutus = $derived.by((): { tulemus: Tulemus | null; viga: string | null } => {
    if (sisendiVead.length > 0) return { tulemus: null, viga: null };
    try {
      return { tulemus: jaota(olek, toetus), viga: null };
    } catch (viga) {
      return { tulemus: null, viga: viga instanceof Error ? viga.message : String(viga) };
    }
  });
  // viimane õnnestunud tulemus jääb nähtavaks, kui sisend on vahepeal vigane
  let viimaneTulemus = $state<Tulemus | null>(null);
  $effect(() => {
    if (arvutus.tulemus) viimaneTulemus = arvutus.tulemus;
  });
  const tulemus = $derived(arvutus.tulemus ?? viimaneTulemus);

  // korduvad simulatsioonid taustalõimes
  let kokkuvõte = $state<Kokkuvõte | null>(null);
  /** kas kokkuvõte vastab praegusele sisendile; vana kokkuvõte jääb uuendamise ajaks tuhmilt nähtavaks */
  let kokkuvõteKehtib = $state(false);
  let mcViga = $state<string | null>(null);
  let mcKäib = $state(false);
  let mcId = 0;
  let taimer: ReturnType<typeof setTimeout> | undefined;
  let tööline: Worker | null = null;

  try {
    tööline = new Worker(new URL('./lib/taustasimulatsioon.ts', import.meta.url), { type: 'module' });
    tööline.postMessage({ tüüp: 'andmed', andmed: andmeTekstid } satisfies SõnumTöölisele);
    tööline.onmessage = (sündmus: MessageEvent<SõnumTöölselt>) => {
      const s = sündmus.data;
      if (s.id !== mcId) return;
      if (s.tüüp === 'vahetulemus') {
        kokkuvõte = s.kokkuvõte;
        kokkuvõteKehtib = true;
        mcKäib = !s.valmis;
      } else {
        mcViga = s.teade;
        mcKäib = false;
      }
    };
    tööline.onerror = (sündmus) => {
      mcViga = sündmus.message || t('viga.tööline');
      mcKäib = false;
    };
  } catch (viga) {
    mcViga = viga instanceof Error ? viga.message : String(viga);
  }

  $effect(() => {
    // sõltuvused: toetus, seeme, küsitluse vea ja häälte jaotuse valik ning tegelike häälte valik
    const andmed = { erakonnad: [...toetus.erakonnad], protsendid: toetus.protsendid.map((r) => [...r]) };
    const seeme = olek.seeme;
    const viga: [number, number] = olek.viga ? eelseadistuseViga(olek.eelseadistus) : [0, 0];
    const jaotus = olek.jaotus;
    const ilmaJuhuseta = olek.tegelikudHääled;
    const vigane = sisendiVead.length > 0;

    clearTimeout(taimer);
    kokkuvõteKehtib = false;
    mcId += 1;
    const id = mcId;
    tööline?.postMessage({ tüüp: 'peata', id: id - 1 } satisfies SõnumTöölisele);
    if (ilmaJuhuseta || vigane || tööline === null) {
      mcKäib = false;
      if (ilmaJuhuseta || vigane) kokkuvõte = null;
      return;
    }
    mcKäib = true;
    mcViga = null;
    taimer = setTimeout(() => {
      tööline?.postMessage({ tüüp: 'alusta', id, toetus: andmed, seeme, viga, jaotus, kordi: KORDI, partii: PARTII } satisfies SõnumTöölisele);
    }, OOTEAEG_MS);
  });

  /** kleepuva riba koosseis: tegelike häältega ametlik tulemus, muidu korduste keskmine (nagu poolringil) */
  const koosseis = $derived.by((): Map<string, number> | null => {
    if (!tulemus) return null;
    if (olek.tegelikudHääled) return new Map(tulemus.erakonnad.map((e) => [e.erakond, e.mandaate]));
    if (!kokkuvõte || kokkuvõte.kordi === 0) return null;
    return keskmineKoosseis(tulemus.erakonnad.map((e) => e.erakond), kokkuvõte);
  });

  onDestroy(() => {
    clearTimeout(taimer);
    tööline?.terminate();
  });
</script>

<svelte:window onhashchange={fragmendiMuutus} />

<Ylariba bind:olek />

<Kleepriba
  {koosseis}
  uueneb={!olek.tegelikudHääled && !kokkuvõteKehtib}
  tegelikud={olek.tegelikudHääled}
  jaotised={[
    { id: 'mandaadid', silt: t('nav.mandaadid') },
    { id: 'sisend', silt: t('nav.toetus') },
    { id: 'koalitsioonid', silt: t('nav.koalitsioonid') },
    { id: 'piirikohad', silt: t('nav.piirikohad') },
    { id: 'ringkonnad', silt: t('nav.ringkonnad') },
    { id: 'arvutus', silt: t('nav.arvutus') },
    { id: 'kandidaadid', silt: t('nav.kandidaadid') },
  ]}
/>

<main>
  {#if lingiVead.length > 0}
    <div class="teade hoiatus" role="status" data-testid="lingi-teade">
      <span>{t('viga.link', { osad: lingiVead.join(', ') })}</span>
      <button onclick={() => (lingiVead = [])}>{t('sulge')}</button>
    </div>
  {/if}
  {#if arvutus.viga}
    <div class="teade viga" role="alert">{t('viga.arvutus', { teade: arvutus.viga })}</div>
  {/if}
  {#if sisendiVead.length > 0}
    <div class="teade viga" role="alert">
      <strong>{t('viga.sisend')}</strong>
      <ul>{#each sisendiVead as viga}<li>{viga}</li>{/each}</ul>
    </div>
  {/if}

  <!-- konsool: sisend vasakul, tulemus paremal ja vaates kinni; kitsal ekraanil tulemus enne sisendit -->
  <div class="konsool">
    <section class="paneel sisend kaart-pind" id="sisend" aria-labelledby="sisend-pealkiri">
      <ToetuseSisend bind:olek {toetus} vead={sisendiVead} />
    </section>
    {#if tulemus}
      <section class="paneel tulemus kaart-pind" id="mandaadid" class:aegunud={!arvutus.tulemus} aria-labelledby="parlament-pealkiri">
        <Parlament {tulemus} {kokkuvõte} {kokkuvõteKehtib} {mcKäib} {mcViga} kordi={KORDI} bind:olek />
      </section>
    {/if}
  </div>

  {#if tulemus}
    <div class="aruanne" class:aegunud={!arvutus.tulemus}>
      <div class="kaks">
        <section class="aruande-jaotis kaart-pind" id="koalitsioonid" aria-labelledby="koalitsioonid-pealkiri">
          <Koalitsioonid {tulemus} {kokkuvõte} {kokkuvõteKehtib} {olek} />
        </section>
        <section class="aruande-jaotis kaart-pind" id="piirikohad" aria-labelledby="piirikohad-pealkiri">
          <Piirikohad {tulemus} bind:olek />
        </section>
      </div>

      <section class="aruande-jaotis" id="ringkonnad" aria-labelledby="kaart-pealkiri">
        <div class="kaks">
          <div class="kaart-pind"><Kaart {tulemus} {toetus} {kokkuvõte} {kokkuvõteKehtib} bind:olek /></div>
          <div class="kaart-pind"><Ringkonnad {tulemus} {kokkuvõte} {kokkuvõteKehtib} bind:olek /></div>
        </div>
      </section>

      <section class="aruande-jaotis" id="arvutus" aria-labelledby="selgitus-pealkiri">
        <div class="virn">
          <div class="kaart-pind"><Selgitus {tulemus} bind:olek /></div>
          <div class="kaart-pind"><Kompensatsioon {tulemus} seeme={olek.seeme} tegelikudHääled={olek.tegelikudHääled} /></div>
        </div>
      </section>

      <section class="aruande-jaotis kaart-pind" id="kandidaadid" aria-labelledby="kandidaadid-pealkiri">
        <Kandidaadid {tulemus} {kokkuvõte} bind:olek />
      </section>
    </div>
  {/if}
</main>

{#if tulemus}
  <SammHaaval {tulemus} bind:olek />
{/if}

<Jalus />

<style>
  main {
    max-width: 1320px;
    margin: 0 auto;
    padding: 1.2rem 16px 3rem;
    counter-reset: jaotis 1;
  }

  /* konsool: kaks valget paneeli; tulemus on laiadel ja piisavalt kõrgetel ekraanidel kerimisel kinni */
  .konsool {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: 1rem;
    align-items: start;
  }

  .paneel {
    background: var(--pind);
    border: 1px solid var(--joon);
    border-radius: 14px;
    box-shadow: var(--vari);
    padding: 1.2rem 1.4rem 1.3rem;
  }

  .tulemus {
    order: -1;
  }

  @media (min-width: 1100px) {
    .konsool {
      grid-template-columns: minmax(360px, 4fr) minmax(0, 8fr);
    }

    .tulemus {
      order: 0;
    }
  }

  @media (min-width: 1100px) and (min-height: 760px) {
    .tulemus {
      position: sticky;
      top: calc(var(--riba-kõrgus) + 12px);
    }
  }

  .aruanne {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: 3rem;
    margin-top: 3rem;
    transition: opacity 0.2s;
  }

  .kaks {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: 3rem 3rem;
    align-items: start;
  }

  @media (min-width: 1000px) {
    .kaks {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

  }

  .virn {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: 2.5rem;
  }

  :global(section[id]) {
    scroll-margin-top: calc(var(--riba-kõrgus) + 16px);
  }

  .aegunud {
    opacity: 0.55;
  }

  .teade {
    border-radius: 8px;
    padding: 0.6rem 0.9rem;
    margin-bottom: 1rem;
    display: flex;
    gap: 1rem;
    align-items: flex-start;
    justify-content: space-between;
  }

  .teade ul {
    margin: 0.3rem 0 0;
    padding-left: 1.2rem;
  }

  .hoiatus {
    background: var(--hoiatus-pind);
  }

  .viga {
    background: var(--viga-pind);
  }

  @media (max-width: 600px) {
    main {
      padding: 0.8rem 12px 2rem;
    }

    .paneel {
      padding: 1rem 1rem 1.1rem;
      border-radius: 12px;
    }
  }
</style>
