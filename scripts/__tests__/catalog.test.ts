import { describe, it, expect, afterEach } from 'vitest';
import { writeFileSync, mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { loadCatalog } from '../lib/catalog.ts';

const tmpDirs: string[] = [];
function tmpFile(contents: object): string {
  const dir = mkdtempSync(join(tmpdir(), 'catalog-test-'));
  tmpDirs.push(dir);
  const path = join(dir, 'catalog.json');
  writeFileSync(path, JSON.stringify(contents), 'utf-8');
  return path;
}

afterEach(() => {
  for (const d of tmpDirs.splice(0)) rmSync(d, { recursive: true, force: true });
});

describe('loadCatalog disposition handling', () => {
  it('accepts entries without disposition', () => {
    const c = loadCatalog(
      tmpFile({
        entries: { 'a.b': { value: 'x', platform: 'desktop', description: '' } },
      }),
    );
    expect(c.entries['a.b'].disposition).toBeUndefined();
  });

  it('accepts valid disposition values', () => {
    for (const d of ['port-to-web', 'port-to-desk', 'accept', 'normalize']) {
      const c = loadCatalog(
        tmpFile({
          entries: { 'a.b': { value: 'x', platform: 'desktop', description: '', disposition: d } },
        }),
      );
      expect(c.entries['a.b'].disposition).toBe(d);
    }
  });

  it('preserves dispositionNote field', () => {
    const c = loadCatalog(
      tmpFile({
        entries: {
          'a.b': {
            value: 'x',
            platform: 'desktop',
            description: '',
            disposition: 'accept',
            dispositionNote: 'ported in abc123',
          },
        },
      }),
    );
    expect(c.entries['a.b'].dispositionNote).toBe('ported in abc123');
  });

  it('rejects invalid disposition values', () => {
    expect(() =>
      loadCatalog(
        tmpFile({
          entries: {
            'a.b': { value: 'x', platform: 'desktop', description: '', disposition: 'bogus' },
          },
        }),
      ),
    ).toThrow(/invalid disposition/);
  });
});
