<script lang="ts">
  // Erakonna mandaatide arvu jaotus korduste vahel kõigile erakondadele ühisel teljel, et hajuvusi saaks
  // võrrelda: tulbad on korduste sagedused, hele riba on 90% vahemik, tume kriips on keskmine. Arvud ise on
  // kõrvalveergudes, seega diagramm on lisateave.
  import { sagedused } from '../lib/statistika';

  let {
    väärtused,
    värv,
    p05,
    p95,
    keskmine,
    telg,
    silt,
  }: { väärtused: number[]; värv: string; p05: number; p95: number; keskmine: number; telg: number; silt: string } =
    $props();

  const LAIUS = 140;
  const KÕRGUS = 22;

  const x = (v: number) => (v / telg) * LAIUS;
  const tulbaLaius = $derived(Math.max(1.5, LAIUS / telg - 1));
  const tulbad = $derived.by(() => {
    const loend = sagedused(väärtused);
    const suurim = Math.max(...loend.map(([, n]) => n));
    return loend.map(([väärtus, n]) => ({ x: x(väärtus) - tulbaLaius / 2, kõrgus: Math.max(1, (n / suurim) * (KÕRGUS - 4)) }));
  });
</script>

<svg viewBox="0 0 {LAIUS} {KÕRGUS}" role="img" aria-label={silt} class="jaotus" preserveAspectRatio="none">
  <title>{silt}</title>
  <rect x={x(p05) - tulbaLaius / 2} y="0" width={x(p95) - x(p05) + tulbaLaius} height={KÕRGUS} rx="3" fill={värv} fill-opacity="0.14" />
  {#each tulbad as t}
    <rect x={t.x} y={KÕRGUS - t.kõrgus} width={tulbaLaius} height={t.kõrgus} rx="1" fill={värv} />
  {/each}
  <rect x={x(keskmine) - 1} y="0" width="2" height={KÕRGUS} fill="var(--tekst)" />
</svg>

<style>
  .jaotus {
    display: block;
    overflow: visible;
    width: var(--jaotuse-laius, 140px);
    height: 22px;
  }
</style>
