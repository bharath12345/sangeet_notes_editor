import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('File Operations', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
  });

  test('save button is visible in toolbar', async () => {
    await expect(app.saveBtn).toBeVisible();
  });

  test('open button is visible in toolbar', async () => {
    await expect(app.openBtn).toBeVisible();
  });

  test('PDF export button is visible', async () => {
    await expect(app.pdfBtn).toBeVisible();
  });

  test('HTML export button is visible', async () => {
    await expect(app.htmlBtn).toBeVisible();
  });

  test('clicking Save triggers save port', async () => {
    // Insert a note first
    await app.pressKey('s');
    // Click save — this triggers a port call to JavaScript
    await app.saveBtn.click();
    await app.waitForApi();
    // Verify no error occurred (status bar should not show error)
    const logs = await app.getStatusLog();
    const hasError = logs.some((l) => l.toLowerCase().includes('error'));
    expect(hasError).toBe(false);
  });

  test('clicking PDF export triggers export', async () => {
    await app.pressKey('s');
    await app.pdfBtn.click();
    await app.waitForApi();
  });

  test('clicking HTML export triggers export', async () => {
    await app.pressKey('s');
    await app.htmlBtn.click();
    await app.waitForApi();
  });

  test('Ctrl+S triggers save', async () => {
    await app.pressKey('s');
    await app.pressWithModifier('Control', 's');
    await app.waitForApi();
  });
});
