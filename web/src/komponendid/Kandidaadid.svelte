<script lang="ts">
  import { erakonnaInfo, järjekorraNr } from '../lib/erakonnad';
  import { arv, protsent, t } from '../lib/i18n.svelte';
  import { mudeliArvud } from '../lib/mudel';
  import type { Olek } from '../lib/olek';
  import type { Kokkuvõte } from '../lib/protokoll';
  import { RINGKONNAD, type Tulemus } from '../lib/simulaator';

  let { tulemus, kokkuvõte, olek = $bindable() }: { tulemus: Tulemus; kokkuvõte: Kokkuvõte | null; olek: Olek } = $props();

  const tegelikudHääled = $derived(olek.tegelikudHääled);

  let erakond = $state('');
  let ringkond = $state(0);
  let näitaKõiki = $state(false);
  /** ainult kandidaadid, kes valiti 5–95% kordustest */
  let ainultPiiril = $state(false);
  const PIIRIL = [0.05, 0.95];
  const piiril = (r: { sagedus: number | null }) => r.sagedus !== null && r.sagedus >= PIIRIL[0] && r.sagedus <= PIIRIL[1];
  const PIIR = 60;

  interface Rida {
    võti: string;
    nimi: string;
    erakond: string;
    ringkond: number;
    hääli: number | null;
    mandaat: string | null;
    sagedus: number | null;
  }

  const read = $derived.by((): Rida[] => {
    const kaart = new Map<string, Rida>();
    const võti = (nimi: string, e: string, r: number) => `${nimi}|${e}|${r}`;
    for (const k of tulemus.valitud) {
      kaart.set(võti(k.nimi, k.erakond, k.ringkond), {
        võti: võti(k.nimi, k.erakond, k.ringkond), nimi: k.nimi, erakond: k.erakond, ringkond: k.ringkond,
        hääli: k.hääli, mandaat: k.mandaat ?? null, sagedus: null,
      });
    }
    if (kokkuvõte && !tegelikudHääled) {
      for (const k of kokkuvõte.kandidaadid) {
        const v = võti(k.nimi, k.erakond, k.ringkond);
        const rida = kaart.get(v) ?? { võti: v, nimi: k.nimi, erakond: k.erakond, ringkond: k.ringkond, hääli: null, mandaat: null, sagedus: null };
        rida.sagedus = k.kordi / kokkuvõte.kordi;
        kaart.set(v, rida);
      }
    }
    return [...kaart.values()]
      .filter((r) => (erakond === '' || r.erakond === erakond) && (ringkond === 0 || r.ringkond === ringkond))
      .filter((r) => !ainultPiiril || tegelikudHääled || piiril(r))
      .sort((a, b) => (b.sagedus ?? (b.mandaat ? 1 : 0)) - (a.sagedus ?? (a.mandaat ? 1 : 0)) ||
        (a.mandaat ? 0 : 1) - (b.mandaat ? 0 : 1) || a.nimi.localeCompare(b.nimi, 'et'));
  });

  const erakonnad = $derived(
    [...new Set(tulemus.valitud.map((k) => k.erakond).concat(kokkuvõte?.erakonnad ?? []))].sort((a, b) => järjekorraNr(a) - järjekorraNr(b)),
  );
  /** kokkuvõte valiku kohta (enne piiril-filtrit) */
  const loendus = $derived.by(() => {
    if (!kokkuvõte || tegelikudHääled) return null;
    const valik = kokkuvõte.kandidaadid
      .filter((k) => (erakond === '' || k.erakond === erakond) && (ringkond === 0 || k.ringkond === ringkond))
      .map((k) => k.kordi / kokkuvõte.kordi);
    return {
      kindlad: valik.filter((s) => s > PIIRIL[1]).length,
      piiril: valik.filter((s) => s >= PIIRIL[0] && s <= PIIRIL[1]).length,
    };
  });

  const nähtavad = $derived(näitaKõiki ? read : read.slice(0, PIIR));
</script>

<h2 id="kandidaadid-pealkiri">{t('kandidaadid.pealkiri')}</h2>
<p class="vaike vaigne">{tegelikudHääled ? t('kandidaadid.selgitus.tegelikud') : t('kandidaadid.selgitus')}</p>

