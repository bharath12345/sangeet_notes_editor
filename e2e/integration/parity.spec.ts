import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import { TestWsServer } from './helpers/ws-server';
import { assertMatchesGolden, loadDefinitions } from './helpers/golden-fixtures';
import { TestDefinition, TestStep, ExpectedState } from './helpers/test-definition';

for (const { name, path: filePath } of loadDefinitions()) {
  test(name, async ({ page }) => {
    const defn: TestDefinition = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
    const ws = await TestWsServer.start();
    try {
      await page.goto(`/?debug=ws://localhost:${ws.port}`);
      await ws.waitForConnection();

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
