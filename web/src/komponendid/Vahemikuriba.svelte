<script lang="ts">
  // Kohtade arvu 90% vahemik ühisel teljel: hele riba on vahemik, tume kriips mediaan ja katkendjoon enamuse
  // piir. Teljel on HTML-elemendid protsentides, et riba oleks igal laiusel terav.
  let {
    p05,
    p95,
    mediaan,
    algus,
    lõpp,
    piir,
    silt,
  }: { p05: number; p95: number; mediaan: number; algus: number; lõpp: number; piir: number; silt: string } = $props();

  const x = (v: number) => `${((v - algus) / (lõpp - algus)) * 100}%`;
  const laius = (a: number, b: number) => `${((b - a) / (lõpp - algus)) * 100}%`;
</script>

<span class="riba" role="img" aria-label={silt} title={silt}>
  <span class="vahemik" class:üle={p05 >= piir} class:alla={p95 < piir} style:left={x(p05 - 0.5)} style:width={laius(p05 - 0.5, p95 + 0.5)}></span>
  <span class="piir" style:left={x(piir - 0.5)}></span>
  <span class="mediaan" style:left={x(mediaan)}></span>
</span>

<style>
  .riba {
    position: relative;
    display: block;
    height: 18px;
  }

  .riba::before {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    top: 50%;
    border-top: 1px solid var(--joon);
  }

  .vahemik {
    position: absolute;
    top: 3px;
    bottom: 3px;
    border-radius: 4px;
    background: var(--tekst-3);
    opacity: 0.45;
  }

  .vahemik.üle {
    background: var(--rohk);
    opacity: 0.45;
  }

  .vahemik.alla {
    opacity: 0.25;
  }

  .piir {
    position: absolute;
    top: -2px;
    bottom: -2px;
    border-left: 1.5px dashed var(--rohk);
  }

  .mediaan {
    position: absolute;
    top: 1px;
    bottom: 1px;
    width: 2px;
    margin-left: -1px;
    border-radius: 1px;
    background: var(--tekst);
  }
</style>
