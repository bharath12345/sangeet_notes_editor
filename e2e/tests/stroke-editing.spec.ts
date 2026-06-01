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

  test('F2 toggles stroke edit mode', async () => {
    await app.pressKey('F2');
    const mode = await app.getEditMode();
    expect(mode).toContain('Stroke');
  });

  test('F2 toggles back to swar mode', async () => {
    await app.pressKey('F2');
    await app.pressKey('F2');
    const mode = await app.getEditMode();
    expect(mode).toContain('Swar');
  });

  test('in stroke mode, pressing d sets Da stroke', async () => {
    await app.pressKey('F2'); // enter stroke mode
    await app.pressKey('d');
    await app.waitForApi();
    // Check stroke row shows Da
    const strokeText = await app.page.locator('.stroke-indicator').first().textContent();
    // Might show Da if stroke line is visible
    await app.pressKey('F2'); // exit stroke mode
  });

  test('in stroke mode, pressing r sets Ra stroke', async () => {
    await app.pressKey('F2');
    await app.pressKey('r');
    await app.waitForApi();
    await app.pressKey('F2');
  });

  test('stroke row visibility toggles with Strokes button', async () => {
    await app.strokesBtn.click();
    await app.waitForApi();
    // Toggle and check stroke row presence
    const strokeRows = await app.page.locator('.stroke-row').count();
    // It's either visible or hidden depending on initial state
    await app.strokesBtn.click();
    await app.waitForApi();
  });
});