{#if !tegelikudHääled}
  <div class="jaotus vaike">
    <span class="vaigne" id="jaotuse-silt">{t('kandidaadid.jaotus')}</span>
    <div class="segmendid" role="radiogroup" aria-labelledby="jaotuse-silt">
      {#each ['kalibreeritud', 'lihtne'] as const as j (j)}
        <label class:valitud={olek.jaotus === j}>
          <input type="radio" name="häälte-jaotus" value={j} bind:group={olek.jaotus} data-testid="häälte-jaotus-{j}" />
          {t(`kandidaadid.jaotus.${j}`)}
        </label>
      {/each}
    </div>
  </div>
  <details class="vaike jaotuse-selgitus">
    <summary>{t('kandidaadid.jaotus.kuidas')}</summary>
    <p class="vaigne">
      {t(`kandidaadid.jaotus.selgitus.${olek.jaotus}`, {
        esimene: mudeliArvud.esimene,
        esimeneEsiviisikus: mudeliArvud.esimeneEsiviisikus,
        kohti: mudeliArvud.kohti,
        kalibreeritud: arv(mudeliArvud.kalibreeritud, 1),
        lihtne: arv(mudeliArvud.lihtne, 1),
      })}
    </p>
  </details>
{/if}

{#if loendus}
  <p class="loendus" data-testid="kandidaatide-loendus">
    {t('kandidaadid.loendus', { kindlad: loendus.kindlad, piiril: loendus.piiril })}
  </p>
{/if}

<div class="filtrid">
  <label class="vaike">
    <span class="vaigne">{t('erakond')}</span>
    <select bind:value={erakond}>
      <option value="">{t('kõik')}</option>
      {#each erakonnad as e}<option value={e}>{erakonnaInfo(e).lühend}</option>{/each}
    </select>
  </label>
  <label class="vaike">
    <span class="vaigne">{t('ringkond')}</span>
    <select bind:value={ringkond}>
      <option value={0}>{t('kõik')}</option>
      {#each RINGKONNAD as r (r.nr)}<option value={r.nr}>{r.nr}. {r.nimetus}</option>{/each}
    </select>
  </label>
  {#if loendus}
    <label class="vaike piiril" class:valitud={ainultPiiril}>
      <input type="checkbox" bind:checked={ainultPiiril} data-testid="ainult-piiril" />
      {t('kandidaadid.ainultPiiril', { arv: loendus.piiril })}
    </label>
  {/if}
</div>

<div class="tabeli-ümbris" class:kitsas={!tegelikudHääled}>
  <table data-testid="kandidaatide-tabel">
    <thead>
      {#if !tegelikudHääled}
        <tr class="rühmad peidetav">
          <td colspan="4"></td>
          <th scope="colgroup" colspan="2" class="rühm">{t('kandidaadid.näidiskordus', { seeme: olek.seeme })}</th>
        </tr>
      {/if}
      <tr>
        <th scope="col">{t('kandidaadid.nimi')}</th>
        <th scope="col">{t('erakond')}</th>
        <th scope="col" class="arv ringkonna-veerg">{t('ringkond')}</th>
        {#if !tegelikudHääled}<th scope="col" class="arv">{t('kandidaadid.sagedus')}</th>{/if}
        <th scope="col" class="arv" class:peidetav={!tegelikudHääled}>{t('kandidaadid.hääled')}</th>
        <th scope="col" class:peidetav={!tegelikudHääled}>{t('kandidaadid.mandaat')}</th>
      </tr>
    </thead>
    <tbody>
      {#each nähtavad as r (r.võti)}
        <tr class:valimata={!r.mandaat}>
          <th scope="row">{r.nimi}</th>
          <td class="erakonna-lahter">
            <span class="varv" style:background={erakonnaInfo(r.erakond).värv}></span>{erakonnaInfo(r.erakond).lühend}<span
              class="ringkond-lühike vaigne"
              title={RINGKONNAD[r.ringkond - 1]?.nimetus}>{t('kandidaadid.ringkonnas', { nr: r.ringkond })}</span
            >
          </td>
          <td class="arv ringkonna-veerg" title={RINGKONNAD[r.ringkond - 1]?.nimetus}>{r.ringkond}</td>
          {#if !tegelikudHääled}
            <td class="arv sagedus">
              {#if r.sagedus !== null}
                <span class="riba" style:--osa={r.sagedus} style:--värv={erakonnaInfo(r.erakond).värv}></span>
                {protsent(r.sagedus * 100, 0)}
              {:else if kokkuvõte}{protsent(0, 0)}{:else}…{/if}
            </td>
          {/if}
          <td class="arv näidisveerg" class:peidetav={!tegelikudHääled}>{r.hääli === null ? '' : arv(r.hääli)}</td>
          <td class="näidisveerg" class:peidetav={!tegelikudHääled}>{r.mandaat ? t(`mandaat.${r.mandaat}`) : ''}</td>
        </tr>
      {/each}
    </tbody>
  </table>
</div>
{#if read.length > PIIR}
  <button class="rohkem" onclick={() => (näitaKõiki = !näitaKõiki)}>
    {näitaKõiki ? t('kandidaadid.vähem') : t('kandidaadid.kõik', { arv: read.length })}
  </button>
{/if}

<style>
  .jaotus {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.4rem 0.7rem;
    margin: 0.6rem 0 0.2rem;
  }

  .segmendid {
    display: inline-flex;
    border: 1px solid var(--joon);
    border-radius: 8px;
    overflow: hidden;
  }

  .segmendid label {
    padding: 0.3rem 0.75rem;
    cursor: pointer;
    color: var(--tekst-2);
  }

  .segmendid label + label {
    border-left: 1px solid var(--joon);
  }

  .segmendid label.valitud {
    background: var(--rohk-pind);
    color: var(--tekst);
    font-weight: 600;
  }

  .segmendid input {
    position: absolute;
    opacity: 0;
    pointer-events: none;
  }

  .segmendid label:has(input:focus-visible) {
    outline: 2px solid var(--rohk);
    outline-offset: -2px;
  }

  .jaotuse-selgitus {
    margin-bottom: 0.6rem;
  }

  .jaotuse-selgitus summary {
    cursor: pointer;
    color: var(--tekst-2);
  }

  .jaotuse-selgitus p {
    margin: 0.3rem 0 0;
  }

  .loendus {
    margin: 0.4rem 0 0.6rem;
  }

  .piiril {
    display: flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.2rem 0.65rem 0.2rem 0.45rem;
    border: 1px solid var(--joon);
    border-radius: 999px;
    cursor: pointer;
  }

  .piiril.valitud {
    border-color: var(--rohk);
    background: var(--rohk-pind);
  }

  .piiril input {
    margin: 0;
  }

  .filtrid {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem 1rem;
    margin-bottom: 0.5rem;
  }

  .filtrid label {
    min-width: 0;
    max-width: 100%;
  }

  .filtrid select {
    min-width: 0;
    max-width: 100%;
  }

  table {
    width: 100%;
    font-size: 0.88rem;
  }

  th[scope='row'] {
    font-weight: 500;
    color: var(--tekst);
  }

  tr.valimata th,
  tr.valimata td {
    color: var(--tekst-2);
  }

  /* kitsas kaart: näidiskorduse veerud peidetakse, sagedus jääb nähtavaks */
  @container kaart (max-width: 520px) {
    .peidetav {
      display: none;
    }

    th[scope='row'] {
      overflow-wrap: break-word;
    }

    .sagedus .riba {
      display: none;
    }

    .ringkonna-veerg {
      display: none;
    }

    .erakonna-lahter .ringkond-lühike {
      display: block;
      font-size: 0.8rem;
    }
  }

  /* laial kaardil kõik veerud, kitsal keritakse ainult siis, kui näidiskorduse veerud on nähtavad */
  @container kaart (min-width: 521px) {
    .tabeli-ümbris.kitsas > table {
      min-width: 34rem;
    }
  }

  .erakonna-lahter {
    white-space: nowrap;
  }

  .ringkond-lühike {
    display: none;
  }

  .rühmad td,
  .rühmad th {
    border-bottom: none;
    padding-bottom: 0;
  }

  .rühm {
    text-align: left;
    font-weight: 400;
    font-size: 0.8rem;
    color: var(--tekst-3);
  }

  .sagedus {
    font-weight: 600;
  }

  td.näidisveerg {
    color: var(--tekst-2);
  }

  .riba {
    display: inline-block;
    width: 48px;
    height: 6px;
    border-radius: 3px;
    background: linear-gradient(to right, var(--värv) calc(var(--osa) * 100%), var(--pind-3) 0);
    margin-right: 0.4em;
    vertical-align: middle;
  }

  .rohkem {
    margin-top: 0.5rem;
  }
</style>
