import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('Dialog Interactions', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
  });

  // --- New Composition Dialog ---

  test('clicking New opens the new composition dialog', async () => {
    await app.newBtn.click();
    await expect(app.modalOverlay).toBeVisible();
    await expect(app.modalDialog).toBeVisible();
  });

  test('new dialog has title field', async () => {
    await app.newBtn.click();
    const titleInput = app.page.locator('#new-title');
    await expect(titleInput).toBeVisible();
  });

  test('new dialog has type, raag, taal selectors', async () => {
    await app.newBtn.click();
    await expect(app.page.locator('#new-type')).toBeVisible();
    await expect(app.page.locator('#new-raag')).toBeVisible();
    await expect(app.page.locator('#new-taal')).toBeVisible();
  });

  test('new dialog Cancel closes without creating', async () => {
    await app.newBtn.click();
    const cancelBtn = app.page.locator('.modal-footer .btn-secondary');
    await cancelBtn.click();
    await expect(app.modalOverlay).toBeHidden();
  });

  test('new dialog Create submits the form', async () => {
    await app.newBtn.click();
    const titleInput = app.page.locator('#new-title');
    await titleInput.fill('Test Composition');
    const createBtn = app.page.locator('.modal-footer .btn-primary');
    await createBtn.click();
    await app.waitForApi();
    // Dialog should close
    await expect(app.modalOverlay).toBeHidden();
  });

  test('New button opens dialog via toolbar click', async () => {
    // Ctrl+N is not mapped in the Elm KeyHandler; use toolbar button
    await app.newBtn.click();
    await expect(app.modalOverlay).toBeVisible();
    // Close it
    const cancelBtn = app.page.locator('.modal-footer .btn-secondary');
    await cancelBtn.click();
    await expect(app.modalOverlay).toBeHidden();
  });

  // --- Properties Dialog ---

  test('clicking Properties opens the properties dialog', async () => {
    await app.propsBtn.click();
    await expect(app.modalOverlay).toBeVisible();
    const title = app.page.locator('.modal-title');
    const text = await title.textContent();
    expect(text).toContain('Properties');
  });

  test('properties dialog shows composition metadata', async () => {
    await app.propsBtn.click();
    await app.page.locator('.modal-body').waitFor({ state: 'visible', timeout: 3000 });
    // Check for form inputs — could be .form-group or direct input/select elements
    const inputs = app.page.locator('.modal-body input, .modal-body select');
    expect(await inputs.count()).toBeGreaterThanOrEqual(1);
  });

  test('properties dialog Cancel closes without saving', async () => {
    await app.propsBtn.click();
    const cancelBtn = app.page.locator('.modal-footer .btn-secondary');
    await cancelBtn.click();
    await expect(app.modalOverlay).toBeHidden();
  });

  // --- About Dialog ---

  test('clicking About opens the about dialog', async () => {
    await app.aboutBtn.click();
    await expect(app.modalOverlay).toBeVisible();
    const title = app.page.locator('.modal-title');
    const text = await title.textContent();
    expect(text).toContain('Sangeet');
  });

  test('about dialog shows app info', async () => {
    await app.aboutBtn.click();
    const body = app.page.locator('.modal-body');
    const text = await body.textContent();
    expect(text).toContain('notation editor');
  });

  test('about dialog can be closed', async () => {
    await app.aboutBtn.click();
    await expect(app.modalOverlay).toBeVisible();
    // About dialog has a Close button with btn-primary class
    const closeBtn = app.page.locator('.modal-footer button', { hasText: 'Close' });
    await closeBtn.click();
    await expect(app.modalOverlay).toBeHidden();
  });
});
