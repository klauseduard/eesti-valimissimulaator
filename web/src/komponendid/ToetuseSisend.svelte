<script lang="ts">
  import { erakonnaInfo, järjekorraNr, kontrastTekst } from '../lib/erakonnad';
  import { arv, protsent, t } from '../lib/i18n.svelte';
  import { ümarda, type Olek } from '../lib/olek';
  import type { ToetuseAndmed } from '../lib/protokoll';
  import { RINGKONNAD, üleriigilineToetus, üleriigiliseToetuseMuudatused } from '../lib/simulaator';
  import Minikaart from './Minikaart.svelte';

  let { olek = $bindable(), toetus, vead }: { olek: Olek; toetus: ToetuseAndmed; vead: string[] } = $props();

  /** liuguri ülemine piir; erakonna toetus võib olla ka suurem, siis kasvab piir */
  const LIUGURI_PIIR = 60;
  const KÜNNIS = 5;

  const read = $derived(
    toetus.erakonnad
      .map((erakond, i) => ({
        erakond,
        info: erakonnaInfo(erakond),
        protsendid: toetus.protsendid[i],
        üleriigiline: üleriigilineToetus(toetus, erakond),
      }))
      .sort((a, b) => järjekorraNr(a.erakond) - järjekorraNr(b.erakond)),
  );

  const summad = $derived(RINGKONNAD.map((_, j) => toetus.protsendid.reduce((s, r) => s + r[j], 0)));
  /** teised nimekirjad ja üksikkandidaadid: kehtivate häälte osa, mida ükski loetletud erakond ei saa */
  const muud = $derived(summad.map((s) => 100 - s));
  const muudÜleriigiliselt = $derived(100 - read.reduce((s, r) => s + r.üleriigiline, 0));

  /** ringkond (0–11), mille veerule on hiir või fookus; selle nimi on päises ja veerg esile tõstetud */
  let aktiivne = $state<number | null>(null);
  /** rida (erakonna lühend või „muud"), mille ruudule osutatakse; selle ruudu kohal on ringkonna nimi */
  let aktiivneRida = $state<string | null>(null);
  function valiRuut(sündmus: Event) {
    const el = (sündmus.target as HTMLElement | null)?.closest<HTMLElement>('[data-ringkond]');
    aktiivne = el ? Number(el.dataset.ringkond) : null;
    aktiivneRida = el?.dataset.rida ?? null;
  }
  function tühjenda() {
    aktiivne = null;
    aktiivneRida = null;
  }
  const muudetud = $derived(Object.keys(olek.muudatused).length > 0);

  function muudaÜleriigilist(erakond: string, väärtus: number) {
    if (!Number.isFinite(väärtus) || väärtus < 0) return;
    olek.muudatused = { ...olek.muudatused, ...üleriigiliseToetuseMuudatused(toetus, erakond, väärtus) };
    olek.tegelikudHääled = false;
  }

  function muudaRingkonda(lühend: string, protsendid: number[], j: number, väärtus: number) {
    if (!Number.isFinite(väärtus)) return;
    const uus = [...protsendid];
    uus[j] = ümarda(väärtus);
    olek.muudatused = { ...olek.muudatused, [lühend]: uus };
    olek.tegelikudHääled = false;
  }

  /** ringkonnaruutude rea laius; ruudus näidatakse kümnendkohta, kui see mahub */
  let ruuduRida = $state(0);
  const kümnendkohti = $derived((ruuduRida - 11 * 2) / 12 >= 36 ? 1 : 0);

  /** kleepuva päise kaardi laius: kitsal paneelil väiksem, et päis ei võtaks liiga palju kõrgust */
  const kaardiLaius = $derived(ruuduRida > 0 && ruuduRida < 360 ? 132 : 190);

  /** ruudu värv: erakonna värv seda tugevamalt, mida lähemal on toetus erakonna tugevaimale ringkonnale */
  function ruuduStiil(värv: string, väärtus: number, suurim: number): { taust: string; tekst: string } {
    if (suurim <= 0 || väärtus <= 0) return { taust: 'var(--pind-2)', tekst: 'var(--tekst-3)' };
    const osa = Math.round(14 + 76 * (väärtus / suurim));
    return {
      taust: `color-mix(in oklab, ${värv} ${osa}%, var(--pind))`,
      tekst: osa >= 58 ? kontrastTekst(värv) : 'var(--tekst)',
    };
  }

  function lahtriMuutus(sündmus: Event & { currentTarget: HTMLInputElement }, lühend: string, protsendid: number[], j: number) {
    const väärtus = Number(sündmus.currentTarget.value.replace(',', '.').trim());
    if (!Number.isFinite(väärtus) || väärtus < 0 || väärtus > 100) {
      sündmus.currentTarget.value = arv(protsendid[j], kümnendkohti);
      return;
    }
    muudaRingkonda(lühend, protsendid, j, väärtus);
  }

  function lahtriKlahv(sündmus: KeyboardEvent & { currentTarget: HTMLInputElement }, protsendid: number[], j: number) {
    if (sündmus.key === 'Enter') sündmus.currentTarget.blur();
    if (sündmus.key === 'Escape') {
      sündmus.currentTarget.value = arv(protsendid[j], kümnendkohti);
      sündmus.currentTarget.blur();
    }
  }

  function taasta() {
    olek.muudatused = {};
  }
