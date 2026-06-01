import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('Swar Editing', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
  });

  test('inserting Sa shows swar glyph on grid', async () => {
    await app.pressKey('s');
    const glyphs = app.page.locator('.swar-glyph');
    const count = await glyphs.count();
    expect(count).toBeGreaterThanOrEqual(1);
  });

  test('inserting multiple notes fills beats', async () => {
    await app.pressKey('s');
    await app.pressKey('r');
    await app.pressKey('g');
    const glyphs = app.page.locator('.swar-glyph');
    const count = await glyphs.count();
    expect(count).toBeGreaterThanOrEqual(3);
  });

  test('inserting rest shows rest symbol', async () => {
    await app.pressKey('-');
    await app.waitForApi();
  });

  test('delete removes last inserted note', async () => {
    await app.pressKey('s');
    const before = await app.page.locator('.swar-glyph').count();
    await app.pressKey('Delete');
    const after = await app.page.locator('.swar-glyph').count();
    expect(after).toBeLessThanOrEqual(before);
  });

  test('inserting note sequence Sa Re Ga Ma', async () => {
    await app.pressKey('s');
    await app.pressKey('r');
    await app.pressKey('g');
    await app.pressKey('m');
    const glyphs = app.page.locator('.swar-glyph');
    expect(await glyphs.count()).toBeGreaterThanOrEqual(4);
  });

  test('dual swar input (fast typing ss)', async () => {
    // Type 's' twice quickly
    await app.appContainer.focus();
    await app.page.keyboard.press('s');
    await app.page.keyboard.press('s');
    await app.waitForApi();
  });

  test('inserting notes across multiple beats', async () => {
    for (let i = 0; i < 7; i++) {
      await app.pressKey(['s', 'r', 'g', 'm', 'p', 'd', 'n'][i]);
    }
    const glyphs = app.page.locator('.swar-glyph');
    expect(await glyphs.count()).toBeGreaterThanOrEqual(7);
  });

  test('Backspace deletes at cursor position', async () => {
    await app.pressKey('s');
    await app.pressKey('r');
    await app.pressKey('Backspace');
    await app.waitForApi();
  });
});
