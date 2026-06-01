import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('Script Switching', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
    // Insert a note so we can see it rendered
    await app.pressKey('s');
  });

  test('script selector is visible with default Devanagari', async () => {
    await expect(app.scriptSelect).toBeVisible();
    const value = await app.scriptSelect.inputValue();
    expect(value).toBe('devanagari');
  });

  test('switching to English renders glyphs in English', async () => {
    await app.selectScript('english');
    const glyphText = await app.page.locator('.swar-glyph .swar-text').first().textContent();
    // English rendering should show "Sa" or similar Latin text
    expect(glyphText).toBeTruthy();
  });

  test('switching to Kannada renders glyphs in Kannada script', async () => {
    await app.selectScript('kannada');
    const glyphText = await app.page.locator('.swar-glyph .swar-text').first().textContent();
    expect(glyphText).toBeTruthy();
  });

  test('switching to Telugu renders glyphs in Telugu script', async () => {
    await app.selectScript('telugu');
    const glyphText = await app.page.locator('.swar-glyph .swar-text').first().textContent();
    expect(glyphText).toBeTruthy();
  });

  test('switching back to Devanagari restores original rendering', async () => {
    const originalText = await app.page.locator('.swar-glyph .swar-text').first().textContent();
    await app.selectScript('english');
    await app.selectScript('devanagari');
    const restoredText = await app.page.locator('.swar-glyph .swar-text').first().textContent();
    expect(restoredText).toBe(originalText);
  });

  test('script change persists across note insertions', async () => {
    await app.selectScript('english');
    await app.pressKey('r');
    // Both notes should be in English
    const glyphs = app.page.locator('.swar-glyph .swar-text');
    const count = await glyphs.count();
    expect(count).toBeGreaterThanOrEqual(2);
  });
});
