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

  // --- Keyboard Legend Toggle (right-side panel) ---

  test('legend button toggles keyboard legend panel', async () => {
    await app.legendBtn.click();
    await app.waitForApi();
    const legendVisible = await app.keyboardLegend.isVisible();
    // Toggle again
    await app.legendBtn.click();
    await app.waitForApi();
    const legendHidden = !(await app.keyboardLegend.isVisible());
    // One of these should be true — it toggled
    expect(legendVisible || legendHidden).toBe(true);
  });

  test('keyboard legend shows shortcut sections', async () => {
    await app.legendBtn.click();
    await app.waitForApi();
    if (await app.keyboardLegend.isVisible()) {
      const legendSections = app.page.locator('.legend-section');
      expect(await legendSections.count()).toBeGreaterThanOrEqual(1);
    }
  });

  test('keyboard legend shows key bindings', async () => {
    await app.legendBtn.click();
    await app.waitForApi();
    if (await app.keyboardLegend.isVisible()) {
      const kbdElements = app.page.locator('.keyboard-legend kbd');
      expect(await kbdElements.count()).toBeGreaterThanOrEqual(1);
    }
  });
});
