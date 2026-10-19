<script lang="ts">
  // Mandaatide jagamine samm-sammult (lib/sammud.ts): dialoog, mis näitab ühe jaotuse (näidiskordus või 2023.
  // aasta tegelikud hääled) sammud RKVS § 62 järjekorras. Sammu suurus on etapp, ringkond või mandaat; kõige
  // väiksema sammuga on igal kohal oma kaart, kus on arvud, võrdlused ja väike diagramm.
  import { erakonnaInfo, järjekorraNr, kontrastTekst } from '../lib/erakonnad';
  import { arv, protsent, t } from '../lib/i18n.svelte';
  import { MANDAATE } from '../lib/koosseis';
  import type { Olek, Sammusuurus } from '../lib/olek';
  import { eelmine, jaotuseSammud, järgmine, rühmaLõpp } from '../lib/sammud';
  import { RINGKONNAD, type Tulemus } from '../lib/simulaator';
  import RingkonnaTabel from './RingkonnaTabel.svelte';

  let { tulemus, olek = $bindable() }: { tulemus: Tulemus; olek: Olek } = $props();

  const SUURUSED: Sammusuurus[] = ['etapp', 'ringkond', 'mandaat'];

  let dialoog = $state<HTMLDialogElement>();
  const sammud = $derived(jaotuseSammud(tulemus));
  const indeks = $derived(Math.min(Math.max(olek.samm ?? 0, 0), sammud.length - 1));
  const samm = $derived(sammud[indeks]);

  // dialoog on avatud, kui olekus on samm
  $effect(() => {
    if (!dialoog) return;
    if (olek.samm !== null && !dialoog.open) dialoog.showModal();
    if (olek.samm === null && dialoog.open) dialoog.close();
  });

  function mine(i: number) {
    olek.samm = i;
  }

  function sulge() {
    olek.samm = null;
  }

  function klahv(sündmus: KeyboardEvent) {
    const sihtmärk = sündmus.target as HTMLElement;
    if (sihtmärk.tagName === 'INPUT' || sihtmärk.tagName === 'SELECT') return;
    if (sündmus.key === 'ArrowRight') {
      mine(järgmine(sammud, indeks, olek.sammuSuurus));
      sündmus.preventDefault();
    } else if (sündmus.key === 'ArrowLeft') {
      mine(eelmine(sammud, indeks, olek.sammuSuurus));
      sündmus.preventDefault();
    }
  }

  // --- arvud kaartide jaoks (kõik tulevad salvestatud jaotusest) ---
  const kehtivaidKokku = $derived(tulemus.ringkonnad.reduce((s, r) => s + r.kehtivaidHääli, 0));
  const ringkond = $derived(samm.ringkond ? tulemus.ringkonnad[samm.ringkond - 1] : null);
  const ringkonnaNimi = (nr: number) => `${nr}. ${RINGKONNAD[nr - 1].nimetus}`;
  const erakonnaArvutus = $derived(ringkond && samm.erakond ? ringkond.erakonnad.find((e) => e.erakond === samm.erakond) ?? null : null);
  const komp = $derived(samm.kompensatsioon !== undefined ? tulemus.kompensatsioon[samm.kompensatsioon] : null);
  const jagaja = (n: number) => (n + 1) ** 0.9;
  const lühend = (e: string) => erakonnaInfo(e).lühend;
  const järjestatud = (erakonnad: string[]) => [...erakonnad].sort((a, b) => järjekorraNr(a) - järjekorraNr(b));

  /** kaardi liik: väiksema sammuga sammu enda kaart, suurema sammuga rühma kokkuvõte */
  const kaart = $derived.by(() => {
    if (olek.sammuSuurus === 'etapp' && samm.etapp === 'ringkonnad') return 'ringkonnad-kokku';
    if (olek.sammuSuurus !== 'mandaat' && samm.etapp === 'kompensatsioon') return 'kompensatsioon-kokku';
    if (olek.sammuSuurus !== 'mandaat' && samm.liik !== 'ringkond-lõpp' && samm.etapp === 'ringkonnad') return 'ringkond-lõpp';
    return samm.liik;
  });

  /** ringkonna etapi kokkuvõte: erakonna ringkonnamandaadid (sh isikumandaadid) ja täitmata kohad */
  const ringkondadeKokku = $derived.by(() => {
    const viimane = sammud.filter((s) => s.etapp === 'ringkonnad').at(-1);
    const jaotatud = viimane?.jaotatud ?? {};
    const kokku = viimane?.kokku ?? 0;
    return { read: järjestatud(Object.keys(jaotatud)).map((e) => ({ erakond: e, mandaate: jaotatud[e] })), kokku };
  });

  /** künnise diagramm: kõik nimekirjad, mis said hääli, protsendi järgi */
  const künniseRead = $derived(
    [...tulemus.erakonnad].filter((e) => e.hääli > 0).sort((a, b) => b.protsent - a.protsent),
  );
  const künniseSkaala = $derived(Math.max(10, ...künniseRead.map((e) => e.protsent)));

  /** ringkonna lihtkvootide diagramm: suurim kvootide arv määrab telje */
  const kvoodiSkaala = $derived(ringkond ? Math.max(1, Math.ceil(Math.max(...ringkond.erakonnad.map((e) => e.kvoote)) + 0.25)) : 1);

  /** kompensatsiooni sammu read: erakond, hääled üle riigi, juba saadud mandaadid, võrdlusarv */
  const kompRead = $derived(
    komp
      ? komp.erakonnad
          .map((e, i) => ({
            erakond: e,
            võrdlusarv: komp.võrdlusarvud[i],
            juba: komp.jubaMandaate[i],
            hääli: Math.round(komp.võrdlusarvud[i] * jagaja(komp.jubaMandaate[i])),
          }))
          .sort((a, b) => b.võrdlusarv - a.võrdlusarv)
      : [],
  );

  /** kompensatsioonimandaadi kandidaadi ringkonna lihtkvoot ja 5% piir */
  const kandidaadiKvoot = $derived(samm.kandidaat ? tulemus.ringkonnad[samm.kandidaat.ringkond - 1].lihtkvoot : 0);

  /** 101 koha riba: jagatud erakonniti püsivas järjekorras, ülejäänu hall */
  const ribaLõigud = $derived(
    järjestatud(Object.keys(samm.jaotatud))
      .filter((e) => samm.jaotatud[e] > 0)
      .map((e) => ({ erakond: e, mandaate: samm.jaotatud[e], info: erakonnaInfo(e) })),
  );
  const uusErakond = $derived(samm.mandaat ? samm.erakond : null);
