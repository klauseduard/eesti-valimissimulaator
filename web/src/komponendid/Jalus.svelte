<script lang="ts">
  import { t } from '../lib/i18n.svelte';
</script>

<footer>
  <div class="sisu vaike">
    <p>{t('jalus.mudel')}</p>
    <ul>
      <li>
        {t('jalus.seadus')}:
        <a href="https://www.riigiteataja.ee/akt/124052024010">Riigikogu valimise seadus</a> (§ 62)
      </li>
      <li>
        {t('jalus.tulemus')}:
        <a href="https://opendata.valimised.ee/api/RK_2023/RESULTS.xml">opendata.valimised.ee</a>
      </li>
      <li>
        {t('jalus.küsitlused')}: Norstat (<a href="https://reitingud.ee">reitingud.ee</a>), Kantar Emor
        (<a href="https://www.err.ee/1608901703/kantar-emori-kusitlus-reformierakond-liigub-valimisvoidu-poole">ERR</a>)
      </li>
      <li>
        {t('jalus.ajalugu')}:
        <a href="https://en.wikipedia.org/wiki/Opinion_polling_for_the_2023_Estonian_parliamentary_election">Wikipedia</a>
        (<a href="https://creativecommons.org/licenses/by-sa/4.0/">CC BY-SA 4.0</a>);
        <a href="https://github.com/klauseduard/eesti-valimissimulaator/blob/main/docs/tagasitestimine.md">{t('jalus.tagasitest')}</a>
      </li>
      <li>
        {t('jalus.kaart')}:
        <a href="https://geoportaal.maaruum.ee/avaandmete-litsents">Haldus- ja asustusjaotus: Maa- ja Ruumiamet</a>
      </li>
      <li>
        {t('jalus.kood')}:
        <a href="https://github.com/klauseduard/eesti-valimissimulaator">github.com/klauseduard/eesti-valimissimulaator</a>
      </li>
    </ul>
  </div>
</footer>

<style>
  footer {
    border-top: 1px solid var(--joon);
    background: var(--pind-2);
    margin-top: 1rem;
  }

  .sisu {
    max-width: 1400px;
    margin: 0 auto;
    padding: 1rem 16px 1.5rem;
    color: var(--tekst-2);
  }

  ul {
    margin: 0.4rem 0 0;
    padding-left: 1.2rem;
  }
</style>
