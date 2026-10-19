import { expect, test } from '@playwright/test';

test('2023 tegelike häältega näitab ametlikke mandaate', async ({ page }) => {
  await page.goto('/#v=rk2023&h=1');
  const oodatud = { REF: 37, EKRE: 17, KESK: 16, E200: 14, SDE: 9, ISAMAA: 8 };
  for (const [lühend, mandaate] of Object.entries(oodatud)) {
    await expect(page.getByTestId(`mandaate-${lühend}`)).toHaveText(String(mandaate));
  }
  await expect(page.getByTestId('tegelikud-hääled')).toBeChecked();
});

/** ootab, kuni 1000 kordust on tehtud ja parlamendi joonis näitab kõige sagedasemat tulemust */
async function ootaKordusi(page: import('@playwright/test').Page) {
  await expect(page.getByTestId('parlamendi-alus')).toContainText('1000', { timeout: 45_000 });
}

test('liugur muudab tulemust', async ({ page }) => {
  await page.goto('/#v=norstat-2023-03-03&s=1&k=et');
  await ootaKordusi(page);
  const arvuna = (tekst: string | null) => Number(tekst);
  const enne = (await page.getByTestId('mandaate-REF').textContent()) ?? '';
  await page.getByTestId('liugur-REF').fill('45');
  await expect.poll(async () => arvuna(await page.getByTestId('mandaate-REF').textContent())).toBeGreaterThan(arvuna(enne));
  // muudatus jõuab URL-i
  await expect(page).toHaveURL(/p\.REF=/);
  await page.getByTestId('taasta').click();
  await ootaKordusi(page);
  await expect(page.getByTestId('mandaate-REF')).toHaveText(enne);
});

test('uus seeme muudab näidiskordust, kuid mandaadid tulevad korduvatest simulatsioonidest', async ({ page }) => {
  // küsitluse veata on erakondade hääled igas korduses samad, seega seeme keskmisi ei muuda
  await page.goto('/#v=norstat-2023-03-03&s=1&k=et&e=0');
  await ootaKordusi(page);
  await expect(page.getByTestId('näidiskordus')).toContainText('seemnega 1');
  const enne = await page.getByTestId('mandaate-REF').textContent();
  await page.getByTestId('seeme').fill('2');
  await page.getByTestId('seeme').press('Enter');
  await expect(page.getByTestId('näidiskordus')).toContainText('seemnega 2');
  // kuni uued kordused pole valmis, ei näidata ühe korduse mandaate
  await ootaKordusi(page);
  await expect(page.getByTestId('parlamendi-alus')).toContainText('küsitluse viga arvestamata');
  expect(await page.getByTestId('mandaate-REF').textContent()).toBe(enne);
});

test('ülemine nupp simuleerib uue seemnega uuesti', async ({ page }) => {
  await page.goto('/#v=koond-2023-03-03&s=1&k=et');
  await ootaKordusi(page);
  await page.getByTestId('uus-seeme-ülal').click();
  await expect(page).not.toHaveURL(/[#&]s=1&/);
  // mõlemad seemne väljad näitavad sama uut seemet
  const uus = await page.getByTestId('seeme-ülal').inputValue();
  expect(uus).not.toBe('1');
  await expect(page.getByTestId('seeme')).toHaveValue(uus);
  await ootaKordusi(page);
});

test('küsitluse viga laiendab mandaatide vahemikku', async ({ page }) => {
  await page.goto('/#v=norstat-2023-03-03&s=1&k=et');
  await ootaKordusi(page);
  await expect(page.getByTestId('parlamendi-alus')).toContainText('küsitluse viga arvestatud');
  const vahemik = page.getByTestId('vahemik-REF');
  await expect(vahemik).toContainText('–');
  await page.getByTestId('küsitluse-viga').uncheck();
  await expect(page).toHaveURL(/e=0/);
  await ootaKordusi(page);
  await expect(page.getByTestId('parlamendi-alus')).toContainText('küsitluse viga arvestamata');
  await expect(vahemik).not.toContainText('–');
});

test('häälte jaotuse valik jõuab linki ja muudab näidiskorduse valituid', async ({ page }) => {
  await page.goto('/#v=norstat-2023-03-03&s=1&k=et');
  await ootaKordusi(page);
  const tabel = page.getByTestId('kandidaatide-tabel');
  const enne = await tabel.textContent();
  await expect(page.getByTestId('häälte-jaotus-kalibreeritud')).toBeChecked();
  await page.getByText('lihtne', { exact: true }).click();
  await expect(page).toHaveURL(/j=lihtne/);
  await ootaKordusi(page);
  expect(await tabel.textContent()).not.toBe(enne);
  await page.reload();
  await expect(page.getByTestId('häälte-jaotus-lihtne')).toBeChecked();
});

test('metoodika leht avaneb päisest samas keeles ja mahub kitsale ekraanile', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 800 });
  for (const [keel, pealkiri] of [['et', 'Metoodika'], ['en', 'Methodology']] as const) {
    await page.goto(`/#v=koond-2023-03-03&s=1&k=${keel}`);
    await page.getByTestId('metoodika-link').click();
    await expect(page).toHaveURL(new RegExp(`metoodika\\.html#k=${keel}$`));
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(pealkiri);
    await expect(page.locator('html')).toHaveAttribute('lang', keel);
    await expect(page.getByTestId('ai-märkus')).toBeVisible();
    const laius = await page.evaluate(() => document.documentElement.scrollWidth);
    expect(laius).toBeLessThanOrEqual(390);
    await page.getByTestId('tagasi').click();
    await expect(page).toHaveURL(new RegExp(`k=${keel}`));
    await expect(page.getByTestId('metoodika-link')).toBeVisible();
  }
});