</script>

<div class="päis">
  <h2 id="sisend-pealkiri">{t('sisend.pealkiri')}</h2>
  <button class="taasta" onclick={taasta} disabled={!muudetud} data-testid="taasta">{t('sisend.taasta')}</button>
</div>
<p class="vaigne vaike">{t('sisend.selgitus')}</p>

{#snippet silt(j: number)}
  <!-- ringkonna nimi ruudu kohal; servades joondatud, et see ei ulatuks paneelist välja -->
  <span
    class="ruudu-silt"
    class:vasak={j < 3}
    class:parem={j > 8}
    style:left="{((j + 0.5) / 12) * 100}%"
    aria-hidden="true"
    data-testid="ruudu-silt"
  >
    <span class="nimi-tekst">{RINGKONNAD[j].nr}. {RINGKONNAD[j].nimetus}</span>
  </span>
{/snippet}

<div
  class="tabel"
  role="presentation"
  onpointerover={valiRuut}
  onpointerleave={tühjenda}
  onfocusin={valiRuut}
  onfocusout={tühjenda}
>
  <!-- kleepuv päis: ringkondade kaart ja veergude numbrid jäävad erakondade ridu kerides nähtavaks -->
  <div class="kaardipea" data-testid="toetuse-kaart">
    <div class="kaardirida">
      <Minikaart ringkond={aktiivne === null ? null : aktiivne + 1} silt={t('sisend.kaart')} laius={kaardiLaius} />
      <div class="kaardiinfo">
        <span class="pealdis">{t('sisend.ringkonnad')}</span>
        <span class="ringkonna-nimi" class:tühi={aktiivne === null} data-testid="ringkonna-nimi">
          {aktiivne === null ? t('sisend.kaart.vihje') : `${RINGKONNAD[aktiivne].nr}. ${RINGKONNAD[aktiivne].nimetus}`}
        </span>
      </div>
    </div>
  <div class="rida pea vaike" aria-hidden="true">
    <span class="ruudud-pea" bind:clientWidth={ruuduRida}>
      <span class="numbrid">
        {#each RINGKONNAD as r, j (r.nr)}
          <span title={r.nimetus} data-ringkond={j} class:vigane={muud[j] < 0} class:aktiivne={aktiivne === j}>{r.nr}</span>
        {/each}
      </span>
    </span>
  </div>
  </div>

  {#each read as rida (rida.erakond)}
    {@const piir = Math.max(LIUGURI_PIIR, Math.ceil(rida.üleriigiline + 5))}
    {@const suurim = Math.max(...rida.protsendid)}
    <div class="rida erakond" class:alla-künnise={rida.üleriigiline < KÜNNIS}>
      <label for="liugur-{rida.info.lühend}" class="nimi">
        <span class="varv" style:background={rida.info.värv}></span>
        <span class="lühend">{rida.info.lühend}</span>
        <span class="täisnimi vaike vaigne">{rida.erakond}</span>
      </label>
      <output for="liugur-{rida.info.lühend}" class="väärtus">{protsent(rida.üleriigiline)}</output>
      <div class="rada" style:--künnis={KÜNNIS / piir}>
        <input
          id="liugur-{rida.info.lühend}"
          type="range"
          min="0"
          max={piir}
          step="0.1"
          value={ümarda(rida.üleriigiline)}
          oninput={(e) => muudaÜleriigilist(rida.erakond, Number(e.currentTarget.value))}
          style:--värv={rida.info.värv}
          style:--täide="{(ümarda(rida.üleriigiline) / piir) * 100}%"
          aria-describedby="künnise-selgitus"
          data-testid="liugur-{rida.info.lühend}"
        />
        <span class="künnis" aria-hidden="true"></span>
      </div>
      <div class="ruudud" role="group" aria-label={t('sisend.ruudud', { erakond: rida.info.lühend })}>
        {#if aktiivneRida === rida.info.lühend && aktiivne !== null}{@render silt(aktiivne)}{/if}
        {#each rida.protsendid as väärtus, j}
          {@const stiil = ruuduStiil(rida.info.värv, väärtus, suurim)}
          <input
            class="ruut"
            class:vigane={muud[j] < 0}
            class:aktiivne={aktiivne === j}
            data-ringkond={j}
            data-rida={rida.info.lühend}
            type="text"
            inputmode="decimal"
            value={arv(väärtus, kümnendkohti)}
            style:background={stiil.taust}
            style:color={stiil.tekst}
            title={`${RINGKONNAD[j].nr}. ${RINGKONNAD[j].nimetus}: ${protsent(väärtus)}`}
            aria-label={t('sisend.lahter', { erakond: rida.info.lühend, ringkond: `${j + 1} (${RINGKONNAD[j].nimetus})` })}
            onfocus={(e) => {
              // täpne väärtus ja kogu tekst valitud, et kirjutamine asendaks selle
              e.currentTarget.value = arv(väärtus, 1);
              e.currentTarget.select();
            }}
            onblur={(e) => (e.currentTarget.value = arv(väärtus, kümnendkohti))}
            onchange={(e) => lahtriMuutus(e, rida.info.lühend, rida.protsendid, j)}
            onkeydown={(e) => lahtriKlahv(e, rida.protsendid, j)}
            data-testid="ruut-{rida.info.lühend}-{j + 1}"
          />
        {/each}
      </div>
    </div>
  {/each}

  <div class="rida erakond muud" data-testid="muud">
    <span class="nimi" title={t('sisend.muud.selgitus')}>
      <span class="varv muu-varv"></span>
      <span class="lühend">{t('sisend.muud')}</span>
      <span class="täisnimi vaike vaigne">{t('sisend.muud.täis')}</span>
    </span>
    <span class="väärtus" class:vigane-tekst={muudÜleriigiliselt < 0}>{protsent(muudÜleriigiliselt)}</span>
    <div class="ruudud" role="group" aria-label={t('sisend.ruudud', { erakond: t('sisend.muud') })}>
      {#if aktiivneRida === 'muud' && aktiivne !== null}{@render silt(aktiivne)}{/if}
      {#each muud as väärtus, j}
        <span
          class="ruut muu"
          class:vigane={väärtus < 0}
          class:aktiivne={aktiivne === j}
          data-ringkond={j}
          data-rida="muud"
          title={`${RINGKONNAD[j].nr}. ${RINGKONNAD[j].nimetus}: ${protsent(väärtus)}`}
          data-testid="muud-{j + 1}"
        >{arv(väärtus, kümnendkohti)}</span>
      {/each}
    </div>
  </div>
</div>

{#if vead.length > 0}
  <p class="vaike veateade" role="alert">{t('sisend.liigaPalju')}</p>
{/if}

<p id="künnise-selgitus" class="vaike vaigne märkus">{t('sisend.künnis')}</p>
<p class="vaike vaigne märkus">{t('sisend.ruudud.selgitus')}</p>

<style>
  .päis {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    justify-content: space-between;
    gap: 0.3rem 1rem;
  }

  .taasta {
    font-size: 0.82rem;
    padding: 0.2em 0.7em;
  }

  .tabel {
    margin-top: 0.9rem;
    display: grid;
    gap: 0.15rem;
  }

  /* lai paneel: nimi | liugur | väärtus, ringkonnaruudud liuguri all samas veerus */
  .rida {
    display: grid;
    grid-template-columns: 9.5rem minmax(0, 1fr) 3.6rem;
    grid-template-areas:
      'nimi rada väärtus'
      '. ruudud ruudud';
    column-gap: 0.8rem;
    align-items: center;
  }

  .rida.erakond {
    padding: 0.45rem 0 0.55rem;
    border-top: 1px solid var(--pind-3);
  }

  .pea {
    grid-template-areas: '. ruudud ruudud';
  }

  .ruudud-pea {
    grid-area: ruudud;
    display: grid;
    gap: 0.15rem;
  }

  .pealdis {
    font-size: 0.7rem;
    font-weight: 600;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: var(--tekst-3);
  }

  .numbrid,
  .ruudud {
    grid-area: ruudud;
    display: grid;
    grid-template-columns: repeat(12, minmax(0, 1fr));
    gap: 2px;
    text-align: center;
  }

  .numbrid span {
    color: var(--tekst-3);
    font-variant-numeric: tabular-nums;
    font-size: 0.72rem;
  }

  .kaardipea {
    position: sticky;
    top: var(--riba-kõrgus);
    z-index: 2;
    background: var(--pind);
    padding: 0.35rem 0 0.15rem;
    border-bottom: 1px solid var(--joon);
  }

  .kaardirida {
    display: flex;
    align-items: center;
    gap: 0.9rem;
    margin-bottom: 0.3rem;
  }

  .kaardiinfo {
    display: grid;
    gap: 0.2rem;
    min-width: 0;
  }

  .ringkonna-nimi {
    font-family: var(--serif);
    font-size: 1.05rem;
    font-weight: 600;
    line-height: 1.25;
  }

  .ringkonna-nimi.tühi {
    font-family: var(--sans);
    font-size: 0.78rem;
    font-weight: 400;
    color: var(--tekst-3);
  }

  .numbrid span.aktiivne {
    color: var(--tekst);
    font-weight: 700;
  }

  .ruudud {
    position: relative;
  }

  .ruudu-silt {
    position: absolute;
    bottom: calc(100% + 4px);
    transform: translateX(-50%);
    z-index: 3;
    display: grid;
    justify-items: start;
    gap: 0.3rem;
    width: max-content;
    max-width: min(100%, 15rem);
    padding: 0.4rem 0.5rem 0.35rem;
    border-radius: 6px;
    background: var(--tekst);
    color: var(--paber);
    box-shadow: 0 4px 14px rgb(0 0 0 / 0.25);
    font-size: 0.72rem;
    font-weight: 600;
    pointer-events: none;
  }

  .nimi-tekst {
    max-width: 100%;
    white-space: normal;
    line-height: 1.25;
  }

  .ruudu-silt.vasak {
    transform: none;
    left: 0 !important;
  }

  .ruudu-silt.parem {
    transform: none;
    left: auto !important;
    right: 0;
  }

  .ruut.aktiivne {
    box-shadow: inset 0 0 0 1.5px var(--tekst);
  }

  .muud {
    border-top: 1px solid var(--joon) !important;
  }

  .muu-varv {
    background: repeating-linear-gradient(45deg, var(--tekst-3) 0 2px, transparent 2px 4px);
    border: 1px solid var(--tekst-3);
  }

  .ruut.muu {
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--pind-2);
    color: var(--tekst-2);
    cursor: default;
  }

  .vigane-tekst {
    color: var(--viga);
  }

  .numbrid .vigane {
    color: var(--viga);
    font-weight: 700;
  }

  .nimi {
    grid-area: nimi;
    display: flex;
    align-items: baseline;
    gap: 0.35rem;
    min-width: 0;
  }

  .lühend {
    font-weight: 650;
  }

  .täisnimi {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .väärtus {
    grid-area: väärtus;
    text-align: right;
    font-family: var(--serif);
    font-size: 1.15rem;
    font-weight: 600;
  }

  .alla-künnise .väärtus,
  .alla-künnise .lühend {
    color: var(--tekst-3);
  }

  .rada {
    grid-area: rada;
    position: relative;
    min-width: 0;
  }

  .ruudud {
    margin-top: 0.4rem;
  }

  .ruut {
    width: 100%;
    min-width: 0;
    height: 26px;
    padding: 0;
    border: none;
    border-radius: 3px;
    text-align: center;
    font-size: 0.72rem;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    cursor: text;
  }

  .ruut:hover {
    box-shadow: inset 0 0 0 1.5px var(--tekst-3);
  }

  .ruut:focus {
    outline: 2px solid var(--rohk);
    outline-offset: 1px;
    background: var(--pind) !important;
    color: var(--tekst) !important;
  }

  .ruut.vigane {
    box-shadow: inset 0 0 0 2px var(--viga);
  }

  /* liugur: raja täidetud osa erakonna värviga, nupul pinnavärvi rõngas */
  .rada input {
    width: 100%;
    margin: 0.45rem 0;
    height: 6px;
    border-radius: 3px;
    appearance: none;
    background: linear-gradient(to right, var(--värv) var(--täide), var(--pind-3) var(--täide));
    cursor: pointer;
  }

  .rada input::-webkit-slider-thumb {
    appearance: none;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: var(--värv);
    border: 3px solid var(--pind);
    box-shadow: 0 0 0 1px var(--tekst-3), 0 1px 3px rgb(0 0 0 / 0.2);
  }

  .rada input::-moz-range-thumb {
    width: 13px;
    height: 13px;
    border-radius: 50%;
    background: var(--värv);
    border: 3px solid var(--pind);
    box-shadow: 0 0 0 1px var(--tekst-3);
  }

  /* 5% künnise märk liuguri raja all; liuguri nupu keskpunkt liigub vahemikus 9px … laius − 9px */
  .künnis {
    position: absolute;
    left: calc(9px + (100% - 18px) * var(--künnis) - 1px);
    bottom: -3px;
    width: 2px;
    height: 6px;
    background: var(--tekst-3);
    pointer-events: none;
  }

  .märkus {
    margin: 0.6rem 0 0;
  }

  .veateade {
    color: var(--viga);
    font-weight: 600;
    margin: 0.6rem 0 0;
  }

  button:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }

  /* kitsas paneel: nimi ja väärtus ühel real, liugur ja ruudud kogu laiuses */
  @container kaart (max-width: 600px) {
    .rida {
      grid-template-columns: minmax(0, 1fr) auto;
      grid-template-areas:
        'nimi väärtus'
        'rada rada'
        'ruudud ruudud';
    }

    .pea {
      grid-template-areas: 'ruudud ruudud';
    }

    .ruut {
      height: 24px;
      font-size: 0.68rem;
    }

    .numbrid span {
      font-size: 0.64rem;
      letter-spacing: -0.02em;
    }
  }
</style>
