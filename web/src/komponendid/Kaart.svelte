<script lang="ts">
  import { onMount } from 'svelte';
  import { geoArea, geoConicConformal, geoPath, type GeoProjection } from 'd3-geo';
  import { feature } from 'topojson-client';
  import type { Feature, FeatureCollection, Geometry, Polygon } from 'geojson';
  import type { Topology, GeometryCollection } from 'topojson-specification';
  import { erakonnaInfo, järjekorraNr, nimiLühendist } from '../lib/erakonnad';
  import { arv, protsent, t } from '../lib/i18n.svelte';
  import { KAART_VÕITJA, type Olek } from '../lib/olek';
  import type { ToetuseAndmed } from '../lib/protokoll';
  import type { Kokkuvõte } from '../lib/protokoll';
  import { RINGKONNAD, type Tulemus } from '../lib/simulaator';

  let {
    tulemus,
    toetus,
    kokkuvõte,
    kokkuvõteKehtib,
    olek = $bindable(),
  }: { tulemus: Tulemus; toetus: ToetuseAndmed; kokkuvõte: Kokkuvõte | null; kokkuvõteKehtib: boolean; olek: Olek } =
    $props();

  type Ringkond = Feature<Geometry, { nr: number }>;

  const LAIUS = 640;
  /** Tallinna lisakaart: ringkonnad 1–3 suuremas mõõtkavas */
  const LISAKAART = { x: 8, y: 8, laius: 170, kõrgus: 170 };
  const TALLINN = [1, 2, 3];
  /**
   * Siltide nihked pikslites, kus ringkonna keskpunkt ei sobi sildi kohaks: ringkond 5 koosneb saartest
   * ja Läänemaast (keskpunkt jääb merele), ringkond 9 ümbritseb Tartu linna (ringkond 10).
   */
  const SILDI_NIHE: Record<number, [number, number]> = { 5: [-38, 18], 9: [-6, -30] };

  let ringkonnad = $state<Ringkond[] | null>(null);
  let allikas = $state('');
  let laadimisViga = $state<string | null>(null);

  async function laadi() {
    laadimisViga = null;
    try {
      const vastus = await fetch(`${import.meta.env.BASE_URL}data/ringkonnad.topo.json`);
      if (!vastus.ok) throw new Error(`HTTP ${vastus.status}`);
      const topo = (await vastus.json()) as Topology<{ ringkonnad: GeometryCollection<{ nr: number }> }> & {
        metadata?: { allikas?: string };
      };
      ringkonnad = (feature(topo, topo.objects.ringkonnad) as FeatureCollection<Geometry, { nr: number }>).features;
      allikas = topo.metadata?.allikas ?? '';
    } catch (viga) {
      laadimisViga = viga instanceof Error ? viga.message : String(viga);
    }
  }

  onMount(laadi);

  function projektsioon(): GeoProjection {
    // Lamberti konformne kooniline projektsioon L-EST97 parameetritega
    return geoConicConformal().parallels([58, 59 + 20 / 60]).rotate([-24, 0]);
  }

  const joonistus = $derived.by(() => {
    if (!ringkonnad) return null;
    const kõik: FeatureCollection<Geometry, { nr: number }> = { type: 'FeatureCollection', features: ringkonnad };
    const tallinn: FeatureCollection<Geometry, { nr: number }> = {
      type: 'FeatureCollection',
      features: ringkonnad.filter((r) => TALLINN.includes(r.properties.nr)),
    };
    // kaardi kõrgus tuleb Eesti kujust, et all ei jääks tühja merd
    const vasak = LISAKAART.x + LISAKAART.laius + 16;
    const proov = projektsioon().fitWidth(LAIUS - vasak - 10, kõik);
    const [[, py0], [, py1]] = geoPath(proov).bounds(kõik);
    const kõrgus = Math.max(Math.ceil(py1 - py0) + 20, LISAKAART.y + LISAKAART.kõrgus + 8);
    const põhi = projektsioon().fitExtent([[vasak, 10], [LAIUS - 10, kõrgus - 10]], kõik);
    // lisakaart sobitatakse iga ringkonna suurima osa järgi; väikesed saared (nt Aegna) jäävad kärbitud alast välja
    const tallinnaTuum: FeatureCollection<Polygon> = {
      type: 'FeatureCollection',
      features: tallinn.features.map((r) => ({ type: 'Feature', properties: {}, geometry: suurimOsa(r.geometry) })),
    };
    const lisa = projektsioon().fitExtent(
      [[LISAKAART.x + 10, LISAKAART.y + 10], [LISAKAART.x + LISAKAART.laius - 10, LISAKAART.y + LISAKAART.kõrgus - 22]],
      tallinnaTuum,
    );
    const põhiTee = geoPath(põhi);
    const lisaTee = geoPath(lisa);
    // Tallinna asukoht põhikaardil lisakaardi ühendusjoone jaoks
    const [[x0, y0], [x1, y1]] = põhiTee.bounds(tallinn);
    const nihutatud = (nr: number, [x, y]: [number, number]): [number, number] => {
      const [dx, dy] = SILDI_NIHE[nr] ?? [0, 0];
      return [x + dx, y + dy];
    };
    return {
      kõrgus,
      põhi: ringkonnad.map((r) => ({ r, d: põhiTee(r) ?? '', keskpunkt: nihutatud(r.properties.nr, põhiTee.centroid(r)) })),
      lisa: tallinn.features.map((r) => ({ r, d: lisaTee(r) ?? '', keskpunkt: lisaTee.centroid(r) })),
      tallinnaKast: { x0, y0, x1, y1 },
    };
  });

  function suurimOsa(geomeetria: Geometry): Polygon {
    if (geomeetria.type === 'Polygon') return geomeetria;
    if (geomeetria.type !== 'MultiPolygon') throw new Error(`Ootamatu geomeetria ${geomeetria.type}`);
    const osad: Polygon[] = geomeetria.coordinates.map((coordinates) => ({ type: 'Polygon', coordinates }));
    return osad.reduce((suurim, osa) => (geoArea(osa) > geoArea(suurim) ? osa : suurim));
  }

  /**
   * ringkonnast valitud kandidaatide arv erakonniti (kõik mandaadiliigid): tegelike häältega selle
   * tulemuse järgi, muidu keskmine korduvates simulatsioonides
   */
  const ringkondadeMandaadid = $derived.by(() => {
    const kaart = new Map<number, Map<string, number>>();
    if (olek.tegelikudHääled) {
      for (const k of tulemus.valitud) {
        const erakonnad = kaart.get(k.ringkond) ?? new Map<string, number>();
        erakonnad.set(k.erakond, (erakonnad.get(k.erakond) ?? 0) + 1);
        kaart.set(k.ringkond, erakonnad);
      }
    } else if (kokkuvõte && kokkuvõte.kordi > 0) {
      kokkuvõte.ringkondadeValitud.forEach((read, r) => {
        const keskmised = read.map((rida) => rida.reduce((a, b) => a + b, 0) / rida.length);
        kaart.set(
          r + 1,
          new Map(kokkuvõte.erakonnad.map((e, i) => [e, keskmised[i]] as [string, number]).filter(([, m]) => m >= 0.05)),
        );
      });
    }
    return kaart;
  });

  /** mandaatide arv tekstina: tegelikud täisarvud, keskmised ühe kümnendkohaga */
  function mandaate(m: number): string {
    return olek.tegelikudHääled ? String(m) : arv(m, 1);
  }

  /** ringkonna võitja: enim mandaate, võrdsuse korral rohkem hääli ringkonnas */
  function võitja(nr: number): string | null {
    const mandaadid = ringkondadeMandaadid.get(nr);
    if (!mandaadid || mandaadid.size === 0) return null;
    const hääled = new Map(tulemus.ringkonnad[nr - 1].erakonnad.map((e) => [e.erakond, e.hääli]));
    return [...mandaadid.entries()].sort((a, b) => b[1] - a[1] || (hääled.get(b[0]) ?? 0) - (hääled.get(a[0]) ?? 0))[0][0];
  }

  const valitudErakond = $derived(olek.kaart === KAART_VÕITJA ? null : nimiLühendist(olek.kaart) ?? null);
  const erakonnaToetus = $derived.by(() => {
    if (!valitudErakond) return null;
    const i = toetus.erakonnad.indexOf(valitudErakond);
    return i < 0 ? null : toetus.protsendid[i];
  });
  const toetuseMax = $derived(erakonnaToetus ? Math.max(1, ...erakonnaToetus) : 1);

  function täide(nr: number): string {
    if (valitudErakond) {
      if (!erakonnaToetus) return 'var(--pind-3)';
      return erakonnaInfo(valitudErakond).värv;
    }
    const v = võitja(nr);
    return v ? erakonnaInfo(v).värv : 'var(--pind-3)';
  }

  /** erakonna toetuse vaates on läbipaistvus võrdeline toetusega (üks toon, heledast tumedani) */
  function läbipaistvus(nr: number): number {
    if (!erakonnaToetus) return 1;
    return 0.1 + 0.9 * (erakonnaToetus[nr - 1] / toetuseMax);
  }

  /** tekst täite peal: valge või tume vastavalt täite heledusele */
  function tekstiVärv(värv: string, läbipaistev: number): string {
    if (!värv.startsWith('#') || läbipaistev < 0.55) return 'var(--tekst)';
    const [r, g, b] = [1, 3, 5].map((i) => parseInt(värv.slice(i, i + 2), 16) / 255);
    const lin = (c: number) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);
    const heledus = 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b);
    return heledus > 0.3 ? '#141413' : '#ffffff';
  }

  function silt(nr: number): string {
    if (erakonnaToetus) return protsent(erakonnaToetus[nr - 1], 0);
    const v = võitja(nr);
    if (!v) return '';
    return erakonnaInfo(v).lühend;
  }

  function kirjeldus(nr: number): string {
    const ringkond = RINGKONNAD[nr - 1];
    const mandaadid = [...(ringkondadeMandaadid.get(nr)?.entries() ?? [])]
      .sort((a, b) => järjekorraNr(a[0]) - järjekorraNr(b[0]))
      .map(([e, m]) => `${erakonnaInfo(e).lühend} ${mandaate(m)}`)
      .join(', ');
    return t('kaart.ringkond', { nr, nimi: ringkond.nimetus, mandaate: ringkond.mandaate }) + (mandaadid ? `: ${mandaadid}` : '');
  }

  let vihje = $state<{ nr: number; x: number; y: number } | null>(null);
  let ümbris: HTMLDivElement | undefined = $state();

  function näitaVihjet(nr: number, sündmus: PointerEvent | FocusEvent) {
    if (!ümbris) return;
    const kast = ümbris.getBoundingClientRect();
    if (sündmus instanceof PointerEvent) {
      vihje = { nr, x: sündmus.clientX - kast.left, y: sündmus.clientY - kast.top };
    } else {
      const sihtmärk = (sündmus.currentTarget as SVGPathElement).getBoundingClientRect();
      vihje = { nr, x: sihtmärk.left + sihtmärk.width / 2 - kast.left, y: sihtmärk.top + sihtmärk.height / 2 - kast.top };
    }
  }

  function vali(nr: number) {
    olek.ringkond = nr;
  }

  function klahv(nr: number, sündmus: KeyboardEvent) {
    if (sündmus.key === 'Enter' || sündmus.key === ' ') {
      sündmus.preventDefault();
      vali(nr);
    }
  }

  const erakondadeValik = $derived(
    toetus.erakonnad.slice().sort((a, b) => järjekorraNr(a) - järjekorraNr(b)).map((e) => erakonnaInfo(e).lühend),
  );
