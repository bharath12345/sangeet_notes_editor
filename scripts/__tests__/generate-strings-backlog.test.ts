import { describe, it, expect } from 'vitest';
import { generateBacklog } from '../generate-strings-backlog.ts';
import { Entry } from '../lib/catalog.ts';

const desk = (value: string, extra: Partial<Entry> = {}): Entry => ({
  value,
  platform: 'desktop',
  description: '',
  ...extra,
});
const web = (value: string, extra: Partial<Entry> = {}): Entry => ({
  value,
  platform: 'web',
  description: '',
  ...extra,
});

describe('generateBacklog', () => {
  it('includes entries with disposition but no dispositionNote', () => {
    const out = generateBacklog({
      'toolbar.file.new': desk('New', { disposition: 'port-to-web' }),
    });
    expect(out).toContain('toolbar.file.new');
    expect(out).toContain('PORT→web');
    expect(out).toContain('Total PENDING entries: **1**');
  });

  it('excludes entries with a dispositionNote (DONE)', () => {
    const out = generateBacklog({
      'toolbar.file.new': desk('New', {
        disposition: 'port-to-web',
        dispositionNote: 'ported in abc123',
      }),
    });
    expect(out).not.toContain('toolbar.file.new');
    expect(out).toContain('Total PENDING entries: **0**');
  });

  it('excludes entries with no disposition (undecided)', () => {
    const out = generateBacklog({
      'toolbar.file.new': desk('New'),
    });
    expect(out).not.toContain('toolbar.file.new');
    expect(out).toContain('Total PENDING entries: **0**');
  });

  it('groups pending entries by component prefix', () => {
    const out = generateBacklog({
      'toolbar.file.new': desk('New', { disposition: 'port-to-web' }),
      'toolbar.file.open': desk('Open', { disposition: 'port-to-web' }),
      'dialog.about.title': web('About', { disposition: 'accept' }),
    });
    expect(out).toMatch(/### toolbar\.file {2}\(2 pending\)/);
    expect(out).toMatch(/### dialog\.about {2}\(1 pending\)/);
  });

  it('rolls up counts per disposition', () => {
    const out = generateBacklog({
      a: desk('A', { disposition: 'port-to-web' }),
      b: desk('B', { disposition: 'port-to-web' }),
      c: web('C', { disposition: 'accept' }),
    });
    expect(out).toMatch(/\| PORT→web \| 2 \|/);
    expect(out).toMatch(/\| ACCEPT \| 1 \|/);
  });

  it('escapes pipe characters in values', () => {
    const out = generateBacklog({
      'x.y.z': desk('left | right', { disposition: 'port-to-web' }),
    });
    expect(out).toContain('left \\| right');
  });

  it('truncates long values', () => {
    const long = 'a'.repeat(100);
    const out = generateBacklog({
      'x.y.z': desk(long, { disposition: 'port-to-web' }),
    });
    expect(out).toContain('a'.repeat(57) + '…');
  });
});
