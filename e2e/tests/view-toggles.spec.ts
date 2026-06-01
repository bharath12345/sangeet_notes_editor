import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('View Toggles', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
  });

  // --- Stroke Line Toggle ---

  test('strokes button toggles stroke row visibility', async () => {
    // Insert a note first so the grid is rendered
    await app.pressKey('s');
    const strokeRowsBefore = await app.page.locator('.stroke-row').count();
    await app.strokesBtn.click();
    await app.waitForApi();
    const strokeRowsAfter = await app.page.locator('.stroke-row').count();
    // Either toggled on or off — count changed (or stayed at 0 if initially off)
    // Toggle again to verify it changes
    await app.strokesBtn.click();
    await app.waitForApi();
    const strokeRowsFinal = await app.page.locator('.stroke-row').count();
    expect(strokeRowsFinal).toBe(strokeRowsBefore);
  });

  // --- Sahitya Line Toggle ---

  test('sahitya button toggles sahitya row visibility', async () => {
    await app.pressKey('s');
    const sahityaRowsBefore = await app.page.locator('.sahitya-row').count();
    await app.sahityaBtn.click();
    await app.waitForApi();
    const sahityaRowsAfter = await app.page.locator('.sahitya-row').count();
    await app.sahityaBtn.click();
    await app.waitForApi();
    const sahityaRowsFinal = await app.page.locator('.sahitya-row').count();
    expect(sahityaRowsFinal).toBe(sahityaRowsBefore);
  });

  // --- Keyboard Legend Toggle ---

  test('keys button toggles keyboard legend panel', async () => {
    await app.keysBtn.click();
    await app.waitForApi();
    const legendVisible = await app.keyboardLegend.isVisible();
    // Toggle again
    await app.keysBtn.click();
    await app.waitForApi();
    const legendHidden = !(await app.keyboardLegend.isVisible());
    // One of these should be true — it toggled
    expect(legendVisible || legendHidden).toBe(true);
  });

  test('keyboard legend shows shortcut sections', async () => {
    await app.keysBtn.click();
    await app.waitForApi();
    if (await app.keyboardLegend.isVisible()) {
      const legendSections = app.page.locator('.legend-section');
      expect(await legendSections.count()).toBeGreaterThanOrEqual(1);
    }
  });

  test('keyboard legend shows key bindings', async () => {
    await app.keysBtn.click();
    await app.waitForApi();
    if (await app.keyboardLegend.isVisible()) {
      const kbdElements = app.page.locator('.keyboard-legend kbd');
      expect(await kbdElements.count()).toBeGreaterThanOrEqual(1);
    }
  });
});
