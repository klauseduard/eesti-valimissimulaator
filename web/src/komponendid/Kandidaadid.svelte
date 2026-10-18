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
      .sort((a, b) => (b.sagedus ?? (b.mandaat ? 1 : 0)) - (a.sagedus ?? (a.mandaat ? 1 : 0)) ||
        (a.mandaat ? 0 : 1) - (b.mandaat ? 0 : 1) || a.nimi.localeCompare(b.nimi, 'et'));
  });

  const erakonnad = $derived(
    [...new Set(tulemus.valitud.map((k) => k.erakond).concat(kokkuvõte?.erakonnad ?? []))].sort((a, b) => järjekorraNr(a) - järjekorraNr(b)),
  );
  const nähtavad = $derived(näitaKõiki ? read : read.slice(0, PIIR));
</script>

<h2 id="kandidaadid-pealkiri">{t('kandidaadid.pealkiri')}</h2>
<p class="vaike vaigne">{tegelikudHääled ? t('kandidaadid.selgitus.tegelikud') : t('kandidaadid.selgitus')}</p>

{#if !tegelikudHääled}
  <div class="jaotus">
    <label class="vaike">
      <span class="vaigne">{t('kandidaadid.jaotus')}</span>
      <select bind:value={olek.jaotus} data-testid="häälte-jaotus">
        <option value="kalibreeritud">{t('kandidaadid.jaotus.kalibreeritud')}</option>
        <option value="lihtne">{t('kandidaadid.jaotus.lihtne')}</option>
      </select>
    </label>
    <p class="vaike vaigne">
      {t(`kandidaadid.jaotus.selgitus.${olek.jaotus}`, {
        esimene: mudeliArvud.esimene,
        esimeneEsiviisikus: mudeliArvud.esimeneEsiviisikus,
        kohti: mudeliArvud.kohti,
        kalibreeritud: arv(mudeliArvud.kalibreeritud, 1),
        lihtne: arv(mudeliArvud.lihtne, 1),
      })}
    </p>
  </div>
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
</div>

<div class="tabeli-ümbris">
  <table data-testid="kandidaatide-tabel">
    <thead>
      {#if !tegelikudHääled}
        <tr class="rühmad">
          <td colspan="4"></td>
          <th scope="colgroup" colspan="2" class="rühm">{t('kandidaadid.näidiskordus', { seeme: olek.seeme })}</th>
        </tr>
      {/if}
      <tr>
        <th scope="col">{t('kandidaadid.nimi')}</th>
        <th scope="col">{t('erakond')}</th>
        <th scope="col" class="arv">{t('ringkond')}</th>
        {#if !tegelikudHääled}<th scope="col" class="arv">{t('kandidaadid.sagedus')}</th>{/if}
        <th scope="col" class="arv">{t('kandidaadid.hääled')}</th>
        <th scope="col">{t('kandidaadid.mandaat')}</th>
      </tr>
    </thead>
    <tbody>
      {#each nähtavad as r (r.võti)}
        <tr class:valimata={!r.mandaat}>
          <th scope="row">{r.nimi}</th>
          <td><span class="varv" style:background={erakonnaInfo(r.erakond).värv}></span>{erakonnaInfo(r.erakond).lühend}</td>
          <td class="arv" title={RINGKONNAD[r.ringkond - 1]?.nimetus}>{r.ringkond}</td>
          {#if !tegelikudHääled}
            <td class="arv sagedus">
              {#if r.sagedus !== null}
                <span class="riba" style:--osa={r.sagedus} style:--värv={erakonnaInfo(r.erakond).värv}></span>
                {protsent(r.sagedus * 100, 0)}
              {:else if kokkuvõte}{protsent(0, 0)}{:else}…{/if}
            </td>
          {/if}
          <td class="arv näidisveerg">{r.hääli === null ? '' : arv(r.hääli)}</td>
          <td class="näidisveerg">{r.mandaat ? t(`mandaat.${r.mandaat}`) : ''}</td>
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
    margin: 0.4rem 0 0.8rem;
    padding: 0.5rem 0.75rem;
    border: 1px solid var(--joon);
    border-radius: 8px;
  }

  .jaotus label {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.5rem;
  }

  .jaotus p {
    margin: 0.4rem 0 0;
  }

  .filtrid {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem 1rem;
    margin-bottom: 0.5rem;
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
