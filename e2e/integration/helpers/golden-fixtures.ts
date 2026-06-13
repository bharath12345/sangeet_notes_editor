import * as fs from 'fs';
import * as path from 'path';
import { expect } from '@playwright/test';

const GOLDEN_ROOT = path.resolve(__dirname, '../../../tests/integration');

/** Volatile fields that differ on every test run (timestamps). */
const VOLATILE_FIELDS = new Set(['createdAt', 'updatedAt']);

export function assertMatchesGolden(actual: string, fixturePath: string): void {
  const fullPath = path.resolve(GOLDEN_ROOT, fixturePath);
  const expected = fs.readFileSync(fullPath, 'utf-8');

  // For .swar files, compare semantically after stripping volatile fields.
  // For .html files, use byte-equality (no timestamps in HTML output).
  if (fixturePath.endsWith('.swar')) {
    assertSwarEquivalent(actual, expected);
  } else {
    expect(actual).toEqual(expected);
  }
}

export function loadDefinitions(): { name: string; path: string }[] {
  const dir = path.join(GOLDEN_ROOT);
  return fs
    .readdirSync(dir)
    .filter((f) => f.endsWith('.json'))
    .sort()
    .map((f) => ({ name: f.replace(/\.json$/, ''), path: path.join(dir, f) }));
}

/** Compare .swar JSON semantically after stripping volatile fields (createdAt, updatedAt). */
function assertSwarEquivalent(actualJson: string, expectedJson: string): void {
  const actual = JSON.parse(actualJson);
  const expected = JSON.parse(expectedJson);

  const normalizedActual = stripVolatileFields(actual);
  const normalizedExpected = stripVolatileFields(expected);

  expect(normalizedActual).toEqual(normalizedExpected);
}

/** Recursively remove volatile fields from an object/array tree. */
function stripVolatileFields(obj: any): any {
  if (obj === null || typeof obj !== 'object') return obj;

  if (Array.isArray(obj)) {
    return obj.map(stripVolatileFields);
  }

  const result: any = {};
  for (const key of Object.keys(obj)) {
    if (!VOLATILE_FIELDS.has(key)) {
      result[key] = stripVolatileFields(obj[key]);
    }
  }
  return result;
}
