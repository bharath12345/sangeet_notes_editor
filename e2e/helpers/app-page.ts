import { Page, Locator } from '@playwright/test';

export class SangeetPage {
  readonly page: Page;

  // Top-level containers
  readonly appContainer: Locator;
  readonly toolbar: Locator;
  readonly mainContent: Locator;
  readonly statusBar: Locator;
  readonly loadingIndicator: Locator;

  // Toolbar buttons (by title)
  readonly newBtn: Locator;
  readonly openBtn: Locator;
  readonly saveBtn: Locator;
  readonly cutBtn: Locator;
  readonly copyBtn: Locator;
  readonly pasteBtn: Locator;
  readonly htmlBtn: Locator;
  readonly undoBtn: Locator;
  readonly redoBtn: Locator;
  readonly saveAsBtn: Locator;
  readonly cheatSheetBtn: Locator;
  readonly propsBtn: Locator;
  readonly aboutBtn: Locator;

  // Script selector
  readonly scriptSelect: Locator;

  // Grid
  readonly swarRow: Locator;
  readonly cursorCell: Locator;
  readonly sectionTabs: Locator;
  readonly addSectionBtn: Locator;

  // Dialogs
  readonly modalOverlay: Locator;
  readonly modalDialog: Locator;

  // Tab bar
  readonly tabBar: Locator;
  readonly fileTabs: Locator;
  readonly activeFileTab: Locator;
  readonly newTabBtn: Locator;

  // File browser panel
  readonly fileBrowserPanel: Locator;
  readonly panelToggleBtn: Locator;
  readonly driveConnectBtn: Locator;
  readonly driveStatus: Locator;
  readonly folderTree: Locator;
  readonly folderItems: Locator;

  constructor(page: Page) {
    this.page = page;
    this.appContainer = page.locator('#app-container');
    this.toolbar = page.locator('.toolbar');
    this.mainContent = page.locator('.main-content');
    this.statusBar = page.locator('.status-bar');
    this.loadingIndicator = page.locator('.loading-indicator');

    // Tooltip strings match sangeet-core/src/main/resources/ui-strings.json
    // (consumed via sangeet-web/src/UiStrings.elm). Keep these in sync with the
    // catalog when tooltips change.
    this.newBtn = page.locator('button[title="Create a new composition (Ctrl+N)"]');
    this.openBtn = page.locator('button[title="Open a .swar file (Ctrl+O)"]');
    this.saveBtn = page.locator('button[title="Save composition to current file (Ctrl+S)"]');
    this.cutBtn = page.locator('button[title="Cut selected events (Ctrl+X)"]');
    this.copyBtn = page.locator('button[title="Copy selected events (Ctrl+C)"]');
    this.pasteBtn = page.locator('button[title="Paste clipboard events (Ctrl+V)"]');
    this.htmlBtn = page.locator('button[title="Export composition as HTML (Ctrl+E)"]');
    this.undoBtn = page.locator('button[title="Undo last edit (Ctrl+Z)"]');
    this.redoBtn = page.locator('button[title="Redo (Ctrl+Y)"]');
    // PR-A consolidated buttons: Strokes/Sahitya toggles removed (rows always render).
    // PR-C C.3 added Save As; PR-C C.4 retired the Legend toggle (cheat sheet now
    // owns both the shortcuts and the keyboard reference).
    this.saveAsBtn = page.locator('button[title^="Save composition as a new .swar file"]');
    this.cheatSheetBtn = page.locator('button:has-text("⌨ Keys")');
    this.propsBtn = page.locator('button[title="Edit composition metadata"]');
    this.aboutBtn = page.locator('button[title="About Sangeet Notes Editor"]');

    this.scriptSelect = page.locator('.script-select');

    this.swarRow = page.locator('.swar-row');
    this.cursorCell = page.locator('.cursor-cell');
    this.sectionTabs = page.locator('.section-tab');
    this.addSectionBtn = page.locator('.section-tab-add');

    this.modalOverlay = page.locator('.modal-overlay');
    this.modalDialog = page.locator('.modal-dialog');

    this.tabBar = page.locator('.toolbar-row-tabs');
    this.fileTabs = page.locator('.file-tab:not(.file-tab-add)');
    this.activeFileTab = page.locator('.file-tab-active');
    this.newTabBtn = page.locator('.file-tab-add');

    this.fileBrowserPanel = page.locator('.file-browser-panel');
    this.panelToggleBtn = page.locator('.panel-toggle-btn');
    this.driveConnectBtn = page.locator('.drive-connect-btn');
    this.driveStatus = page.locator('.drive-status');
    this.folderTree = page.locator('.folder-tree');
    this.folderItems = page.locator('.folder-item');
  }

  async goto() {
    await this.page.goto('/');
    await this.appContainer.waitFor({ state: 'visible', timeout: 10000 });
    // Wait for initial API calls to complete (taals, raags, colors, scripts)
    await this.waitForApi();
  }

