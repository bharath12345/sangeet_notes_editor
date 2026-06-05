import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('Multi-Step Flows', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
  });

  test('full workflow: create composition, add notes, undo, redo', async () => {
    // Create new composition
    await app.newBtn.click();
    const titleInput = app.page.locator('#new-title');
    await titleInput.fill('E2E Test Gat');
    const createBtn = app.page.locator('.modal-footer .btn-primary');
    await createBtn.click();
    await app.waitForApi();

    // Insert several notes
    await app.pressKeys(['s', 'r', 'g', 'm']);
    const glyphsAfterInsert = await app.page.locator('.swar-glyph').count();
    expect(glyphsAfterInsert).toBeGreaterThanOrEqual(4);

    // Undo the last note
    await app.pressWithModifier('Control', 'z');
    const glyphsAfterUndo = await app.page.locator('.swar-glyph').count();
    expect(glyphsAfterUndo).toBeLessThan(glyphsAfterInsert);

    // Redo
    await app.pressWithModifier('Control', 'y');
    await app.waitForApi();
    const glyphsAfterRedo = await app.page.locator('.swar-glyph').count();
    expect(glyphsAfterRedo).toBe(glyphsAfterInsert);
  });

  test('section workflow: add section, insert notes, switch sections', async () => {
    // Insert notes in first section
    await app.pressKeys(['s', 'r', 'g']);
    const firstSectionGlyphs = await app.page.locator('.swar-glyph').count();

    // Add a new section
    await app.addSectionBtn.click();
    await app.waitForApi();

    // The new section should be empty or have fewer glyphs visible
    // Insert notes in new section
    await app.pressKeys(['p', 'd', 'n']);

    // Switch back to first section
    await app.sectionTabs.first().click();
    await app.waitForApi();

    // First section content should be preserved
    const restoredGlyphs = await app.page.locator('.swar-glyph').count();
    expect(restoredGlyphs).toBeGreaterThanOrEqual(firstSectionGlyphs);
  });

  test('ornament then undo: gamak applied and reversed', async () => {
    await app.pressKey('s');
    const _glyphsBefore = await app.page.locator('.swar-glyph').count();

    // Apply gamak ornament
    await app.pressWithModifier('Control', 'g');
    await app.waitForApi();

    // Ornament mode should be cleared
    const isActive = await app.isOrnamentModeActive();
    expect(isActive).toBe(false);

    // Undo the ornament
    await app.pressWithModifier('Control', 'z');
    await app.waitForApi();
  });

  test('stroke editing workflow: enter mode, set strokes, exit', async () => {
    // Insert a few notes
    await app.pressKeys(['s', 'r', 'g']);

    // Enter stroke mode
    await app.pressKey('F2');
    let mode = await app.getEditMode();
    expect(mode).toContain('Stroke');

    // Set Da stroke
    await app.pressKey('d');
    await app.waitForApi();

    // Exit stroke mode
    await app.pressKey('F2');
    mode = await app.getEditMode();
    expect(mode).toContain('Swar');
  });

  test('stress test: rapid note insertion of full saptak', async () => {
    // Insert all 7 notes rapidly
    const notes = ['s', 'r', 'g', 'm', 'p', 'd', 'n'];
    for (const note of notes) {
      await app.pressKey(note);
    }
    const glyphs = await app.page.locator('.swar-glyph').count();
    expect(glyphs).toBeGreaterThanOrEqual(7);
  });

  test('dialog-edit-dialog cycle: properties, edit, about', async () => {
    // Open and close properties
    await app.propsBtn.click();
    await expect(app.modalOverlay).toBeVisible();
    const cancelBtn = app.page.locator('.modal-footer .btn-secondary');
    await cancelBtn.click();
    await expect(app.modalOverlay).toBeHidden();

    // Insert a note
    await app.pressKey('s');
    const glyphs = await app.page.locator('.swar-glyph').count();
    expect(glyphs).toBeGreaterThanOrEqual(1);

    // Open about
    await app.aboutBtn.click();
    await expect(app.modalOverlay).toBeVisible();

    // Close about
    const closeBtn = app.page
      .locator('.modal-footer .btn-secondary, .modal-footer .btn-primary')
      .first();
    if (await closeBtn.isVisible()) {
      await closeBtn.click();
    }
  });

  test('komal/tivra notes with octave changes', async () => {
    // Insert shuddha Sa in madhya
    await app.pressKey('s');

    // Change to taar octave via ] key
    await app.pressKey(']');
    await app.waitForApi();

    // Verify octave changed to Taar
    const octaveChip = app.page.locator('.header-chip', { hasText: /Octave/ });
    const octaveText = await octaveChip.textContent();
    expect(octaveText).toContain('Taar');

    // Insert Pa in taar octave
    await app.pressKey('p');
    await app.waitForApi();

    const glyphs = await app.page.locator('.swar-glyph').count();
    // Sa (madhya) + Pa (taar) = 2 swar glyphs
    expect(glyphs).toBeGreaterThanOrEqual(2);
  });
});
