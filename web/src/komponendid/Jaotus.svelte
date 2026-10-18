<script lang="ts">
  // Erakonna mandaatide arvu jaotus korduste vahel väikese tulpdiagrammina. Tulbad väljaspool 90% vahemikku
  // on heledamad. Arvud ise on kõrvalveergudes (vahemik, keskmine), seega diagramm on lisateave.
  import { sagedused } from '../lib/statistika';

  let { väärtused, värv, p05, p95, silt }: { väärtused: number[]; värv: string; p05: number; p95: number; silt: string } =
    $props();

  const LAIUS = 96;
  const KÕRGUS = 22;

  const tulbad = $derived.by(() => {
    const loend = sagedused(väärtused);
    const min = loend[0][0];
    const max = loend[loend.length - 1][0];
    const ulatus = Math.max(max - min + 1, 12);
    const tulbaLaius = LAIUS / ulatus;
    const suurim = Math.max(...loend.map(([, n]) => n));
    return loend.map(([väärtus, n]) => ({
      x: (väärtus - min) * tulbaLaius,
      laius: Math.max(1, tulbaLaius - 1),
      kõrgus: Math.max(1, (n / suurim) * KÕRGUS),
      sees: väärtus >= p05 && väärtus <= p95,
    }));
  });
</script>

<svg width={LAIUS} height={KÕRGUS} role="img" aria-label={silt} class="jaotus">
  <title>{silt}</title>
  {#each tulbad as t}
    <rect x={t.x} y={KÕRGUS - t.kõrgus} width={t.laius} height={t.kõrgus} rx="1" fill={värv} fill-opacity={t.sees ? 1 : 0.35} />
  {/each}
</svg>

<style>
  .jaotus {
    display: block;
    overflow: visible;
  }
</style>
