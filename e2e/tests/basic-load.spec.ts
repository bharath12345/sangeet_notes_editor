import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('Basic Page Load', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
  });

  test('page loads with app container', async () => {
    await expect(app.appContainer).toBeVisible();
  });

  test('toolbar is visible with file buttons', async () => {
    await expect(app.toolbar).toBeVisible();
    await expect(app.newBtn).toBeVisible();
    await expect(app.openBtn).toBeVisible();
    await expect(app.saveBtn).toBeVisible();
  });

  test('notation grid area is visible', async () => {
    await expect(app.mainContent).toBeVisible();
    await expect(app.page.locator('.canvas-area')).toBeVisible();
  });

  test('status bar is visible', async () => {
    await expect(app.statusBar).toBeVisible();
  });

  test('section tabs are visible with at least one section', async () => {
    const count = await app.getSectionCount();
    expect(count).toBeGreaterThanOrEqual(1);
  });

  test('script selector shows Devanagari as default', async () => {
    await expect(app.scriptSelect).toBeVisible();
    const value = await app.scriptSelect.inputValue();
    expect(value).toBe('devanagari');
  });

  test('edit mode shows Swar by default', async () => {
    const mode = await app.getEditMode();
    expect(mode).toContain('Swar');
  });
});
