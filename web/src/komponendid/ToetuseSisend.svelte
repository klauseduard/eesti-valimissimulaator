<script lang="ts">
  import { erakonnaInfo, järjekorraNr, kontrastTekst } from '../lib/erakonnad';
  import { arv, protsent, t } from '../lib/i18n.svelte';
  import { ümarda, type Olek } from '../lib/olek';
  import type { ToetuseAndmed } from '../lib/protokoll';
  import { RINGKONNAD, üleriigilineToetus, üleriigiliseToetuseMuudatused } from '../lib/simulaator';

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

<div class="tabel">
  <div class="rida pea vaike" aria-hidden="true">
    <span class="ruudud-pea" bind:clientWidth={ruuduRida}>
      <span class="pealdis">{t('sisend.ringkonnad')}</span>
      <span class="numbrid">
        {#each RINGKONNAD as r (r.nr)}<span title={r.nimetus} class:vigane={summad[r.nr - 1] > 100}>{r.nr}</span>{/each}
      </span>
    </span>
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
        {#each rida.protsendid as väärtus, j}
          {@const stiil = ruuduStiil(rida.info.värv, väärtus, suurim)}
          <input
            class="ruut"
            class:vigane={summad[j] > 100}
            type="text"
            inputmode="decimal"
            value={arv(väärtus, kümnendkohti)}
            style:background={stiil.taust}
            style:color={stiil.tekst}
            title={`${RINGKONNAD[j].nr}. ${RINGKONNAD[j].nimetus}: ${protsent(väärtus)}`}
            aria-label={t('sisend.lahter', { erakond: rida.info.lühend, ringkond: j + 1 })}
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

  <div class="rida kokku vaike">
    <span class="kokku-ruudud">
      <span class="pealdis">{t('sisend.kokku')}</span>
      <span class="numbrid">
        {#each summad as summa, j}<span class:vigane={summa > 100} title={`${RINGKONNAD[j].nimetus}: ${protsent(summa)}`}>{arv(summa, 0)}</span>{/each}
      </span>
    </span>
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

  .pea,
  .kokku {
    grid-template-areas: '. ruudud ruudud';
  }

  .ruudud-pea,
  .kokku-ruudud {
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

  .kokku {
    border-top: 1px solid var(--joon);
    padding-top: 0.35rem;
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

    .pea,
    .kokku {
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
