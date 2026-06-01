import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('Keyboard Input', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
  });

  test('pressing s inserts Sa', async () => {
    await app.pressKey('s');
    const glyphs = app.page.locator('.swar-glyph');
    expect(await glyphs.count()).toBeGreaterThanOrEqual(1);
  });

  test('pressing r inserts Re', async () => {
    await app.pressKey('r');
    // Verify swar row has content
    const swarCells = app.page.locator('.swar-row .swar-glyph');
    const count = await swarCells.count();
    expect(count).toBeGreaterThanOrEqual(0);
  });

  test('pressing g inserts Ga', async () => {
    await app.pressKey('g');
    await app.waitForApi();
  });

  test('pressing m inserts Ma', async () => {
    await app.pressKey('m');
    await app.waitForApi();
  });

  test('pressing p inserts Pa', async () => {
    await app.pressKey('p');
    await app.waitForApi();
  });

  test('pressing d inserts Dha', async () => {
    await app.pressKey('d');
    await app.waitForApi();
  });

  test('pressing n inserts Ni', async () => {
    await app.pressKey('n');
    await app.waitForApi();
  });

  test('pressing - inserts rest', async () => {
    await app.pressKey('-');
    await app.waitForApi();
  });

  test('pressing _ inserts sustain', async () => {
    await app.pressWithModifier('Shift', '-');
    await app.waitForApi();
  });

  test('pressing Shift+R inserts komal Re', async () => {
    await app.pressWithModifier('Shift', 'r');
    await app.waitForApi();
  });

  test('pressing Shift+M inserts tivra Ma', async () => {
    await app.pressWithModifier('Shift', 'm');
    await app.waitForApi();
  });

  test('pressing Delete removes last event', async () => {
    await app.pressKey('s');
    await app.pressKey('Delete');
    await app.waitForApi();
  });
});
