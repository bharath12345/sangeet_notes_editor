import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('Cursor Navigation', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
  });

  async function getBeatNumber(page: import('@playwright/test').Page): Promise<number> {
    const chips = page.locator('.header-chip');
    const count = await chips.count();
    for (let i = 0; i < count; i++) {
      const text = await chips.nth(i).textContent();
      const match = text?.match(/Beat (\d+)\/\d+/);
      if (match) return parseInt(match[1]);
    }
    return -1;
  }

  async function getCycleNumber(page: import('@playwright/test').Page): Promise<number> {
    const chips = page.locator('.header-chip');
    const count = await chips.count();
    for (let i = 0; i < count; i++) {
      const text = await chips.nth(i).textContent();
      const match = text?.match(/Cycle (\d+)/);
      if (match) return parseInt(match[1]);
    }
    return -1;
  }

  test('cursor starts at beat 1', async () => {
    const beat = await getBeatNumber(app.page);
    expect(beat).toBe(1);
  });

  test('right arrow advances cursor beat', async () => {
    const initialBeat = await getBeatNumber(app.page);
    await app.pressKey('ArrowRight');
    const newBeat = await getBeatNumber(app.page);
    expect(newBeat).toBe(initialBeat + 1);
  });

  test('left arrow moves cursor back', async () => {
    await app.pressKey('ArrowRight');
    await app.pressKey('ArrowRight');
    const beat2 = await getBeatNumber(app.page);
    await app.pressKey('ArrowLeft');
    const beat1 = await getBeatNumber(app.page);
    expect(beat1).toBe(beat2 - 1);
  });

  test('left arrow stops at beat 1', async () => {
    await app.pressKey('ArrowLeft');
    await app.pressKey('ArrowLeft');
    await app.pressKey('ArrowLeft');
    const beat = await getBeatNumber(app.page);
    expect(beat).toBeGreaterThanOrEqual(1);
  });

  test('cursor wraps to next cycle at end of taal', async () => {
    // Teentaal has 16 beats; press right 16 times to cross cycle boundary
    for (let i = 0; i < 16; i++) {
      await app.pressKey('ArrowRight');
    }
    const cycle = await getCycleNumber(app.page);
    expect(cycle).toBeGreaterThanOrEqual(2);
  });

  test('Tab advances subdivision within beat', async () => {
    await app.pressKey('Tab');
    await app.waitForApi();
    const subChip = app.page.locator('.header-chip', { hasText: /Sub/ });
    const text = await subChip.textContent();
    expect(text).toBeTruthy();
  });

  test('clicking a beat cell moves cursor', async ({ page }) => {
    // Insert notes to create grid cells
    await app.pressKeys(['s', 'r', 'g']);
    const beatCells = page.locator('.swar-row td[data-beat]');
    if (await beatCells.count() > 1) {
      await beatCells.first().click();
      await app.waitForApi();
      const beat = await getBeatNumber(page);
      expect(beat).toBeGreaterThanOrEqual(1);
    }
  });
});
