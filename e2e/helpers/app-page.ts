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
  readonly pdfBtn: Locator;
  readonly htmlBtn: Locator;
  readonly undoBtn: Locator;
  readonly redoBtn: Locator;
  readonly strokesBtn: Locator;
  readonly sahityaBtn: Locator;
  readonly keysBtn: Locator;
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

  // Keyboard legend
  readonly keyboardLegend: Locator;

  constructor(page: Page) {
    this.page = page;
    this.appContainer = page.locator('#app-container');
    this.toolbar = page.locator('.toolbar');
    this.mainContent = page.locator('.main-content');
    this.statusBar = page.locator('.status-bar');
    this.loadingIndicator = page.locator('.loading-indicator');

    this.newBtn = page.locator('button[title="New Composition (Ctrl+N)"]');
    this.openBtn = page.locator('button[title="Open File"]');
    this.saveBtn = page.locator('button[title="Save File (Ctrl+S)"]');
    this.pdfBtn = page.locator('button[title="Export PDF"]');
    this.htmlBtn = page.locator('button[title="Export HTML"]');
    this.undoBtn = page.locator('button[title="Undo (Ctrl+Z)"]');
    this.redoBtn = page.locator('button[title="Redo (Ctrl+Y)"]');
    this.strokesBtn = page.locator('button[title="Toggle Stroke Line"]');
    this.sahityaBtn = page.locator('button[title="Toggle Sahitya Line"]');
    this.keysBtn = page.locator('button[title="Keyboard Shortcuts"]');
    this.propsBtn = page.locator('button[title="Composition Properties"]');
    this.aboutBtn = page.locator('button[title="About"]');

    this.scriptSelect = page.locator('.script-select');

    this.swarRow = page.locator('.swar-row');
    this.cursorCell = page.locator('.cursor-cell');
    this.sectionTabs = page.locator('.section-tab');
    this.addSectionBtn = page.locator('.section-tab-add');

    this.modalOverlay = page.locator('.modal-overlay');
    this.modalDialog = page.locator('.modal-dialog');

    this.keyboardLegend = page.locator('.keyboard-legend');
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

  async getEditMode(): Promise<string> {
    const label = this.page.locator('.toolbar-label');
    const text = await label.first().textContent();
    return text?.trim() ?? '';
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
}
