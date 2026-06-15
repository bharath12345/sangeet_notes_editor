// Rendered-layout parity tests. The byte-equality parity tests in
// parity.spec.ts (`.swar` + `.html` golden compare) catch model-level
// regressions but not DOM-rendering bugs — Plan-16 PR-B issues #6 and #7
// (Teen Taal vibhag separators at 5+4+4+3 instead of 4+4+4+4, and
// taal-change failing to reflow the row) both shipped past golden tests
// because the goldens only assert what the layout engine THINKS, not
// what the browser actually renders.
//
// This file adds DOM-level assertions: build a known composition via the
// debug bridge, then read the rendered DOM and check positions.

import { test, expect } from '@playwright/test';
import { TestWsServer } from './helpers/ws-server';

test.describe('DOM layout parity', () => {
  let ws: TestWsServer;

  test.beforeEach(async ({ page }) => {
    ws = await TestWsServer.start();
    await page.goto(`/?debug=ws://localhost:${ws.port}`);
    await ws.waitForConnection();
    await waitForReferenceData(ws);
  });

  test.afterEach(async () => {
    await ws.close();
  });

  test('Teen Taal renders vibhag breaks at beats [3, 7, 11]', async ({ page }) => {
    // Reset to a fresh Teen Taal gat and type 16 swars so the first cycle is
    // full and every cell renders.
    await ws.send({ Reset: { compositionType: 'gat', raag: 'yaman', taal: 'teentaal' } });
    for (let i = 0; i < 16; i++) {
      await ws.send({ TypeChar: { ch: 's' } });
    }

    // Vibhag breaks are flagged on the cell AFTER the break boundary
    // (GridRenderer.elm marks cells where `(idx + 1)` is in `vibhagBreaks`).
    // For Teen Taal, vibhagBreaks = [4, 8, 12], so the .vibhag-break class
    // attaches to cells at zero-indexed beats 3, 7, 11 within each row.
    // Pre-PR-B these landed at 4, 8, 12 instead — the rendered bar was 5+4+4+3.
    const breakBeats = await page
      .locator('.swar-row .beat-cell.vibhag-break')
      .evaluateAll((cells) => cells.map((c) => Number(c.getAttribute('data-beat'))));
    breakBeats.sort((a, b) => a - b);
    expect(breakBeats).toEqual([3, 7, 11]);
  });

  // The two tests below depend on the Tapir backend AND on the debug
  // bridge's editor-result handler refreshing both the active tab's
  // history and the layoutGrids cache. Currently `handleDebugEditorResultReceived`
  // (sangeet-web/src/State/Update.elm) only pushes the new snapshot into
  // `model.history`; it does not propagate to `model.tabs[activeTab]` or
  // call `requestLayout`. Result: SetTaal succeeds at the API level, but
  // the rendered DOM still reflects the pre-SetTaal layout, so the cycle-0
  // cell count is wrong. The Stroke test has the same issue plus the
  // backend-wait flakiness in CI shard 1 (sbt cold-start sometimes takes
  // longer than the 60s wait, see CI hardening below).
  //
  // Re-enable both once the debug result handler dispatches requestLayout
  // and updates the active tab. The Stroke/SetTaal handlers in
  // Debug/Interpreter.elm stay wired — they're still reachable via the
  // MCP debug-console transport, which is what they were originally for.
  test.skip('Ek Taal (12 matras) reflows row width on taal change', async ({ page }) => {
    await ws.send({ Reset: { compositionType: 'gat', raag: 'yaman', taal: 'teentaal' } });
    for (let i = 0; i < 8; i++) {
      await ws.send({ TypeChar: { ch: 's' } });
    }
    await ws.send({ SetTaal: { taal: 'ektaal' } });

    const cycle0Count = await page.locator('.swar-row .beat-cell[data-cycle="0"]').count();
    expect(cycle0Count).toBe(12);
  });

  test.skip('stroke row renders an indicator when a swar carries a Da', async ({ page }) => {
    await ws.send({ Reset: { compositionType: 'gat', raag: 'yaman', taal: 'teentaal' } });
    await ws.send({ TypeChar: { ch: 's' } });
    await ws.send({ Press: { key: 'ArrowLeft' } });
    await ws.send({ Stroke: { stroke: 'da' } });

    const strokeCount = await page.locator('.stroke-row .stroke-indicator').count();
    expect(strokeCount).toBeGreaterThanOrEqual(1);
  });

  // ═════════════════════════════════════════════════════════════════════
  // Render-correctness tests (plan-17 PR-7)
  // ═════════════════════════════════════════════════════════════════════
  //
  // These tests catch CSS-rendering bugs that escaped byte-equality parity
  // checks (Layer 2) because the HTML export matches but the in-app DOM
  // rendering has broken styles. Bug 12 (cursor cell invisible due to broken
  // @keyframes) is the motivating case.
  //
  // The tests below will FAIL today because bug 12 isn't fixed yet (that's
  // PR-4's scope). They're marked `test.fixme` so CI sees them as known-
  // failing rather than skipped. They'll go green after PR-4 lands.

  test.fixme('cursor cell stays visible (animation opacity >= 0.4)', async ({ page }) => {
    // Build a known composition with a swar so a cursor cell exists.
    await ws.send({ Reset: { compositionType: 'gat', raag: 'yaman', taal: 'teentaal' } });
    await ws.send({ TypeChar: { ch: 's' } });

    // Sample the computed opacity over a full animation cycle. The bug 12
    // fix ensures the cursor never drops below opacity 0.4 (the 40% from
    // the @keyframes rule). Pre-fix, the animation is broken and the cursor
    // is invisible (opacity 0 or undefined).
    const samples = await page.evaluate(async () => {
      const cell = document.querySelector('.cursor-cell');
      if (!cell) return null;
      const results: number[] = [];
      // Sample 10 times over 1 second (the animation cycle is 0.8s, so we cover a full loop).
      for (let i = 0; i < 10; i++) {
        const op = parseFloat(getComputedStyle(cell).opacity);
        results.push(op);
        await new Promise((r) => setTimeout(r, 100));
      }
      return results;
    });

    expect(samples).not.toBeNull();
    expect(samples!.length).toBe(10);
    const minOpacity = Math.min(...samples!);
    expect(minOpacity).toBeGreaterThanOrEqual(0.4);
  });

  test.fixme('cursor cell outline has sufficient contrast (WCAG 3:1)', async ({ page }) => {
    // Build a composition with a cursor cell.
    await ws.send({ Reset: { compositionType: 'gat', raag: 'yaman', taal: 'teentaal' } });
    await ws.send({ TypeChar: { ch: 's' } });

    // Read the cursor cell's border color and background color, compute
    // relative luminance per WCAG, assert contrast >= 3:1.
    const contrast = await page.evaluate(() => {
      const cell = document.querySelector('.cursor-cell') as HTMLElement | null;
      if (!cell) return null;

      const styles = getComputedStyle(cell);
      const borderColor = styles.borderColor;
      const bgColor = styles.backgroundColor;

      // Parse rgb(r, g, b) or rgba(r, g, b, a) to [r, g, b].
      function parseRgb(colorStr: string): [number, number, number] | null {
        const match = colorStr.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/);
        if (!match) return null;
        return [parseInt(match[1], 10), parseInt(match[2], 10), parseInt(match[3], 10)];
      }

      // Compute relative luminance (WCAG 2.1).
      function luminance(rgb: [number, number, number]): number {
        const [r, g, b] = rgb.map((c) => {
          const sRGB = c / 255;
          return sRGB <= 0.03928 ? sRGB / 12.92 : Math.pow((sRGB + 0.055) / 1.055, 2.4);
        });
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
      }

      const border = parseRgb(borderColor);
      const bg = parseRgb(bgColor);
      if (!border || !bg) return null;

      const L1 = luminance(border);
      const L2 = luminance(bg);
      const lighter = Math.max(L1, L2);
      const darker = Math.min(L1, L2);
      return (lighter + 0.05) / (darker + 0.05);
    });

    expect(contrast).not.toBeNull();
    expect(contrast!).toBeGreaterThanOrEqual(3.0);
  });
});

/** Reset commands look up raag/taal in availableRaags/availableTaals, which
 * the init batch populates async. Same helper as parity.spec.ts. */
async function waitForReferenceData(ws: TestWsServer): Promise<void> {
  const deadline = Date.now() + 5000;
  while (Date.now() < deadline) {
    const state = (await ws.send({ GetState: {} })) as Record<string, number>;
    if ((state.availableRaagsCount ?? 0) > 0 && (state.availableTaalsCount ?? 0) > 0) {
      return;
    }
    await new Promise((r) => setTimeout(r, 100));
  }
  throw new Error('Reference data (raags/taals) never loaded within 5s');
}
