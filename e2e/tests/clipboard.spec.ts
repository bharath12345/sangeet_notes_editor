import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('Clipboard Operations', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
  });

  test('clipboard toolbar buttons are visible', async () => {
    await expect(app.cutBtn).toBeVisible();
    await expect(app.copyBtn).toBeVisible();
    await expect(app.pasteBtn).toBeVisible();
  });

  test('Shift+ArrowRight creates selection highlight', async () => {
    await app.pressKey('s');
    await app.pressKey('r');
    await app.pressKey('g');
    // Move cursor back to beat 0
    await app.pressKey('ArrowLeft');
    await app.pressKey('ArrowLeft');
    await app.pressKey('ArrowLeft');
    // Select forward
    await app.pressWithModifier('Shift', 'ArrowRight');
    const selected = app.page.locator('.selected');
    expect(await selected.count()).toBeGreaterThanOrEqual(1);
  });

  test('Shift+ArrowLeft creates selection highlight', async () => {
    await app.pressKey('s');
    await app.pressKey('r');
    // Cursor is now at beat 2; select backward
    await app.pressWithModifier('Shift', 'ArrowLeft');
    const selected = app.page.locator('.selected');
    expect(await selected.count()).toBeGreaterThanOrEqual(1);
  });

  test('plain arrow after selection clears highlight', async () => {
    await app.pressKey('s');
    await app.pressKey('r');
    await app.pressKey('ArrowLeft');
    // Create selection
    await app.pressWithModifier('Shift', 'ArrowRight');
    const selectedBefore = await app.page.locator('.selected').count();
    expect(selectedBefore).toBeGreaterThanOrEqual(1);
    // Plain arrow clears selection
    await app.pressKey('ArrowRight');
    const selectedAfter = await app.page.locator('.selected').count();
    expect(selectedAfter).toBe(0);
  });

  test('copy without selection shows status message', async () => {
    await app.pressWithModifier('Control', 'c');
    const logs = await app.getStatusLog();
    const hasNoSelection = logs.some((l) => l.toLowerCase().includes('no selection'));
    expect(hasNoSelection).toBe(true);
  });

  test('cut without selection shows status message', async () => {
    await app.pressWithModifier('Control', 'x');
    const logs = await app.getStatusLog();
    const hasNoSelection = logs.some((l) => l.toLowerCase().includes('no selection'));
    expect(hasNoSelection).toBe(true);
  });

  test('copy button without selection shows status message', async () => {
    await app.copyBtn.click();
    await app.waitForApi();
    const logs = await app.getStatusLog();
    const hasNoSelection = logs.some((l) => l.toLowerCase().includes('no selection'));
    expect(hasNoSelection).toBe(true);
  });
});
