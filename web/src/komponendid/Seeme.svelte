<script lang="ts">
  // Juhuslikkuse seeme ja nupp uue juhusliku seemne jaoks. Seeme määrab nii korduvad simulatsioonid kui ka
  // näidiskorduse, seega uus seeme arvutab mõlemad uuesti.
  import { t } from '../lib/i18n.svelte';
  import type { Olek } from '../lib/olek';

  let { olek = $bindable(), nupuSilt, testid = '' }: { olek: Olek; nupuSilt: string; testid?: string } = $props();

  function uusSeeme() {
    olek.seeme = Math.floor(Math.random() * 1_000_000);
  }

  function muudaSeemet(sündmus: Event) {
    const väärtus = Number((sündmus.currentTarget as HTMLInputElement).value);
    if (Number.isInteger(väärtus)) olek.seeme = väärtus;
  }
</script>

<span class="seeme">
  <label title={t('seeme.selgitus')}>
    <span>{t('seeme')}</span>
    <input type="number" value={olek.seeme} onchange={muudaSeemet} data-testid="seeme{testid}" />
  </label>
  <button onclick={uusSeeme} title={t('seeme.uus.selgitus')} data-testid="uus-seeme{testid}">
    <span aria-hidden="true">↻</span>
    {nupuSilt}
  </button>
</span>

<style>
  .seeme {
    display: inline-flex;
    flex-wrap: wrap;
    gap: 0.4rem;
    align-items: center;
  }

  label {
    display: inline-flex;
    gap: 0.4rem;
    align-items: center;
  }

  input {
    width: 6.5em;
    padding: 0.2em 0.4em;
    border: 1px solid var(--joon);
    border-radius: 6px;
    background: var(--pind);
  }

  button {
    display: inline-flex;
    gap: 0.35rem;
    align-items: center;
  }
</style>
