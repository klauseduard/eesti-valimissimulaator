<script lang="ts">
  // Ringkonnast valitud kandidaatide keskmine arv erakondade kaupa (ka kompensatsioonimandaadid), ühisel
  // skaalal virnastatud tulpadena. Kaart näitab ainult iga ringkonna suurimat erakonda; see diagramm näitab,
  // kuidas proportsionaalne süsteem kohad ringkonnas tegelikult jagab.
  import { erakonnaInfo, järjekorraNr, kontrastTekst } from '../lib/erakonnad';
  import { arv, t } from '../lib/i18n.svelte';
  import type { Olek } from '../lib/olek';
  import type { Kokkuvõte } from '../lib/protokoll';
  import { RINGKONNAD, type Tulemus } from '../lib/simulaator';

  let {
    tulemus,
    kokkuvõte,
    kokkuvõteKehtib,
    olek = $bindable(),
  }: { tulemus: Tulemus; kokkuvõte: Kokkuvõte | null; kokkuvõteKehtib: boolean; olek: Olek } = $props();

  /** ringkond -> erakond -> (keskmine) valitute arv */
  const andmed = $derived.by((): Map<number, Map<string, number>> | null => {
    const kaart = new Map<number, Map<string, number>>();
    if (olek.tegelikudHääled) {
      for (const k of tulemus.valitud) {
        const e = kaart.get(k.ringkond) ?? new Map<string, number>();
        e.set(k.erakond, (e.get(k.erakond) ?? 0) + 1);
        kaart.set(k.ringkond, e);
      }
      return kaart;
    }
    if (!kokkuvõte || kokkuvõte.kordi === 0) return null;
    kokkuvõte.ringkondadeValitud.forEach((read, r) => {
      kaart.set(
        r + 1,
        new Map(
          kokkuvõte.erakonnad
            .map((e, i) => [e, read[i].reduce((a, b) => a + b, 0) / read[i].length] as [string, number])
            .filter(([, m]) => m >= 0.05),
        ),
      );
    });
    return kaart;
  });

  const erakonnad = $derived(
    andmed === null
      ? []
      : [...new Set([...andmed.values()].flatMap((m) => [...m.keys()]))].sort((a, b) => järjekorraNr(a) - järjekorraNr(b)),
  );
  const skaala = $derived(andmed === null ? 1 : Math.max(1, ...[...andmed.values()].map((m) => [...m.values()].reduce((a, b) => a + b, 0))));

  /** lõigu number näidatakse ainult siis, kui lõik on vähemalt nii mitu pikslit lai (et number mahuks) */
  const SILDI_MIN_LAIUS = 30;
  /** tulpade ala laius pikslites (mõõdetud esimese rea järgi) */
  let tulbaAla = $state(0);


  const mandaate = (m: number) => (olek.tegelikudHääled ? String(m) : arv(m, 1));
</script>

<h2 id="ringkonnad-pealkiri">{t('ringkonnad.pealkiri')}</h2>
<p class="vaike vaigne">{olek.tegelikudHääled ? t('ringkonnad.selgitus.tegelikud') : t('ringkonnad.selgitus')}</p>

{#if andmed === null}
  <p class="vaigne">{t('parlament.alus.arvutan')}</p>
{:else}
  <ul class="legend vaike" aria-label={t('legend')}>
    {#each erakonnad as e (e)}
      <li><span class="varv" style:background={erakonnaInfo(e).värv}></span>{erakonnaInfo(e).lühend}</li>
    {/each}
  </ul>

  <ol class="read" class:uueneb={!olek.tegelikudHääled && !kokkuvõteKehtib} data-testid="ringkondade-diagramm">
    {#each RINGKONNAD as r (r.nr)}
      {@const m = andmed.get(r.nr) ?? new Map<string, number>()}
      {@const kokku = [...m.values()].reduce((a, b) => a + b, 0)}
      <li>
        <button
          class="rida"
          class:valitud={olek.ringkond === r.nr}
          onclick={() => (olek.ringkond = r.nr)}
          aria-label={`${r.nr}. ${r.nimetus}: ` + erakonnad.filter((e) => m.has(e)).map((e) => `${erakonnaInfo(e).lühend} ${mandaate(m.get(e)!)}`).join(', ')}
        >
          <span class="nimi"><span class="nr">{r.nr}</span>{r.nimetus}</span>
          <span class="tulp-ala" bind:clientWidth={tulbaAla}>
          <span class="tulp" style:width="{(kokku / skaala) * 100}%">
            {#each erakonnad.filter((e) => (m.get(e) ?? 0) > 0) as e (e)}
              {@const osa = ((m.get(e) ?? 0) / kokku) * 100}
              {@const värv = erakonnaInfo(e).värv}
              <span class="lõik" style:flex-grow={m.get(e)} style:background={värv} title={`${erakonnaInfo(e).lühend} ${mandaate(m.get(e)!)}`}>
                {#if ((osa / 100) * kokku * tulbaAla) / skaala >= SILDI_MIN_LAIUS}
                  <span class="arv" style:color={kontrastTekst(värv)}>{mandaate(m.get(e)!)}</span>
                {/if}
              </span>
            {/each}
          </span>
          </span>
          <span class="kokku vaike vaigne">{mandaate(kokku)}</span>
        </button>
      </li>
    {/each}
  </ol>
{/if}

<style>
  .legend {
    display: flex;
    flex-wrap: wrap;
    gap: 0.2rem 0.9rem;
    list-style: none;
    padding: 0;
    margin: 0.2rem 0 0.6rem;
    color: var(--tekst-2);
  }

  .read {
    list-style: none;
    padding: 0;
    margin: 0;
    display: grid;
    gap: 2px;
    transition: opacity 0.2s;
  }

  .uueneb {
    opacity: 0.5;
  }

  .rida {
    width: 100%;
    display: grid;
    grid-template-columns: minmax(6rem, 10rem) minmax(0, 1fr) 2.5rem;
    gap: 0.6rem;
    align-items: center;
    background: none;
    border: none;
    border-radius: 6px;
    padding: 0.2rem 0.3rem;
    text-align: left;
    cursor: pointer;
  }

  .rida:hover,
  .rida.valitud {
    background: var(--pind-2);
  }

  .rida.valitud .nimi {
    font-weight: 600;
  }

  .nimi {
    font-size: 0.85rem;
    color: var(--tekst-2);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .nr {
    display: inline-block;
    width: 1.5em;
    color: var(--tekst-3);
    font-variant-numeric: tabular-nums;
  }

  .tulp-ala {
    display: block;
    min-width: 0;
  }

  .tulp {
    display: flex;
    gap: 2px;
    height: 20px;
  }

  .lõik {
    flex-basis: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 2px;
    border-radius: 2px;
    overflow: hidden;
  }

  .arv {
    font-size: 0.72rem;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }

  .kokku {
    text-align: right;
    font-variant-numeric: tabular-nums;
  }
</style>