</script>

<dialog
  bind:this={dialoog}
  class="samm-haaval"
  aria-labelledby="samm-pealkiri"
  oncancel={(e) => {
    e.preventDefault();
    sulge();
  }}
  onkeydown={klahv}
  data-testid="samm-haaval"
>
  {#if olek.samm !== null}
    <header>
      <div class="pealkiri">
        <h2 id="samm-pealkiri">{t('samm.pealkiri')}</h2>
        <p class="vaike vaigne">
          {olek.tegelikudHääled ? t('samm.allikas.tegelikud') : t('samm.allikas.näidis', { seeme: olek.seeme })}
        </p>
      </div>
      <button class="sulge" onclick={sulge} aria-label={t('samm.sulge')} data-testid="samm-sulge">✕</button>
    </header>

    <div class="juhtimine">
      <div class="segmendid vaike" role="radiogroup" aria-label={t('samm.suurus')}>
        {#each SUURUSED as s (s)}
          <label class:valitud={olek.sammuSuurus === s}>
            <input
              type="radio"
              name="sammu-suurus"
              value={s}
              checked={olek.sammuSuurus === s}
              onchange={() => {
                // suurema sammuga kaart on rühma kokkuvõte, seega liigutakse rühma lõppu
                olek.sammuSuurus = s;
                olek.samm = rühmaLõpp(sammud, indeks, s);
              }}
              data-testid="sammu-suurus-{s}"
            />
            {t(`samm.suurus.${s}`)}
          </label>
        {/each}
      </div>
      <div class="nupud">
        <button onclick={() => mine(eelmine(sammud, indeks, olek.sammuSuurus))} disabled={indeks === 0} data-testid="samm-eelmine">
          ← {t('samm.eelmine')}
        </button>
        <span class="loendur vaike vaigne" data-testid="samm-loendur">{t('samm.loendur', { nr: indeks + 1, kokku: sammud.length })}</span>
        <button
          class="peamine"
          onclick={() => mine(järgmine(sammud, indeks, olek.sammuSuurus))}
          disabled={indeks === sammud.length - 1}
          data-testid="samm-järgmine"
        >
          {t('samm.järgmine')} →
        </button>
      </div>
      <input
        class="kerimine"
        type="range"
        min="0"
        max={sammud.length - 1}
        value={indeks}
        oninput={(e) => mine(Number(e.currentTarget.value))}
        aria-label={t('samm.kerimine')}
      />
    </div>

    <!-- jagatud kohad: 101 koha tulp, mis täitub sammude kaupa -->
    <div class="kohad">
      <div class="kohad-silt vaike">
        <span>{t('samm.jagatud', { kokku: samm.kokku, mandaate: MANDAATE })}</span>
        <span class="vaigne">{t('samm.enamus')}</span>
      </div>
      <div class="kohatulp" data-testid="samm-kohad">
        {#each ribaLõigud as l (l.erakond)}
          <span
            class="lõik"
            class:uus={l.erakond === uusErakond}
            style:width="{(l.mandaate / MANDAATE) * 100}%"
            style:background={l.info.värv}
            style:color={kontrastTekst(l.info.värv)}
            title={`${l.info.lühend} ${l.mandaate}`}
          >{(l.mandaate / MANDAATE) * 100 >= 6 ? `${l.info.lühend} ${l.mandaate}` : (l.mandaate / MANDAATE) * 100 >= 2.5 ? l.mandaate : ''}</span>
        {/each}
        <span class="enamuse-joon" style:left="{(50.5 / MANDAATE) * 100}%"></span>
      </div>
    </div>

    <article class="kaart" data-testid="samm-kaart" data-liik={kaart}>
      {#if kaart === 'algus'}
        <h3>{t('samm.algus.pealkiri')}</h3>
        <p>{t('samm.algus.1')}</p>
        <p>{t('samm.algus.2')}</p>
        <p class="vaigne">{t('samm.algus.3')}</p>
      {:else if kaart === 'künnis'}
        <h3>{t('samm.künnis.pealkiri')} <span class="viide">RKVS § 62 lg 3</span></h3>
        <p>{t('samm.künnis', { künnis: arv(kehtivaidKokku * 0.05), kehtivaid: arv(kehtivaidKokku) })}</p>
        <div class="ribad">
          {#each künniseRead as e (e.erakond)}
            <span class="riba-silt"><span class="varv" style:background={erakonnaInfo(e.erakond).värv}></span>{lühend(e.erakond)}</span>
            <span class="rada">
              <span class="täide" class:hele={!e.ületasKünnise} style:width="{(e.protsent / künniseSkaala) * 100}%" style:background={erakonnaInfo(e.erakond).värv}></span>
              <span class="piirjoon" style:left="{(5 / künniseSkaala) * 100}%"></span>
            </span>
            <span class="riba-arv" class:läbi={e.ületasKünnise}>{protsent(e.protsent)}</span>
          {/each}
        </div>
        <p class="vaike vaigne">{t('samm.künnis.isikumandaat')}</p>
      {:else if kaart === 'kvoot' && ringkond}
        <h3>{ringkonnaNimi(ringkond.ringkond)} <span class="viide">RKVS § 62 lg 1</span></h3>
        <p>
          {t('samm.kvoot', {
            mandaate: ringkond.mandaate,
            hääli: arv(ringkond.kehtivaidHääli),
            kvoot: arv(ringkond.lihtkvoot, 1),
          })}
        </p>
        {@render kvoodiDiagramm(null, 0)}
        <p class="vaike vaigne">{t('samm.kvoot.selgitus')}</p>
      {:else if kaart === 'isikumandaat' && ringkond && samm.kandidaat}
        <h3>{t('samm.isikumandaat.pealkiri')} <span class="viide">RKVS § 62 lg 2</span></h3>
        <p>
          {t('samm.isikumandaat', {
            nimi: samm.kandidaat.nimi,
            erakond: lühend(samm.kandidaat.erakond),
            hääli: arv(samm.kandidaat.hääli),
            kvoot: arv(ringkond.lihtkvoot, 0),
          })}
        </p>
        <div class="ribad">
          <span class="riba-silt">{samm.kandidaat.nimi}</span>
          <span class="rada">
            <span class="täide" style:width="{Math.min(100, (samm.kandidaat.hääli / (ringkond.lihtkvoot * 1.6)) * 100)}%" style:background={erakonnaInfo(samm.kandidaat.erakond).värv}></span>
            <span class="piirjoon" style:left="{(1 / 1.6) * 100}%" title={t('samm.lihtkvoot')}></span>
          </span>
          <span class="riba-arv">{arv(samm.kandidaat.hääli)}</span>
        </div>
        <p class="vaike vaigne">
          {ringkond.erakonnad.some((e) => e.erakond === samm.kandidaat?.erakond)
            ? t('samm.isikumandaat.erakonnale', { erakond: lühend(samm.kandidaat.erakond) })
            : t('samm.isikumandaat.eraldi')}
        </p>
      {:else if (kaart === 'ringkonnamandaat' || kaart === 'jagamata') && ringkond && erakonnaArvutus && samm.koht}
        {@const e = erakonnaArvutus}
        {@const piir = ringkond.lihtkvoot * 0.1}
        <h3>
          {kaart === 'jagamata' ? t('samm.jagamata.pealkiri', { erakond: lühend(e.erakond), koht: samm.koht }) : t('samm.ringkonnamandaat.pealkiri', { erakond: lühend(e.erakond), koht: samm.koht })}
          <span class="viide">RKVS § 62 lg 3–4</span>
        </h3>
        <p>
          {t('samm.ringkonnamandaat.kvoodid', { erakond: lühend(e.erakond), hääli: arv(e.hääli), kvoote: arv(e.kvoote, 2), mandaate: e.mandaate })}
          {samm.jäägist
            ? t('samm.ringkonnamandaat.jäägist', { koht: samm.koht, jääk: arv(e.kvoote - Math.floor(e.kvoote), 2) })
            : t('samm.ringkonnamandaat.täiskvoodist', { koht: samm.koht })}
        </p>
        {@render kvoodiDiagramm(e.erakond, samm.koht)}
        {#if kaart === 'ringkonnamandaat' && samm.kandidaat}
          <p>{t('samm.ringkonnamandaat.kandidaat', { piir: arv(piir, 0), koht: samm.koht, nimi: samm.kandidaat.nimi, hääli: arv(samm.kandidaat.hääli) })}</p>
        {:else}
          <p>
            {t('samm.jagamata', { koht: samm.koht, piir: arv(piir, 0) })}
            {#if samm.kandidaat}{t('samm.jagamata.kandidaat', { nimi: samm.kandidaat.nimi, hääli: arv(samm.kandidaat.hääli) })}{/if}
          </p>
        {/if}
        {@render kandidaadid(e.valitud, e.alla10Protsendi, piir)}
      {:else if kaart === 'ringkond-lõpp' && ringkond}
        {@const jaotatud = sammud.filter((s) => s.ringkond === ringkond.ringkond && s.mandaat).length}
        <h3>{t('samm.ringkond.kokkuvõte', { ringkond: ringkonnaNimi(ringkond.ringkond) })} <span class="viide">RKVS § 62 lg 4</span></h3>
        <p>
          {jaotatud < ringkond.mandaate
            ? t('samm.ringkond.jagamata', { jaotatud, mandaate: ringkond.mandaate, jagamata: ringkond.mandaate - jaotatud })
            : t('samm.ringkond.kõik', { mandaate: ringkond.mandaate })}
        </p>
        <div class="kaart-pind"><RingkonnaTabel arvutus={ringkond} /></div>
      {:else if kaart === 'ringkonnad-kokku'}
        <h3>{t('samm.ringkonnad.pealkiri')}</h3>
        <p>{t('samm.ringkonnad', { jaotatud: ringkondadeKokku.kokku, jagamata: MANDAATE - ringkondadeKokku.kokku })}</p>
        <ul class="nimekiri">
          {#each ringkondadeKokku.read as r (r.erakond)}
            <li><span class="varv" style:background={erakonnaInfo(r.erakond).värv}></span>{r.erakond}: <strong>{r.mandaate}</strong></li>
          {/each}
        </ul>
      {:else if kaart === 'kompensatsioon-algus'}
        {@const ringkondades = samm.kokku}
        <h3>{t('samm.komp.algus.pealkiri')} <span class="viide">RKVS § 62 lg 4–5</span></h3>
        <p>{t('samm.komp.algus', { ringkondades, komp: MANDAATE - ringkondades })}</p>
        <p class="vaigne">{t('samm.komp.meetod')}</p>
      {:else if kaart === 'kompensatsioonimandaat' && komp && samm.kandidaat}
        {@const võitja = kompRead.find((r) => r.erakond === komp.erakond)!}
        <h3>
          {t('samm.komp.pealkiri', { nr: (samm.kompensatsioon ?? 0) + 1, kokku: tulemus.kompensatsioon.length, erakond: lühend(komp.erakond) })}
          <span class="viide">RKVS § 62 lg 5–7</span>
        </h3>
        <p>
          {t('samm.komp.võrdlusarv', {
            erakond: lühend(komp.erakond),
            hääli: arv(võitja.hääli),
            juba: võitja.juba,
            jagaja: arv(jagaja(võitja.juba), 3),
            võrdlusarv: arv(võitja.võrdlusarv),
          })}
          {#if komp.viik}<span class="märk" title={t('kompensatsioon.viik.selgitus')}>{t('kompensatsioon.viik')}</span>{/if}
        </p>
        <div class="ribad">
          {#each kompRead as r (r.erakond)}
            <span class="riba-silt"><span class="varv" style:background={erakonnaInfo(r.erakond).värv}></span>{lühend(r.erakond)}</span>
            <span class="rada" title={t('kompensatsioon.võrdlusarv', { hääli: arv(r.hääli), mandaate: r.juba, jagaja: arv(jagaja(r.juba), 3) })}>
              <span class="täide" class:hele={r.erakond !== komp.erakond} style:width="{(r.võrdlusarv / kompRead[0].võrdlusarv) * 100}%" style:background={erakonnaInfo(r.erakond).värv}></span>
            </span>
            <span class="riba-arv" class:läbi={r.erakond === komp.erakond}>{arv(r.võrdlusarv)}</span>
          {/each}
        </div>
        <p class="vaike vaigne">{t('samm.komp.diagramm')}</p>
        <p>
          {t('samm.komp.kandidaat', {
            nimi: samm.kandidaat.nimi,
            nr: samm.kandidaat.jrkNr,
            ringkond: samm.kandidaat.ringkond,
            hääli: arv(samm.kandidaat.hääli),
            piir: arv(kandidaadiKvoot * 0.05, 0),
          })}
        </p>
        {#if komp.vahelejäetud.length > 0}
          <p class="vaike">
            {t(komp.varureegel ? 'samm.komp.varureegel' : 'samm.komp.vahelejäetud', {
              kandidaadid: komp.vahelejäetud.map((k) => `${k.nimi} (nr ${k.jrkNr}, ${arv(k.hääli)})`).join(', '),
            })}
          </p>
        {/if}
      {:else if kaart === 'kompensatsioon-kokku'}
        <h3>{t('samm.komp.kokku.pealkiri')}</h3>
        <ol class="nimekiri komp-nimekiri">
          {#each tulemus.kompensatsioon.slice(0, samm.liik === 'kompensatsioon-algus' ? 0 : (samm.kompensatsioon ?? -1) + 1) as k, i (i)}
            <li><span class="varv" style:background={erakonnaInfo(k.erakond).värv}></span>{lühend(k.erakond)}: {k.kandidaat.nimi}</li>
          {/each}
        </ol>
      {:else if kaart === 'lõpp'}
        <h3>{t('samm.lõpp.pealkiri')}</h3>
        <p>{t('samm.lõpp')}</p>
        <table class="lõputabel">
          <thead>
            <tr>
              <th scope="col">{t('erakond')}</th>
              <th scope="col" class="arv">{t('parlament.ringkonnamandaadid')}</th>
              <th scope="col" class="arv">{t('parlament.kompensatsioon')}</th>
              <th scope="col" class="arv">{t('parlament.kokku')}</th>
            </tr>
          </thead>
          <tbody>
            {#each tulemus.erakonnad.filter((e) => e.mandaate > 0).sort((a, b) => b.mandaate - a.mandaate) as e (e.erakond)}
              <tr>
                <th scope="row"><span class="varv" style:background={erakonnaInfo(e.erakond).värv}></span>{lühend(e.erakond)}</th>
                <td class="arv">{e.ringkonnamandaate}</td>
                <td class="arv">{e.kompensatsioonimandaate}</td>
                <td class="arv"><strong>{e.mandaate}</strong></td>
              </tr>
            {/each}
          </tbody>
        </table>
      {/if}
    </article>
  {/if}
</dialog>

{#snippet kvoodiDiagramm(esile: string | null, koht: number)}
  {#if ringkond}
    <div class="ribad kvoodid">
      {#each ringkond.erakonnad as e (e.erakond)}
        {@const info = erakonnaInfo(e.erakond)}
        {@const täis = Math.floor(e.kvoote)}
        <span class="riba-silt" class:tuhm={esile !== null && esile !== e.erakond}>
          <span class="varv" style:background={info.värv}></span>{info.lühend}
        </span>
        <span class="rada" class:tuhm={esile !== null && esile !== e.erakond} title={t('samm.kvoodid.silt', { kvoote: arv(e.kvoote, 2), mandaate: e.mandaate })}>
          <span class="täide" style:width="{(e.kvoote / kvoodiSkaala) * 100}%" style:background={info.värv}></span>
          {#each { length: kvoodiSkaala - 1 } as _, j}
            <span class="kvoodijoon" style:left="{((j + 1) / kvoodiSkaala) * 100}%"></span>
          {/each}
          <!-- 0,75 piir: jääk sellest paremal annab veel ühe koha -->
          <span class="jäägijoon" style:left="{((täis + 0.75) / kvoodiSkaala) * 100}%"></span>
          {#if esile === e.erakond && koht > 0}
            {@const algus = koht - 1}
            {@const lõpp = e.lisamandaat && koht === e.mandaate ? e.kvoote : koht}
            <span class="valitud-kvoot" style:left="{(algus / kvoodiSkaala) * 100}%" style:width="{((lõpp - algus) / kvoodiSkaala) * 100}%"></span>
          {/if}
        </span>
        <span class="riba-arv">{arv(e.kvoote, 2)} → {e.mandaate}</span>
      {/each}
    </div>
  {/if}
{/snippet}

{#snippet kandidaadid(valitud: Tulemus['valitud'], alla: Tulemus['valitud'], piir: number)}
  {@const kõik = [...valitud, ...alla].sort((a, b) => b.hääli - a.hääli)}
  {@const skaala = Math.max(piir * 1.5, ...kõik.map((k) => k.hääli))}
  <div class="ribad kandidaadid">
    {#each kõik as k (k.regNr)}
      <span class="riba-silt" class:esile={k.regNr === samm.kandidaat?.regNr}>{k.nimi}</span>
      <span class="rada">
        <span class="täide" class:hele={k.hääli < piir} style:width="{(k.hääli / skaala) * 100}%" style:background={erakonnaInfo(k.erakond).värv}></span>
        <span class="piirjoon" style:left="{(piir / skaala) * 100}%" title={t('samm.10protsenti')}></span>
      </span>
      <span class="riba-arv" class:läbi={k.hääli >= piir}>{arv(k.hääli)}</span>
    {/each}
  </div>
  <p class="vaike vaigne">{t('samm.kandidaadid.selgitus', { piir: arv(piir, 0) })}</p>
{/snippet}

<style>
  .samm-haaval {
    width: min(920px, calc(100vw - 32px));
    max-height: calc(100vh - 32px);
    padding: 0;
    border: 1px solid var(--joon);
    border-radius: 14px;
    background: var(--pind);
    color: var(--tekst);
    box-shadow: 0 20px 60px rgb(0 0 0 / 0.3);
    overflow: auto;
  }

  .samm-haaval::backdrop {
    background: rgb(20 18 12 / 0.55);
  }

  header,
  .juhtimine,
  .kohad,
  .kaart {
    padding-left: 1.3rem;
    padding-right: 1.3rem;
  }

  header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 1rem;
    padding-top: 1.1rem;
  }

  header h2 {
    margin: 0 0 0.2rem;
  }

  header p {
    margin: 0;
  }

  .sulge {
    font-size: 1.1rem;
    line-height: 1;
    padding: 0.35rem 0.55rem;
  }

  .juhtimine {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.6rem 1.2rem;
    padding-top: 0.8rem;
    padding-bottom: 0.4rem;
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

  .nupud {
    display: flex;
    align-items: center;
    gap: 0.6rem;
  }

  .peamine {
    background: var(--tekst);
    color: var(--pind);
    border-color: var(--tekst);
    font-weight: 600;
  }

  button:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  .loendur {
    font-variant-numeric: tabular-nums;
    min-width: 6.5em;
    text-align: center;
  }

  .kerimine {
    flex: 1 1 100%;
    accent-color: var(--rohk);
  }

  .kohad {
    padding-top: 0.3rem;
    padding-bottom: 0.6rem;
  }

  .kohad-silt {
    display: flex;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 0.3rem;
    font-variant-numeric: tabular-nums;
  }

  .kohatulp {
    position: relative;
    display: flex;
    height: 26px;
    border-radius: 5px;
    background: var(--pind-2);
    box-shadow: inset 0 0 0 1px var(--joon);
  }

  .lõik {
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    white-space: nowrap;
    font-size: 0.72rem;
    font-weight: 650;
    border-right: 2px solid var(--pind);
    transition: width 0.25s ease;
  }

  .lõik:first-child {
    border-radius: 5px 0 0 5px;
  }

  .lõik.uus {
    box-shadow: inset 0 0 0 2px var(--tekst);
  }

  .enamuse-joon {
    position: absolute;
    top: -4px;
    bottom: -4px;
    width: 2px;
    margin-left: -1px;
    background: var(--tekst);
  }

  .kaart {
    border-top: 1px solid var(--joon);
    padding-top: 1rem;
    padding-bottom: 1.4rem;
    min-height: 18rem;
    container: kaart / inline-size;
  }

  .kaart h3 {
    font-family: var(--serif);
    font-size: 1.3rem;
    margin-bottom: 0.5rem;
  }

  .kaart p {
    margin: 0.4rem 0 0.7rem;
    max-width: 68ch;
  }

  .viide {
    font-family: var(--sans);
    font-weight: 400;
    font-size: 0.78rem;
    color: var(--tekst-3);
    margin-left: 0.3rem;
  }

  /* horisontaalsed tulbad: silt | rada | arv */
  .ribad {
    display: grid;
    grid-template-columns: minmax(4rem, max-content) minmax(0, 1fr) max-content;
    gap: 0.35rem 0.7rem;
    align-items: center;
    margin: 0.6rem 0 0.4rem;
    font-size: 0.85rem;
  }

  .riba-silt {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 14rem;
  }

  .riba-silt.esile {
    font-weight: 700;
  }

  .rada {
    position: relative;
    height: 16px;
    background: var(--pind-2);
    border-radius: 3px;
  }

  .täide {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    border-radius: 3px;
  }

  .täide.hele {
    opacity: 0.35;
  }

  .piirjoon,
  .jäägijoon,
  .kvoodijoon {
    position: absolute;
    top: -3px;
    bottom: -3px;
    width: 0;
  }

  .piirjoon {
    border-left: 2px solid var(--tekst);
  }

  .kvoodijoon {
    border-left: 1px solid var(--pind);
    top: 0;
    bottom: 0;
  }

  .jäägijoon {
    border-left: 1.5px dashed var(--tekst-2);
  }

  .valitud-kvoot {
    position: absolute;
    top: -3px;
    bottom: -3px;
    border: 2px solid var(--tekst);
    border-radius: 3px;
  }

  .riba-arv {
    font-variant-numeric: tabular-nums;
    text-align: right;
    color: var(--tekst-2);
  }

  .riba-arv.läbi {
    color: var(--tekst);
    font-weight: 650;
  }

  .tuhm {
    opacity: 0.35;
  }

  .märk {
    font-size: 0.75rem;
    background: var(--rohk-pind);
    border-radius: 4px;
    padding: 0 0.3em;
    margin-left: 0.25em;
  }

  .nimekiri {
    padding-left: 1.2rem;
    columns: 2 16rem;
    font-size: 0.9rem;
  }

  .lõputabel {
    width: 100%;
    max-width: 30rem;
  }

  @media (max-width: 600px) {
    .samm-haaval {
      width: 100vw;
      max-width: 100vw;
      height: 100dvh;
      max-height: 100dvh;
      border-radius: 0;
      margin: 0;
    }

    header,
    .juhtimine,
    .kohad,
    .kaart {
      padding-left: 12px;
      padding-right: 12px;
    }

    .ribad {
      grid-template-columns: minmax(3rem, max-content) minmax(0, 1fr) max-content;
      font-size: 0.8rem;
    }

    .riba-silt {
      max-width: 8rem;
    }
  }
</style>
