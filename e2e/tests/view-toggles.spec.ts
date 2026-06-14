import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('View Toggles', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
  });

  // --- Stroke / Sahitya rows always render (PR-A removed toggles) ---

  test('stroke row always renders', async () => {
    await app.pressKey('s');
    await app.waitForApi();
    const strokeRows = await app.page.locator('.stroke-row').count();
    expect(strokeRows).toBeGreaterThanOrEqual(1);
  });

  test('sahitya row always renders', async () => {
    await app.pressKey('s');
    await app.waitForApi();
    const sahityaRows = await app.page.locator('.sahitya-row').count();
    expect(sahityaRows).toBeGreaterThanOrEqual(1);
  });

  // PR-C C.4: the right-side keyboard legend panel and its toggle button were
  // retired. The keyboard reference now lives inside the cheat sheet dialog
  // (covered by keyboard-shortcuts.spec.ts / cheat-sheet specs).
});
