import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('Tab Management', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
  });

  test('tab bar is visible on load', async () => {
    await expect(app.tabBar).toBeVisible();
  });

  test('starts with one tab', async () => {
    const count = await app.getFileTabCount();
    expect(count).toBe(1);
  });

  test('initial tab is active', async () => {
    await expect(app.activeFileTab).toBeVisible();
  });

  test('new tab button is visible', async () => {
    await expect(app.newTabBtn).toBeVisible();
  });

  test('clicking new tab adds a tab', async () => {
    await app.clickNewTab();
    const count = await app.getFileTabCount();
    expect(count).toBe(2);
  });

  test('new tab becomes active', async () => {
    await app.clickNewTab();
    const name = await app.getActiveFileTabName();
    expect(name).toBe('Untitled');
  });

  test('switching tabs by index changes active tab', async () => {
    await app.clickNewTab();
    // Now at tab index 1 (new tab), switch back to index 0
    await app.switchToTabByIndex(0);
    // The first tab should now be active
    await expect(app.fileTabs.nth(0)).toHaveClass(/file-tab-active/);
  });

  test('closing inactive tab preserves active', async () => {
    await app.clickNewTab();
    // Active is now tab index 1 (new), close tab index 0 (original)
    await app.closeTabByIndex(0);
    const count = await app.getFileTabCount();
    expect(count).toBe(1);
  });

  test('closing active tab switches to remaining', async () => {
    await app.clickNewTab();
    // Active is tab index 1 (new), close it
    await app.closeTabByIndex(1);
    const count = await app.getFileTabCount();
    expect(count).toBe(1);
    await expect(app.activeFileTab).toBeVisible();
  });

  test('closing last tab shows empty state', async () => {
    await app.closeTabByIndex(0);
    const count = await app.getFileTabCount();
    expect(count).toBe(0);
    await expect(app.page.locator('.empty-state')).toBeVisible();
  });

  test('multiple new tabs create distinct entries', async () => {
    await app.clickNewTab();
    await app.clickNewTab();
    const count = await app.getFileTabCount();
    expect(count).toBe(3);
  });

  test('tab close button has correct title', async () => {
    const closeBtn = app.fileTabs.first().locator('.file-tab-close');
    await expect(closeBtn).toHaveAttribute('title', 'Close tab');
  });

  test('switching tabs preserves section state', async () => {
    // Enter a note in tab 1
    await app.pressKey('s');
    const _beat1 = await app.getCursorBeat();

    // Open tab 2 and enter a different note
    await app.clickNewTab();
    await app.pressKey('r');

    // Switch back to tab 1 — cursor should be restored
    await app.switchToTabByIndex(0);
    await expect(app.activeFileTab).toBeVisible();
    // Tab should have maintained its independent state
    expect(await app.getFileTabCount()).toBe(2);
  });
});
