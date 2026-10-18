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
  const enne = Number(await page.getByTestId('mandaate-REF').textContent());
  await page.getByTestId('liugur-REF').fill('45');
  await expect.poll(async () => Number(await page.getByTestId('mandaate-REF').textContent())).toBeGreaterThan(enne);
  // muudatus jõuab URL-i
  await expect(page).toHaveURL(/p\.REF=/);
  await page.getByTestId('taasta').click();
  await ootaKordusi(page);
  await expect(page.getByTestId('mandaate-REF')).toHaveText(String(enne));
});

test('uus seeme muudab näidiskordust, kuid mandaadid tulevad korduvatest simulatsioonidest', async ({ page }) => {
  await page.goto('/#v=norstat-2023-03-03&s=1&k=et');
  await ootaKordusi(page);
  await expect(page.getByTestId('näidiskordus')).toContainText('seemnega 1');
  const enne = await page.getByTestId('mandaate-REF').textContent();
  await page.getByTestId('seeme').fill('2');
  await page.getByTestId('seeme').press('Enter');
  await expect(page.getByTestId('näidiskordus')).toContainText('seemnega 2');
  // kuni uued kordused pole valmis, ei näidata ühe korduse mandaate
  await ootaKordusi(page);
  await expect(page.getByTestId('parlamendi-alus')).toContainText('Kõige sagedasem tulemus');
  expect(await page.getByTestId('mandaate-REF').textContent()).toBe(enne);
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
  await expect(page.getByTestId('eelseadistus')).toHaveValue('norstat-2023-03-03');
});

test('kaart joonistab kõik ringkonnad ja korduvad simulatsioonid lõpevad', async ({ page }) => {
  await page.goto('/#v=norstat-2023-03-03&s=1&k=et');
  // 12 ringkonda põhikaardil ja Tallinna 3 ringkonda lisakaardil
  await expect(page.locator('[data-testid="kaart"] path[data-ringkond]')).toHaveCount(15);
  await ootaKordusi(page);
  await page.locator('[data-testid="kaart"] path[data-ringkond="7"]').first().click();
  await expect(page.getByTestId('selgituse-ringkond')).toHaveValue('7');
});
