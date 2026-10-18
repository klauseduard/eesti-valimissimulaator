<script lang="ts">
  import { erakonnaInfo, järjekorraNr } from '../lib/erakonnad';
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

  function taasta() {
    olek.muudatused = {};
  }
</script>

<h2>{t('sisend.pealkiri')}</h2>
<p class="vaigne vaike">{t('sisend.selgitus')}</p>

<div class="liugurid">
  {#each read as rida (rida.erakond)}
    {@const piir = Math.max(LIUGURI_PIIR, Math.ceil(rida.üleriigiline + 5))}
    <div class="liugur" class:alla-künnise={rida.üleriigiline < KÜNNIS}>
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
    </div>
  {/each}
</div>

<p id="künnise-selgitus" class="vaike vaigne">{t('sisend.künnis')}</p>

<div class="nupud">
  <button onclick={taasta} disabled={!muudetud} data-testid="taasta">{t('sisend.taasta')}</button>
</div>

<details>
  <summary>{t('sisend.ringkonnad')}</summary>
  <p class="vaike vaigne">{t('sisend.ringkonnad.selgitus')}</p>
  <div class="tabeli-ümbris">
    <table class="ringkonnatabel">
      <thead>
        <tr>
          <th scope="col">{t('erakond')}</th>
          {#each RINGKONNAD as ringkond (ringkond.nr)}
            <th scope="col" class="arv" title={ringkond.nimetus}>{ringkond.nr}</th>
          {/each}
        </tr>
      </thead>
      <tbody>
        {#each read as rida (rida.erakond)}
          <tr>
            <th scope="row" title={rida.erakond}><span class="varv" style:background={rida.info.värv}></span>{rida.info.lühend}</th>
            {#each rida.protsendid as väärtus, j}
              <td>
                <input
                  type="number"
                  min="0"
                  max="100"
                  step="0.1"
                  value={ümarda(väärtus)}
                  aria-label={t('sisend.lahter', { erakond: rida.info.lühend, ringkond: j + 1 })}
                  class:vigane={summad[j] > 100}
                  onchange={(e) => muudaRingkonda(rida.info.lühend, rida.protsendid, j, Number(e.currentTarget.value))}
                />
              </td>
            {/each}
          </tr>
        {/each}
      </tbody>
      <tfoot>
        <tr>
          <th scope="row">{t('kokku')}</th>
          {#each summad as summa}
            <td class="arv" class:vigane={summa > 100}>{arv(summa, 1)}</td>
          {/each}
        </tr>
      </tfoot>
    </table>
  </div>
  {#if vead.length > 0}
    <p class="vaike veateade" role="alert">{t('sisend.liigaPalju')}</p>
  {/if}
</details>

<style>
  .liugurid {
    display: grid;
    gap: 0.55rem;
  }

  .liugur {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    grid-template-areas: 'nimi väärtus' 'rada rada';
    column-gap: 0.5rem;
  }

  .nimi {
    grid-area: nimi;
    display: flex;
    align-items: baseline;
    gap: 0.35rem;
    min-width: 0;
  }

  .lühend {
    font-weight: 600;
  }

  .täisnimi {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .väärtus {
    grid-area: väärtus;
    font-variant-numeric: tabular-nums;
  }

  .alla-künnise .väärtus {
    color: var(--tekst-3);
  }

  .rada {
    grid-area: rada;
    position: relative;
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
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: var(--värv);
    border: 2px solid var(--pind);
    box-shadow: 0 0 0 1px var(--tekst-3);
  }

  .rada input::-moz-range-thumb {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: var(--värv);
    border: 2px solid var(--pind);
    box-shadow: 0 0 0 1px var(--tekst-3);
  }

  /* 5% künnise märk liuguri raja all; liuguri nupu keskpunkt liigub vahemikus 8px … laius − 8px */
  .künnis {
    position: absolute;
    left: calc(8px + (100% - 16px) * var(--künnis) - 1px);
    bottom: -3px;
    width: 2px;
    height: 6px;
    background: var(--tekst-3);
    pointer-events: none;
  }

  .nupud {
    margin: 0.6rem 0;
  }

  details {
    margin-top: 0.8rem;
    border-top: 1px solid var(--joon);
    padding-top: 0.6rem;
  }

  summary {
    cursor: pointer;
    font-weight: 600;
  }

  .ringkonnatabel {
    font-size: 0.85rem;
  }

  .ringkonnatabel th,
  .ringkonnatabel td {
    padding: 0.15rem 0.2rem;
  }

  .ringkonnatabel input {
    width: 4.2em;
    padding: 0.1em 0.2em;
    border: 1px solid var(--joon);
    border-radius: 4px;
    background: var(--pind);
    text-align: right;
  }

  .vigane {
    color: var(--viga);
    border-color: var(--viga) !important;
    font-weight: 600;
  }

  .veateade {
    color: var(--viga);
  }

  button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
</style>
