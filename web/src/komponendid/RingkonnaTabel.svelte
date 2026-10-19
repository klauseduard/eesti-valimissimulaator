<script lang="ts">
  // Ühe ringkonna erakondade arvutus tabelina: hääled, lihtkvoodid, mandaadid (0,75 jäägiga märgitud) ja
  // valitud kandidaadid; 10% nõude tõttu valimata jäänud on punasega. Kasutavad selgitus ja samm-sammult vaade.
  import { erakonnaInfo } from '../lib/erakonnad';
  import { arv, protsent, t } from '../lib/i18n.svelte';
  import type { Tulemus } from '../lib/simulaator';

  let { arvutus }: { arvutus: Tulemus['ringkonnad'][number] } = $props();
</script>

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

<style>
  table {
    width: 100%;
    font-size: 0.9rem;
  }

  th[scope='row'] {
    white-space: nowrap;
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

  /* kitsas: kvootide veerg peidetakse (mandaatide arv ja 0,75 märk jäävad), lahtrid tihedamad */
  @container kaart (max-width: 460px) {
    .kvoote {
      display: none;
    }

    table :is(th, td) {
      padding-left: 0.3em;
      padding-right: 0.3em;
    }
  }
</style>
