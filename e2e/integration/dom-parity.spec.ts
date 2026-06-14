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

  // Two more DOM-layout assertions are valuable but blocked on the debug
  // bridge growing real implementations of SetTaal and Stroke (today both
  // return `errResp` with "not implemented"). Once that wiring lands,
  // restore the tests below.
  //
  // test('Ek Taal (12 matras) reflows row width on taal change', ...)
  //   → assert `.swar-row .beat-cell[data-cycle="0"]` count is 12 after
  //   switching from Teen Taal to Ek Taal mid-edit.
  //
  // test('stroke row renders when a swar carries a stroke', ...)
  //   → assert `.stroke-row .stroke-indicator` count is ≥ 1 after
  //   attaching a Da via Stroke command.
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
