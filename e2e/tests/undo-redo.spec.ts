import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('Undo/Redo', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
  });

  test('undo button is initially disabled', async () => {
    await expect(app.undoBtn).toBeDisabled();
  });

  test('redo button is initially disabled', async () => {
    await expect(app.redoBtn).toBeDisabled();
  });

  test('undo is enabled after inserting a note', async () => {
    await app.pressKey('s');
    await expect(app.undoBtn).toBeEnabled();
  });

  test('clicking undo reverses the last action', async () => {
    await app.pressKey('s');
    const before = await app.page.locator('.swar-glyph').count();
    await app.undoBtn.click();
    await app.waitForApi();
    const after = await app.page.locator('.swar-glyph').count();
    expect(after).toBeLessThan(before);
  });

  test('Ctrl+Z triggers undo', async () => {
    await app.pressKey('s');
    const before = await app.page.locator('.swar-glyph').count();
    await app.pressWithModifier('Control', 'z');
    const after = await app.page.locator('.swar-glyph').count();
    expect(after).toBeLessThan(before);
  });

  test('redo is enabled after undo', async () => {
    await app.pressKey('s');
    await app.undoBtn.click();
    await app.waitForApi();
    await expect(app.redoBtn).toBeEnabled();
  });

  test('clicking redo restores the undone action', async () => {
    await app.pressKey('s');
    await app.undoBtn.click();
    await app.waitForApi();
    const afterUndo = await app.page.locator('.swar-glyph').count();
    await app.redoBtn.click();
    await app.waitForApi();
    const afterRedo = await app.page.locator('.swar-glyph').count();
    expect(afterRedo).toBeGreaterThan(afterUndo);
  });

  test('Ctrl+Y triggers redo', async () => {
    await app.pressKey('s');
    await app.pressWithModifier('Control', 'z');
    await app.pressWithModifier('Control', 'y');
    await app.waitForApi();
  });
});
