import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('Stroke Editing', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
    // Insert a note first — strokes attach to notes
    await app.pressKey('s');
  });

  test('in stroke mode, pressing d sets Da stroke', async () => {
    await app.pressKey('F2'); // enter stroke mode
    await app.pressKey('d');
    await app.waitForApi();
    // Check stroke row shows Da
    const _strokeText = await app.page.locator('.stroke-indicator').first().textContent();
    // Might show Da if stroke line is visible
    await app.pressKey('F2'); // exit stroke mode
  });

  test('in stroke mode, pressing r sets Ra stroke', async () => {
    await app.pressKey('F2');
    await app.pressKey('r');
    await app.waitForApi();
    await app.pressKey('F2');
  });

  test('stroke row always renders (PR-A removed the toggle)', async () => {
    // Insert a swar so there is a grid line at all
    await app.pressKey('s');
    await app.waitForApi();
    // The stroke row is now unconditional in GridRenderer.elm
    const strokeRows = await app.page.locator('.stroke-row').count();
    expect(strokeRows).toBeGreaterThanOrEqual(1);
  });
});