</script>

<div class="päis">
  <h2 id="kaart-pealkiri">{t('kaart.pealkiri')}</h2>
  <label class="vaike">
    <span class="vaigne">{t('kaart.vaade')}</span>
    <select bind:value={olek.kaart} data-testid="kaardi-vaade">
      <option value={KAART_VÕITJA}>{t('kaart.võitja')}</option>
      {#each erakondadeValik as lühend}
        <option value={lühend}>{t('kaart.toetus', { erakond: lühend })}</option>
      {/each}
    </select>
  </label>
</div>

<div class="ümbris" class:uueneb={!olek.tegelikudHääled && !kokkuvõteKehtib && !valitudErakond} bind:this={ümbris}>
  {#if laadimisViga}
    <div class="laadimisviga" role="alert">
      <p>{t('kaart.laadimisviga', { teade: laadimisViga })}</p>
      <button onclick={laadi}>{t('proovi.uuesti')}</button>
    </div>
  {:else if !joonistus}
    <p class="vaigne">{t('laadin')}</p>
  {:else}
    <svg viewBox="0 0 {LAIUS} {joonistus.kõrgus}" role="group" aria-label={t('kaart.pealkiri')} data-testid="kaart">
      <defs>
        <clipPath id="lisakaardi-ala">
          <rect x={LISAKAART.x} y={LISAKAART.y} width={LISAKAART.laius} height={LISAKAART.kõrgus} rx="6" />
        </clipPath>
      </defs>
      <rect class="meri" x="0" y="0" width={LAIUS} height={joonistus.kõrgus} rx="8" />
      {#each joonistus.põhi as { r, d, keskpunkt } (r.properties.nr)}
        {@const nr = r.properties.nr}
        <path
          {d}
          fill={täide(nr)}
          fill-opacity={läbipaistvus(nr)}
          class="ringkond"
          class:valitud={olek.ringkond === nr}
          role="button"
          tabindex={TALLINN.includes(nr) ? -1 : 0}
          aria-label={kirjeldus(nr)}
          onclick={() => vali(nr)}
          onkeydown={(e) => klahv(nr, e)}
          onpointermove={(e) => näitaVihjet(nr, e)}
          onpointerleave={() => (vihje = null)}
          onfocus={(e) => näitaVihjet(nr, e)}
          onblur={() => (vihje = null)}
          data-ringkond={nr}
        />
        {#if !TALLINN.includes(nr)}
          <text x={keskpunkt[0]} y={keskpunkt[1]} class="silt" fill={tekstiVärv(täide(nr), läbipaistvus(nr))}>{silt(nr)}</text>
        {/if}
      {/each}

      <!-- Tallinna lisakaart ja ühendusjoon Tallinna asukohaga põhikaardil -->
      <line
        x1={LISAKAART.x + LISAKAART.laius}
        y1={LISAKAART.y + LISAKAART.kõrgus / 2}
        x2={joonistus.tallinnaKast.x0}
        y2={(joonistus.tallinnaKast.y0 + joonistus.tallinnaKast.y1) / 2}
        class="ühendus"
      />
      <rect
        x={joonistus.tallinnaKast.x0 - 2}
        y={joonistus.tallinnaKast.y0 - 2}
        width={joonistus.tallinnaKast.x1 - joonistus.tallinnaKast.x0 + 4}
        height={joonistus.tallinnaKast.y1 - joonistus.tallinnaKast.y0 + 4}
        class="tallinna-märk"
      />
      <rect x={LISAKAART.x} y={LISAKAART.y} width={LISAKAART.laius} height={LISAKAART.kõrgus} rx="6" class="lisakaart" />
      <text x={LISAKAART.x + 8} y={LISAKAART.y + LISAKAART.kõrgus - 8} class="lisakaardi-nimi">Tallinn</text>
      <g clip-path="url(#lisakaardi-ala)">
      {#each joonistus.lisa as { r, d, keskpunkt } (r.properties.nr)}
        {@const nr = r.properties.nr}
        <path
          {d}
          fill={täide(nr)}
          fill-opacity={läbipaistvus(nr)}
          class="ringkond"
          class:valitud={olek.ringkond === nr}
          role="button"
          tabindex="0"
          aria-label={kirjeldus(nr)}
          onclick={() => vali(nr)}
          onkeydown={(e) => klahv(nr, e)}
          onpointermove={(e) => näitaVihjet(nr, e)}
          onpointerleave={() => (vihje = null)}
          onfocus={(e) => näitaVihjet(nr, e)}
          onblur={() => (vihje = null)}
          data-ringkond={nr}
        />
        <text x={keskpunkt[0]} y={keskpunkt[1]} class="silt" fill={tekstiVärv(täide(nr), läbipaistvus(nr))}>{silt(nr)}</text>
      {/each}
      </g>
    </svg>

    {#if vihje}
      {@const nr = vihje.nr}
      <div class="vihje" style:left="{vihje.x}px" style:top="{vihje.y}px" role="tooltip">
        <strong>{nr}. {RINGKONNAD[nr - 1].nimetus}</strong>
        <div class="vaike vaigne">{t('kaart.vihje.mandaate', { mandaate: RINGKONNAD[nr - 1].mandaate })}</div>
        {#if erakonnaToetus && valitudErakond}
          <div>{erakonnaInfo(valitudErakond).lühend}: {protsent(erakonnaToetus[nr - 1])}</div>
        {/if}
        <ul>
          {#each [...(ringkondadeMandaadid.get(nr)?.entries() ?? [])].sort((a, b) => b[1] - a[1]) as [erakond, arvMandaate]}
            <li><span class="varv" style:background={erakonnaInfo(erakond).värv}></span>{erakonnaInfo(erakond).lühend} {mandaate(arvMandaate)}</li>
          {/each}
        </ul>
      </div>
    {/if}
  {/if}
</div>

{#if erakonnaToetus && valitudErakond}
  <div class="legend vaike" aria-hidden="true">
    <span>0 %</span>
    <span class="skaala" style:--värv={erakonnaInfo(valitudErakond).värv}></span>
    <span>{protsent(toetuseMax, 0)}</span>
  </div>
{/if}

<p class="vaike vaigne">{olek.tegelikudHääled ? t('kaart.selgitus.tegelikud') : t('kaart.selgitus')}</p>

<details class="vaike">
  <summary>{t('kaart.tabelina')}</summary>
  <div class="tabeli-ümbris">
    <table>
      <thead>
        <tr><th scope="col">{t('ringkond')}</th><th scope="col">{t('kaart.valitud')}</th></tr>
      </thead>
      <tbody>
        {#each RINGKONNAD as ringkond (ringkond.nr)}
          <tr>
            <th scope="row">{ringkond.nr}. {ringkond.nimetus}</th>
            <td>
              {[...(ringkondadeMandaadid.get(ringkond.nr)?.entries() ?? [])]
                .sort((a, b) => b[1] - a[1])
                .map(([e, m]) => `${erakonnaInfo(e).lühend} ${mandaate(m)}`)
                .join(', ')}
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
</details>

{#if allikas}
  <p class="vaike vaigne allikas">
    {t('kaart.allikas')}: <a href="https://geoportaal.maaruum.ee/avaandmete-litsents">{allikas}</a>
  </p>
{/if}

<style>
  .päis {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem 1rem;
    justify-content: space-between;
    align-items: baseline;
  }

  .ümbris {
    position: relative;
    transition: opacity 0.2s;
  }

  .uueneb {
    opacity: 0.5;
  }

  svg {
    width: 100%;
    height: auto;
    display: block;
  }

  .meri {
    fill: var(--meri);
  }

  .ringkond {
    stroke: var(--pind);
    stroke-width: 1;
    stroke-linejoin: round;
    cursor: pointer;
    transition: fill 0.3s, fill-opacity 0.3s;
    outline: none;
  }

  .ringkond:hover,
  .ringkond:focus-visible {
    stroke: var(--tekst);
    stroke-width: 1.5;
  }

  .ringkond.valitud {
    stroke: var(--tekst);
    stroke-width: 1.6;
  }

  .silt {
    font-size: 12px;
    font-weight: 600;
    text-anchor: middle;
    dominant-baseline: middle;
    pointer-events: none;
  }

  .lisakaart {
    fill: var(--meri);
    stroke: var(--joon);
    stroke-width: 1;
  }

  .lisakaardi-nimi {
    font-size: 11px;
    fill: var(--tekst-2);
  }

  .ühendus,
  .tallinna-märk {
    stroke: var(--tekst-3);
    stroke-width: 1;
    fill: none;
  }

  .vihje {
    position: absolute;
    transform: translate(12px, 12px);
    background: var(--pind);
    border: 1px solid var(--joon);
    border-radius: 8px;
    padding: 0.5rem 0.7rem;
    box-shadow: var(--vari);
    pointer-events: none;
    font-size: 0.85rem;
    max-width: 240px;
    z-index: 2;
  }

  .vihje ul {
    list-style: none;
    margin: 0.3rem 0 0;
    padding: 0;
  }

  .legend {
    display: flex;
    gap: 0.5rem;
    align-items: center;
    margin-top: 0.4rem;
  }

  .skaala {
    width: 140px;
    height: 10px;
    border-radius: 5px;
    background: linear-gradient(to right, color-mix(in srgb, var(--värv) 10%, transparent), var(--värv));
  }

  .laadimisviga {
    background: var(--viga-pind);
    border-radius: 8px;
    padding: 0.8rem;
  }

  .allikas {
    margin-bottom: 0;
  }

  details {
    margin-top: 0.4rem;
  }

  summary {
    cursor: pointer;
  }
</style>