test('rakendus mahub kitsale ekraanile ja jaotiste riba kerib jaotiseni', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 800 });
  await page.goto('/#v=koond-2023-03-03&s=1&k=et');
  await ootaKordusi(page);
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await page.getByTestId('nav-kandidaadid').click();
  await expect(page.getByTestId('nav-kandidaadid')).toHaveAttribute('aria-current', 'location');
  await expect(page.getByRole('heading', { name: 'Kandidaadid' })).toBeInViewport();
  // nupp ei muuda URL-i olekut (#ankur tekitaks lingi vea)
  await expect(page.getByTestId('lingi-teade')).toHaveCount(0);
  await page.getByTestId('nav-sisend').click();
  await expect(page.getByTestId('liugur-REF')).toBeInViewport();
});

for (const [laius, kõrgus] of [[390, 844], [1024, 768], [1440, 900]]) {
  test(`ükski tabel ega ala ei ole laiusel ${laius} vaikides kärbitud`, async ({ page }) => {
    await page.setViewportSize({ width: laius, height: kõrgus });
    await page.goto('/#v=koond-2023-03-03&s=1&k=et');
    await ootaKordusi(page);
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(laius);
    // kõik külgsuunas keritavad alad peale jaotiste riba (selle parem serv hajub, et kerimine oleks näha)
    const kärbitud = await page.evaluate(() =>
      [...document.querySelectorAll<HTMLElement>('main *')]
        .filter((el) => ['auto', 'scroll'].includes(getComputedStyle(el).overflowX))
        .filter((el) => el.offsetParent !== null && el.scrollWidth > el.clientWidth + 1)
        .map((el) => `${el.tagName}.${[...el.classList].join('.')} ${el.scrollWidth}>${el.clientWidth}`),
    );
    expect(kärbitud).toEqual([]);
  });
}

test('ringkonna ruudu muutmine jõuab linki ja üle 100% annab hoiatuse', async ({ page }) => {
  await page.goto('/#v=koond-2023-03-03&s=1&k=et');
  await ootaKordusi(page);
  const ruut = page.getByTestId('ruut-PP-1');
  await ruut.fill('3');
  await ruut.press('Enter');
  await expect(page).toHaveURL(/p\.PP=3,/);
  await expect(ruut).toHaveValue(/^3/);
  // veerule osutamine näitab ringkonna nime päises
  await page.getByTestId('ruut-KESK-7').hover();
  await expect(page.getByTestId('ringkonna-nimi')).toHaveText('7. Ida-Virumaa');
  await expect(page.getByTestId('ruudu-silt')).toContainText('7. Ida-Virumaa');
  // päise kaardil on sama ringkond esile tõstetud ja kaart jääb ridu kerides nähtavaks
  const kaart = page.getByTestId('toetuse-kaart');
  await expect(kaart.locator('path.valitud')).toHaveCount(1);
  await expect(kaart).toBeInViewport();
  // kaardil ringkonnale osutamine tõstab esile selle veeru
  // sündmus saadetakse otse ringkonna kujule: hiirega osutamisel võtaks Playwright piirdekasti keskpunkti, mis
  // saartega ringkondade (5, 12) puhul jääb merele
  await kaart.locator('path[data-ringkond="11"]').first().dispatchEvent('pointerover');
  await expect(page.getByTestId('ringkonna-nimi')).toHaveText('12. Pärnumaa');
  await expect(page.getByTestId('ruut-KESK-12')).toHaveClass(/aktiivne/);
  const muudEnne = Number((await page.getByTestId('muud-1').textContent())!.replace(',', '.'));
  expect(muudEnne).toBeGreaterThan(0);
  await page.getByTestId('ruut-REF-1').fill('90');
  await page.getByTestId('ruut-REF-1').press('Enter');
  await expect(page.getByRole('alert').first()).toBeVisible();
  // muude osa muutub negatiivseks ja punaseks
  await expect(page.getByTestId('muud-1')).toHaveClass(/vigane/);
  expect(Number((await page.getByTestId('muud-1').textContent())!.replace(',', '.').replace('−', '-'))).toBeLessThan(0);
  // vigane väärtus taastab endise
  await page.getByTestId('ruut-PP-2').fill('abc');
  await page.getByTestId('ruut-PP-2').press('Enter');
  await expect(page.getByTestId('ruut-PP-2')).not.toHaveValue('abc');
});

