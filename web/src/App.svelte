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
    // sõltuvused: toetus, seeme ja tegelike häälte valik
    const andmed = { erakonnad: [...toetus.erakonnad], protsendid: toetus.protsendid.map((r) => [...r]) };
    const seeme = olek.seeme;
    const viga: [number, number] = olek.viga ? eelseadistuseViga(olek.eelseadistus) : [0, 0];
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
      tööline?.postMessage({ tüüp: 'alusta', id, toetus: andmed, seeme, viga, kordi: KORDI, partii: PARTII } satisfies SõnumTöölisele);
    }, OOTEAEG_MS);
  });

  onDestroy(() => {
    clearTimeout(taimer);
    tööline?.terminate();
  });
</script>

<svelte:window onhashchange={fragmendiMuutus} />

<Ylariba bind:olek />

<main>
  {#if lingiVead.length > 0}
    <div class="teade hoiatus" role="status" data-testid="lingi-teade">
      <span>{t('viga.link', { osad: lingiVead.join(', ') })}</span>
      <button onclick={() => (lingiVead = [])}>{t('sulge')}</button>
    </div>
  {/if}

  <div class="paigutus">
    <aside class="kaart-pind sisend" id="sisend">
      <ToetuseSisend bind:olek {toetus} vead={sisendiVead} />
    </aside>

    <div class="tulemused">
      {#if arvutus.viga}
        <div class="teade viga" role="alert">{t('viga.arvutus', { teade: arvutus.viga })}</div>
      {/if}
      {#if sisendiVead.length > 0}
        <div class="teade viga" role="alert">
          <strong>{t('viga.sisend')}</strong>
          <ul>{#each sisendiVead as viga}<li>{viga}</li>{/each}</ul>
        </div>
      {/if}

      <a class="sisendisse vaike" href="#sisend">{t('sisend.muuda')}</a>
      {#if tulemus}
        <section class="kaart-pind" class:aegunud={!arvutus.tulemus} aria-labelledby="parlament-pealkiri">
          <Parlament {tulemus} {kokkuvõte} {kokkuvõteKehtib} {mcKäib} {mcViga} kordi={KORDI} bind:olek />
        </section>
        <div class="ylemine" class:aegunud={!arvutus.tulemus}>
          <section class="kaart-pind" aria-labelledby="koalitsioonid-pealkiri">
            <Koalitsioonid {tulemus} {kokkuvõte} {kokkuvõteKehtib} {olek} />
          </section>
          <section class="kaart-pind" aria-labelledby="piirikohad-pealkiri">
            <Piirikohad {tulemus} bind:olek />
          </section>
        </div>
        <div class="ylemine" class:aegunud={!arvutus.tulemus}>
          <section class="kaart-pind" aria-labelledby="kaart-pealkiri">
            <Kaart {tulemus} {toetus} {kokkuvõte} {kokkuvõteKehtib} bind:olek />
          </section>
          <section class="kaart-pind" aria-labelledby="ringkonnad-pealkiri">
            <Ringkonnad {tulemus} {kokkuvõte} {kokkuvõteKehtib} bind:olek />
          </section>
        </div>

        <section class="kaart-pind" aria-labelledby="selgitus-pealkiri">
          <Selgitus {tulemus} bind:olek />
        </section>

        <section class="kaart-pind" aria-labelledby="kompensatsioon-pealkiri">
          <Kompensatsioon {tulemus} seeme={olek.seeme} tegelikudHääled={olek.tegelikudHääled} />
        </section>

        <section class="kaart-pind" aria-labelledby="kandidaadid-pealkiri">
          <Kandidaadid {tulemus} {kokkuvõte} tegelikudHääled={olek.tegelikudHääled} seeme={olek.seeme} />
        </section>
      {/if}
    </div>
  </div>
</main>

<Jalus />

<style>
  main {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 16px 2rem;
  }

  .paigutus {
    display: grid;
    grid-template-columns: minmax(280px, 340px) minmax(0, 1fr);
    gap: 1rem;
    align-items: start;
  }

  .sisend {
    position: sticky;
    top: 1rem;
    max-height: calc(100vh - 2rem);
    overflow-y: auto;
  }

  .tulemused {
    display: grid;
    gap: 1rem;
    min-width: 0;
  }

  .ylemine {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 400px), 1fr));
    gap: 1rem;
    align-items: start;
    transition: opacity 0.2s;
  }

  .aegunud {
    opacity: 0.55;
  }

  /* kitsal ekraanil liigub sisend tulemuste alla; see link viib sinna */
  .sisendisse {
    display: none;
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

  .tulemused .teade {
    margin-bottom: 0;
    display: block;
  }

  .hoiatus {
    background: var(--hoiatus-pind);
  }

  .viga {
    background: var(--viga-pind);
  }

  @media (max-width: 860px) {
    .paigutus {
      grid-template-columns: minmax(0, 1fr);
    }

    .sisend {
      position: static;
      max-height: none;
      order: 2;
    }

    .sisendisse {
      display: inline-block;
    }
  }
</style>
