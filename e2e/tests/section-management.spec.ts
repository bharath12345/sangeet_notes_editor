import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('Section Management', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
  });

  test('initial section tab is visible', async () => {
    const count = await app.getSectionCount();
    expect(count).toBeGreaterThanOrEqual(1);
  });

  test('active section tab has active class', async () => {
    const active = app.page.locator('.section-tab-active');
    expect(await active.count()).toBe(1);
  });

  test('add section button is visible', async () => {
    await expect(app.addSectionBtn).toBeVisible();
    const text = await app.addSectionBtn.textContent();
    expect(text).toContain('+');
  });

  test('clicking + adds a new section', async () => {
    const before = await app.getSectionCount();
    await app.addSectionBtn.click();
    await app.waitForApi();
    const after = await app.getSectionCount();
    expect(after).toBe(before + 1);
  });

  test('clicking a section tab switches to that section', async () => {
    // Add a section first
    await app.addSectionBtn.click();
    await app.waitForApi();

    // Click the first section tab
    const firstTab = app.sectionTabs.first();
    await firstTab.click();
    await app.waitForApi();

    await expect(firstTab).toHaveClass(/section-tab-active/);
  });

  test('adding section preserves content in original section', async () => {
    // Insert a note in first section
    await app.pressKey('s');
    const glyphsBefore = await app.page.locator('.swar-glyph').count();

    // Add and switch to new section
    await app.addSectionBtn.click();
    await app.waitForApi();

    // Switch back to first section
    await app.sectionTabs.first().click();
    await app.waitForApi();

    const glyphsAfter = await app.page.locator('.swar-glyph').count();
    expect(glyphsAfter).toBeGreaterThanOrEqual(glyphsBefore);
  });
});
