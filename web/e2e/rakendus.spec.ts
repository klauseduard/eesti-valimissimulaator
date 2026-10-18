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
  await expect(page.getByTestId('häälte-jaotus')).toHaveValue('kalibreeritud');
  await page.getByTestId('häälte-jaotus').selectOption('lihtne');
  await expect(page).toHaveURL(/j=lihtne/);
  await ootaKordusi(page);
  expect(await tabel.textContent()).not.toBe(enne);
  await page.reload();
  await expect(page.getByTestId('häälte-jaotus')).toHaveValue('lihtne');
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