  async waitForApi(timeout = 5000) {
    // Wait for loading indicator to appear then disappear, or just settle
    try {
      await this.loadingIndicator.waitFor({ state: 'visible', timeout: 500 });
      await this.loadingIndicator.waitFor({ state: 'hidden', timeout });
    } catch {
      // Loading indicator might not appear for fast operations
    }
    // Extra settle time for DOM updates
    await this.page.waitForTimeout(100);
  }

  async pressKey(key: string) {
    await this.appContainer.focus();
    await this.page.keyboard.press(key);
    await this.waitForApi();
  }

  async pressKeys(keys: string[]) {
    await this.appContainer.focus();
    for (const key of keys) {
      await this.page.keyboard.press(key);
      await this.waitForApi();
    }
  }

  async typeText(text: string) {
    await this.appContainer.focus();
    for (const char of text) {
      await this.page.keyboard.press(char);
      await this.waitForApi();
    }
  }

  async pressWithModifier(mod: 'Control' | 'Shift' | 'Alt', key: string) {
    await this.appContainer.focus();
    await this.page.keyboard.press(`${mod}+${key}`);
    await this.waitForApi();
  }

  async clickButton(title: string) {
    await this.page.locator(`button[title="${title}"]`).click();
    await this.waitForApi();
  }

  async getBeatCell(beat: number, cycle: number = 0): Promise<Locator> {
    return this.page.locator(`.swar-row td[data-beat="${beat}"][data-cycle="${cycle}"]`);
  }

  async getCursorBeatContent(): Promise<string> {
    // cursor-cell only exists on beats with events; try swar-glyph first
    const glyphs = this.page.locator('.swar-glyph');
    if ((await glyphs.count()) > 0) {
      return (await glyphs.last().textContent())?.trim() ?? '';
    }
    return '';
  }

  async getCursorBeat(): Promise<number> {
    const chips = this.page.locator('.header-chip');
    const count = await chips.count();
    for (let i = 0; i < count; i++) {
      const text = await chips.nth(i).textContent();
      const match = text?.match(/Beat (\d+)\/\d+/);
      if (match) return parseInt(match[1]);
    }
    return -1;
  }

  async getCursorCycle(): Promise<number> {
    const chips = this.page.locator('.header-chip');
    const count = await chips.count();
    for (let i = 0; i < count; i++) {
      const text = await chips.nth(i).textContent();
      const match = text?.match(/Cycle (\d+)/);
      if (match) return parseInt(match[1]);
    }
    return -1;
  }

  async getActiveSection(): Promise<string> {
    const active = this.page.locator('.section-tab-active');
    return (await active.textContent())?.trim() ?? '';
  }

  async getSectionCount(): Promise<number> {
    return this.sectionTabs.count();
  }

  async getStatusLog(): Promise<string[]> {
    const entries = this.page.locator('.status-entry');
    const count = await entries.count();
    const logs: string[] = [];
    for (let i = 0; i < count; i++) {
      const text = await entries.nth(i).textContent();
      if (text) logs.push(text.trim());
    }
    return logs;
  }

  async isOrnamentModeActive(): Promise<boolean> {
    const badge = this.page.locator('.ornament-badge');
    return (await badge.count()) > 0;
  }

  async getOrnamentModeText(): Promise<string> {
    const badge = this.page.locator('.ornament-badge');
    if ((await badge.count()) === 0) return '';
    return (await badge.textContent())?.trim() ?? '';
  }

  async getCompositionTitle(): Promise<string> {
    const title = this.page.locator('.composition-title');
    return (await title.textContent())?.trim() ?? '';
  }

  async selectScript(script: string) {
    await this.scriptSelect.selectOption(script);
    await this.waitForApi();
  }

  async getFileTabCount(): Promise<number> {
    return this.fileTabs.count();
  }

  async getActiveFileTabName(): Promise<string> {
    const label = this.activeFileTab.locator('.file-tab-label');
    return (await label.textContent())?.trim() ?? '';
  }

  async clickNewTab() {
    await this.newTabBtn.click();
    await this.waitForApi();
  }

  async switchToTab(name: string) {
    const tab = this.page.locator(`.file-tab-label:has-text("${name}")`).first();
    await tab.click();
    await this.waitForApi();
  }

  async switchToTabByIndex(index: number) {
    const tab = this.fileTabs.nth(index).locator('.file-tab-label');
    await tab.click();
    await this.waitForApi();
  }

  async closeTab(name: string) {
    const tab = this.page.locator(`.file-tab:has-text("${name}")`).first();
    await tab.locator('.file-tab-close').click();
    await this.waitForApi();
  }

  async closeTabByIndex(index: number) {
    const tab = this.fileTabs.nth(index).locator('.file-tab-close');
    await tab.click();
    await this.waitForApi();
  }

  async isFileBrowserCollapsed(): Promise<boolean> {
    return (
      (await this.fileBrowserPanel.getAttribute('class'))?.includes('file-browser-collapsed') ??
      false
    );
  }

  async toggleFileBrowser() {
    await this.panelToggleBtn.first().click();
    await this.waitForApi();
  }
}
