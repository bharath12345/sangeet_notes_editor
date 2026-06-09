import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('File Browser Panel', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
  });

  test('file browser panel is present', async () => {
    await expect(app.fileBrowserPanel).toBeVisible();
  });

  test('file browser starts collapsed', async () => {
    const collapsed = await app.isFileBrowserCollapsed();
    expect(collapsed).toBe(true);
  });

  test('toggle button expands collapsed panel', async () => {
    await app.toggleFileBrowser();
    const collapsed = await app.isFileBrowserCollapsed();
    expect(collapsed).toBe(false);
  });

  test('toggle button collapses expanded panel', async () => {
    await app.toggleFileBrowser(); // expand
    await app.toggleFileBrowser(); // collapse
    const collapsed = await app.isFileBrowserCollapsed();
    expect(collapsed).toBe(true);
  });

  test('expanded panel shows Drive connect button when disconnected', async () => {
    await app.toggleFileBrowser();
    await expect(app.driveConnectBtn).toBeVisible();
  });

  test('expanded panel shows panel title', async () => {
    await app.toggleFileBrowser();
    const title = app.page.locator('.panel-title');
    await expect(title).toHaveText('Files');
  });

  test('expanded panel shows empty folder tree message', async () => {
    await app.toggleFileBrowser();
    const emptyMsg = app.page.locator('.folder-tree-empty');
    await expect(emptyMsg).toBeVisible();
    await expect(emptyMsg).toContainText('Connect Drive');
  });

  test('Drive connect button text', async () => {
    await app.toggleFileBrowser();
    await expect(app.driveConnectBtn).toContainText('Connect Google Drive');
  });

  test('collapsed panel shows toggle button', async () => {
    await expect(app.panelToggleBtn.first()).toBeVisible();
  });
});
