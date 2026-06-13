import * as fs from 'fs';
import * as path from 'path';
import { expect } from '@playwright/test';

const GOLDEN_ROOT = path.resolve(__dirname, '../../../tests/integration');

export function assertMatchesGolden(actual: string, fixturePath: string): void {
  const fullPath = path.resolve(GOLDEN_ROOT, fixturePath);
  const expected = fs.readFileSync(fullPath, 'utf-8');
  expect(actual).toEqual(expected); // byte-equality
}

export function loadDefinitions(): { name: string; path: string }[] {
  const dir = path.join(GOLDEN_ROOT);
  return fs
    .readdirSync(dir)
    .filter((f) => f.endsWith('.json'))
    .sort()
    .map((f) => ({ name: f.replace(/\.json$/, ''), path: path.join(dir, f) }));
}
