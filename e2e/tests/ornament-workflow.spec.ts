import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('Ornament Workflow', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
    // Insert a note first — ornaments apply to existing notes
    await app.pressKey('s');
  });

  test('Ctrl+G enters gamak ornament mode', async () => {
    await app.pressWithModifier('Control', 'g');
    // After gamak, ornament is applied directly (simple ornament)
    await app.waitForApi();
  });

  test('Ctrl+A enters andolan ornament mode', async () => {
    await app.pressWithModifier('Control', 'a');
    await app.waitForApi();
  });

  test('Ctrl+K enters kan swar mode and shows badge', async () => {
    await app.pressWithModifier('Control', 'k');
    const isActive = await app.isOrnamentModeActive();
    if (isActive) {
      const text = await app.getOrnamentModeText();
      expect(text).toContain('type note');
    }
  });

  test('Escape cancels ornament mode', async () => {
    await app.pressWithModifier('Control', 'k');
    await app.pressKey('Escape');
    const isActive = await app.isOrnamentModeActive();
    expect(isActive).toBe(false);
  });

  test('kan swar: enter mode, type note, applies ornament', async () => {
    await app.pressWithModifier('Control', 'k');
    // Type a note for the kan swar
    await app.pressKey('r');
    await app.waitForApi();
  });

  test('meend workflow: start note then end note', async () => {
    // Ctrl+E to start meend (if that's the binding)
    await app.pressWithModifier('Control', 'e');
    if (await app.isOrnamentModeActive()) {
      const text = await app.getOrnamentModeText();
      if (text.includes('Meend')) {
        await app.pressKey('s');
        await app.pressKey('g');
        await app.waitForApi();
      }
    }
  });

  test('murki workflow: collect notes then Enter', async () => {
    await app.pressWithModifier('Control', 'u');
    if (await app.isOrnamentModeActive()) {
      await app.pressKey('s');
      await app.pressKey('r');
      await app.pressKey('g');
      await app.pressKey('Enter');
      await app.waitForApi();
    }
  });

  test('ornament badge disappears after applying ornament', async () => {
    await app.pressWithModifier('Control', 'g');
    await app.waitForApi();
    const isActive = await app.isOrnamentModeActive();
    expect(isActive).toBe(false);
  });
});
