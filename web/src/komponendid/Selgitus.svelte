<script lang="ts">
  import { erakonnaInfo } from '../lib/erakonnad';
  import { arv, protsent, t } from '../lib/i18n.svelte';
  import type { Olek } from '../lib/olek';
  import { RINGKONNAD, type Tulemus } from '../lib/simulaator';

  let { tulemus, olek = $bindable() }: { tulemus: Tulemus; olek: Olek } = $props();

  const ringkond = $derived(RINGKONNAD[olek.ringkond - 1]);

  function uusSeeme() {
    olek.seeme = Math.floor(Math.random() * 1_000_000);
  }

  function muudaSeemet(sündmus: Event) {
    const väärtus = Number((sündmus.currentTarget as HTMLInputElement).value);
    if (Number.isInteger(väärtus)) olek.seeme = väärtus;
  }
  const arvutus = $derived(tulemus.ringkonnad[olek.ringkond - 1]);
  const jaotatud = $derived(arvutus.erakonnad.reduce((s, e) => s + e.valitud.length, 0) +
    arvutus.isikumandaadid.filter((k) => !arvutus.erakonnad.some((e) => e.erakond === k.erakond)).length);
  const jagamata = $derived(arvutus.mandaate - jaotatud);
  const allaKünnise = $derived(tulemus.erakonnad.filter((e) => !e.ületasKünnise && e.hääli > 0).map((e) => erakonnaInfo(e.erakond).lühend));
</script>