test('kandidaatide piiril-filter', async ({ page }) => {
  await page.goto('/#v=koond-2023-03-03&s=1&k=et');
  await ootaKordusi(page);
  await expect(page.getByTestId('kandidaatide-loendus')).toContainText('jäid piirile');
  await page.getByTestId('ainult-piiril').check();
  const sagedused = await page.getByTestId('kandidaatide-tabel').locator('td.sagedus').allTextContents();
  expect(sagedused.length).toBeGreaterThan(0);
  for (const s of sagedused) {
    const p = Number(s.replace(/[^0-9]/g, ''));
    expect(p).toBeGreaterThanOrEqual(5);
    expect(p).toBeLessThanOrEqual(95);
  }
});

test('jagatud link taastab oleku', async ({ page }) => {
  await page.goto('/#v=emor-2023-03&s=7&k=en&r=4&kaart=SDE');
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Riigikogu election simulator');
  await expect(page.getByTestId('eelseadistus')).toHaveValue('emor-2023-03');
  await expect(page.getByTestId('seeme')).toHaveValue('7');
  await expect(page.getByTestId('selgituse-ringkond')).toHaveValue('4');
  await expect(page.getByTestId('kaardi-vaade')).toHaveValue('SDE');
});

test('vigane link annab teate ja vaikimisi oleku', async ({ page }) => {
  await page.goto('/#v=olematu&s=1');
  await expect(page.getByTestId('lingi-teade')).toContainText('v=olematu');
  await expect(page.getByTestId('eelseadistus')).toHaveValue('koond-2023-03-03');
});

test('kaart joonistab kõik ringkonnad ja korduvad simulatsioonid lõpevad', async ({ page }) => {
  await page.goto('/#v=norstat-2023-03-03&s=1&k=et');
  // 12 ringkonda põhikaardil ja Tallinna 3 ringkonda lisakaardil
  await expect(page.locator('[data-testid="kaart"] path[data-ringkond]')).toHaveCount(15);
  await ootaKordusi(page);
  await page.locator('[data-testid="kaart"] path[data-ringkond="7"]').first().click();
  await expect(page.getByTestId('selgituse-ringkond')).toHaveValue('7');
});

test('kokkuvõtte lause ja ringkondade diagramm', async ({ page }) => {
  await page.goto('/#k=et');
  await ootaKordusi(page);
  await expect(page.getByTestId('kokkuvõtte-lause')).toContainText('Suurim erakond oleks');
  await page.getByTestId('ringkondade-diagramm').getByRole('button').nth(3).click();
  await expect(page.getByTestId('selgituse-ringkond')).toHaveValue('4');
});

test('koalitsioonide loetelu ja koostaja', async ({ page }) => {
  await page.goto('/#k=et');
  await ootaKordusi(page);
  await expect(page.getByTestId('koalitsioonid').locator('li').first()).toBeVisible();
  await page.getByTestId('koalitsioon-REF').check();
  await page.getByTestId('koalitsioon-EKRE').check();
  await expect(page.getByTestId('valitud-koalitsioon')).toContainText('REF + EKRE');
  await expect(page.getByTestId('viimane-kompensatsioon')).toContainText('Viimase kompensatsioonimandaadi sai');
});
