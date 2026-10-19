<script lang="ts">
  // Väike Eesti kaart, millel üks ringkond on esile tõstetud; Tallinna kolm ringkonda on lisaks suurendatult
  // vasakul ülal, sest Eesti kaardil on need vaid mõne piksli suurused. Igal ringkonnal on atribuut
  // data-ringkond (0–11), et ümbritsev komponent saaks osutamise järgi ringkonna valida.
  import { onMount } from 'svelte';
  import { geoPath } from 'd3-geo';
  import type { FeatureCollection, Geometry, Polygon } from 'geojson';
  import { laadiRingkonnad, projektsioon, suurimOsa, TALLINN, type Ringkond } from '../lib/ringkonnakaart';

  let { ringkond, silt, laius = 168 }: { ringkond: number | null; silt: string; laius?: number } = $props();

  const LAIUS = $derived(laius);
  const LISA = $derived(Math.round(laius * 0.26));

  let ringkonnad = $state<Ringkond[] | null>(null);
  onMount(() => {
    laadiRingkonnad().then((r) => (ringkonnad = r.ringkonnad)).catch(() => (ringkonnad = null));
  });

  const joonistus = $derived.by(() => {
    if (!ringkonnad) return null;
    const kõik: FeatureCollection<Geometry, { nr: number }> = { type: 'FeatureCollection', features: ringkonnad };
    const põhi = projektsioon().fitWidth(LAIUS - LISA - 8, kõik);
    const [[x0, y0], [, y1]] = geoPath(põhi).bounds(kõik);
    põhi.translate([põhi.translate()[0] - x0 + LISA + 6, põhi.translate()[1] - y0 + 2]);
    const kõrgus = Math.ceil(y1 - y0) + 4;
    const tallinnaTuum: FeatureCollection<Polygon> = {
      type: 'FeatureCollection',
      features: ringkonnad
        .filter((r) => TALLINN.includes(r.properties.nr))
        .map((r) => ({ type: 'Feature', properties: {}, geometry: suurimOsa(r.geometry) })),
    };
    const lisa = projektsioon().fitExtent([[2, 2], [LISA - 2, LISA - 2]], tallinnaTuum);
    return {
      kõrgus: Math.max(kõrgus, LISA + 4),
      põhi: ringkonnad.map((r) => ({ nr: r.properties.nr, d: geoPath(põhi)(r) ?? '' })),
      lisa: ringkonnad
        .filter((r) => TALLINN.includes(r.properties.nr))
        .map((r) => ({ nr: r.properties.nr, d: geoPath(lisa)(r) ?? '' })),
    };
  });
</script>

{#if joonistus}
  <svg viewBox="0 0 {LAIUS} {joonistus.kõrgus}" width={LAIUS} height={joonistus.kõrgus} role="img" aria-label={silt}>
    {#each joonistus.põhi as r (r.nr)}
      <path d={r.d} class:valitud={r.nr === ringkond} data-ringkond={r.nr - 1}><title>{r.nr}</title></path>
    {/each}
    <rect x="0.5" y="0.5" width={LISA} height={LISA} rx="3" class="lisa-kast" />
    {#each joonistus.lisa as r (r.nr)}
      <path d={r.d} class:valitud={r.nr === ringkond} data-ringkond={r.nr - 1}><title>{r.nr}</title></path>
    {/each}
  </svg>
{/if}

<style>
  svg {
    display: block;
  }

  path {
    fill: color-mix(in srgb, var(--tekst) 20%, var(--pind));
    stroke: var(--pind);
    stroke-width: 0.7;
    cursor: pointer;
    transition: fill 0.12s;
  }

  path:hover {
    fill: color-mix(in srgb, var(--rohk) 45%, var(--pind));
  }

  path.valitud {
    fill: var(--rohk);
  }

  .lisa-kast {
    fill: none;
    stroke: var(--joon);
    stroke-width: 0.8;
  }
</style>
