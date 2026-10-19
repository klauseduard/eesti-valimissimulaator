<script lang="ts">
  import { erakonnaInfo } from '../lib/erakonnad';
  import { arv, protsent, t } from '../lib/i18n.svelte';
  import type { Olek } from '../lib/olek';
  import { RINGKONNAD, type Tulemus } from '../lib/simulaator';
  import RingkonnaTabel from './RingkonnaTabel.svelte';
  import Seeme from './Seeme.svelte';

  let { tulemus, olek = $bindable() }: { tulemus: Tulemus; olek: Olek } = $props();

  const ringkond = $derived(RINGKONNAD[olek.ringkond - 1]);

  const arvutus = $derived(tulemus.ringkonnad[olek.ringkond - 1]);
  const jaotatud = $derived(arvutus.erakonnad.reduce((s, e) => s + e.valitud.length, 0) +
    arvutus.isikumandaadid.filter((k) => !arvutus.erakonnad.some((e) => e.erakond === k.erakond)).length);
  const jagamata = $derived(arvutus.mandaate - jaotatud);
  const allaKünnise = $derived(tulemus.erakonnad.filter((e) => !e.ületasKünnise && e.hääli > 0).map((e) => erakonnaInfo(e.erakond).lühend));
</script>

<div class="päis">
  <h2 id="selgitus-pealkiri">{t('selgitus.pealkiri')}</h2>
  <button class="samm-nupp" onclick={() => (olek.samm = 0)} data-testid="ava-samm-arvutus">▶ {t('samm.ava')}</button>
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
    <Seeme bind:olek nupuSilt={t('seeme.uus')} />
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
    <RingkonnaTabel {arvutus} />
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

  .samm-nupp {
    font-weight: 600;
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


</style>