<div class="päis">
  <h2 id="selgitus-pealkiri">{t('selgitus.pealkiri')}</h2>
  <label class="vaike">
    <span class="vaigne">{t('ringkond')}</span>
    <select bind:value={olek.ringkond} data-testid="selgituse-ringkond">
      {#each RINGKONNAD as r (r.nr)}
        <option value={r.nr}>{r.nr}. {r.nimetus}</option>
      {/each}
    </select>
  </label>
</div>

<div class="näidis vaike">
  <p data-testid="näidiskordus">
    {olek.tegelikudHääled ? t('näidiskordus.tegelikud') : t('näidiskordus', { seeme: olek.seeme })}
  </p>
  {#if !olek.tegelikudHääled}
    <label class="seeme" title={t('seeme.selgitus')}>
      <span>{t('seeme')}</span>
      <input type="number" value={olek.seeme} onchange={muudaSeemet} data-testid="seeme" />
      <button onclick={uusSeeme} data-testid="uus-seeme">{t('seeme.uus')}</button>
    </label>
  {/if}
</div>

<p class="vaigne">
  {t('selgitus.sissejuhatus', { mandaate: ringkond.mandaate, hääli: arv(arvutus.kehtivaidHääli) })}
</p>

<ol class="sammud">
  <li>
    <h3>{t('selgitus.kvoot.pealkiri')} <span class="viide">{t('viide', { lõige: '1' })}</span></h3>
    <p>
      {t('selgitus.kvoot', {
        hääli: arv(arvutus.kehtivaidHääli),
        mandaate: arvutus.mandaate,
        kvoot: arv(arvutus.lihtkvoot, 1),
      })}
    </p>
  </li>

  <li>
    <h3>{t('selgitus.isikumandaat.pealkiri')} <span class="viide">{t('viide', { lõige: '2' })}</span></h3>
    {#if arvutus.isikumandaadid.length === 0}
      <p>{t('selgitus.isikumandaat.puudub', { kvoot: arv(arvutus.lihtkvoot, 0) })}</p>
    {:else}
      <p>{t('selgitus.isikumandaat', { kvoot: arv(arvutus.lihtkvoot, 0) })}</p>
      <ul>
        {#each arvutus.isikumandaadid as k (k.regNr)}
          <li>
            <span class="varv" style:background={erakonnaInfo(k.erakond).värv}></span>
            <strong>{k.nimi}</strong> ({erakonnaInfo(k.erakond).lühend}): {arv(k.hääli)} {t('häält')}
          </li>
        {/each}
      </ul>
    {/if}
  </li>

  <li>
    <h3>{t('selgitus.ringkonnamandaat.pealkiri')} <span class="viide">{t('viide', { lõige: '3' })}</span></h3>
    <p>{t('selgitus.ringkonnamandaat')}</p>
    {#if allaKünnise.length > 0}
      <p class="vaike vaigne">{t('selgitus.allaKünnise', { erakonnad: allaKünnise.join(', ') })}</p>
    {/if}
    <div class="tabeli-ümbris kitsas">
      <table data-testid="ringkonna-tabel">
        <thead>
          <tr>
            <th scope="col">{t('erakond')}</th>
            <th scope="col" class="arv">{t('selgitus.hääled')}</th>
            <th scope="col" class="arv kvoote">{t('selgitus.kvoote')}</th>
            <th scope="col" class="arv">{t('selgitus.mandaate')}</th>
            <th scope="col">{t('selgitus.valitud')}</th>
          </tr>
        </thead>
        <tbody>
          {#each arvutus.erakonnad as e (e.erakond)}
            {@const info = erakonnaInfo(e.erakond)}
            <tr>
              <th scope="row"><span class="varv" style:background={info.värv}></span><abbr title={e.erakond}>{info.lühend}</abbr></th>
              <td class="arv">{arv(e.hääli)}</td>
              <td class="arv kvoote">{arv(e.kvoote, 2)}</td>
              <td class="arv">
                {e.mandaate}
                {#if e.lisamandaat}<span class="märk" title={t('selgitus.lisamandaat')}>{t('selgitus.lisamandaat.lühike')}</span>{/if}
              </td>
              <td>
                {#each e.valitud as k, i (k.regNr)}
                  {i > 0 ? ', ' : ''}<span title={protsent((k.hääli / arvutus.lihtkvoot) * 100)}>{k.nimi} ({arv(k.hääli)})</span>
                {/each}
                {#if e.alla10Protsendi.length > 0}
                  <div class="vaike puudu">
                    {t('selgitus.alla10', {
                      kandidaadid: e.alla10Protsendi.map((k) => `${k.nimi} (${arv(k.hääli)})`).join(', '),
                      piir: arv(arvutus.lihtkvoot * 0.1, 0),
                    })}
                  </div>
                {/if}
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  </li>

  <li>
    <h3>{t('selgitus.jagamata.pealkiri')} <span class="viide">{t('viide', { lõige: '4' })}</span></h3>
    <p>
      {jagamata > 0
        ? t('selgitus.jagamata', { jaotatud, jagamata })
        : t('selgitus.jagamata.puudub', { jaotatud })}
    </p>
  </li>
</ol>

<style>
  .päis {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem 1rem;
    justify-content: space-between;
    align-items: baseline;
  }

  .päis label {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    min-width: 0;
    max-width: 100%;
  }

  .päis select {
    min-width: 0;
    max-width: 100%;
  }

  .näidis {
    background: var(--rohk-pind);
    border-radius: 8px;
    padding: 0.5rem 0.75rem;
    margin: 0 0 0.8rem;
    display: flex;
    flex-wrap: wrap;
    gap: 0.4rem 1rem;
    align-items: center;
    justify-content: space-between;
  }

  .näidis p {
    margin: 0;
    flex: 1 1 24rem;
  }

  .seeme {
    display: flex;
    gap: 0.4rem;
    align-items: center;
  }

  .seeme input {
    width: 6.5em;
    padding: 0.2em 0.4em;
    border: 1px solid var(--joon);
    border-radius: 6px;
    background: var(--pind);
  }

  th[scope='row'] {
    white-space: nowrap;
  }

  /* kitsas: kvootide veerg peidetakse (mandaatide arv ja 0,75 märk jäävad), lahtrid tihedamad */
  @container kaart (max-width: 460px) {
    .kvoote {
      display: none;
    }

    [data-testid='ringkonna-tabel'] :is(th, td) {
      padding-left: 0.3em;
      padding-right: 0.3em;
    }
  }

  .sammud {
    padding-left: 1.3rem;
    margin: 0;
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: 0.8rem;
  }

  .sammud h3 {
    margin-bottom: 0.2rem;
  }

  .sammud p {
    margin: 0.2rem 0;
  }

  .viide {
    font-weight: 400;
    font-size: 0.8rem;
    color: var(--tekst-3);
    margin-left: 0.3rem;
  }

  abbr {
    text-decoration: none;
  }

  .märk {
    font-size: 0.75rem;
    background: var(--rohk-pind);
    border-radius: 4px;
    padding: 0 0.3em;
    margin-left: 0.25em;
  }

  .puudu {
    color: var(--viga);
    margin-top: 0.2rem;
  }

  table {
    width: 100%;
    font-size: 0.9rem;
  }
</style>
