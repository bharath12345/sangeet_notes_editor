import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import { TestWsServer } from './helpers/ws-server';
import { assertMatchesGolden, loadDefinitions } from './helpers/golden-fixtures';
import { TestDefinition, ExpectedState } from './helpers/test-definition';

for (const { name, path: filePath } of loadDefinitions()) {
  test(name, async ({ page }) => {
    const defn: TestDefinition = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
    const ws = await TestWsServer.start();
    try {
      await page.goto(`/?debug=ws://localhost:${ws.port}`);
      await ws.waitForConnection();
      await waitForReferenceData(ws);

      for (const step of defn.steps) {
        if ('Cmd' in step) {
          await ws.send(step.Cmd.cmd);
        } else if ('Checkpoint' in step) {
          const state = await ws.send({ GetState: {} });
          assertCheckpoint(state, step.Checkpoint.expect);
        } else if ('AssertGoldenSwar' in step) {
          const actual = (await ws.send({ DumpComposition: {} })) as string;
          assertMatchesGolden(actual, step.AssertGoldenSwar.fixture);
        } else if ('AssertGoldenHtml' in step) {
          const actual = (await ws.send({ ExportHtml: {} })) as string;
          assertMatchesGolden(actual, step.AssertGoldenHtml.fixture);
        }
      }
    } finally {
      await ws.close();
    }
  });
}

function assertCheckpoint(state: unknown, expect_: ExpectedState): void {
  for (const key of Object.keys(expect_) as (keyof ExpectedState)[]) {
    if (expect_[key] !== undefined) {
      expect((state as Record<string, unknown>)[key]).toEqual(expect_[key]);
    }
  }
}

/** Reset commands look up raag/taal by name in availableRaags/availableTaals,
 * which are populated by /raags and /taals fetches fired from init. Under CI
 * load those fetches can land after the first WS frame is in flight. Poll
 * GetState until both lists are non-empty before issuing any test commands. */
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
