import { describe, it, expect } from 'vitest';
import { checkParity } from '../check-string-parity.ts';
import { keyToElmIdent } from '../lib/catalog.ts';

const both = (k: string) => ({ value: 'x', platform: 'both' as const, description: '' });
const desk = (k: string) => ({ value: 'x', platform: 'desktop' as const, description: '' });
const web = (k: string) => ({ value: 'x', platform: 'web' as const, description: '' });

describe('checkParity', () => {
  it("passes when 'both' entry used on both sides", () => {
    const r = checkParity(
      { entries: { 'toolbar.new': both('toolbar.new') } },
      new Set([keyToElmIdent('toolbar.new')]),
      new Set([keyToElmIdent('toolbar.new')]),
    );
    expect(r.failures).toEqual([]);
  });

  it("fails when 'both' entry missing from desktop", () => {
    const r = checkParity(
      { entries: { 'toolbar.new': both('toolbar.new') } },
      new Set(),
      new Set([keyToElmIdent('toolbar.new')]),
    );
    expect(r.failures.some((f) => f.kind === 'missing_on_desktop')).toBe(true);
  });

  it("fails when 'both' entry missing from web", () => {
    const r = checkParity(
      { entries: { 'toolbar.new': both('toolbar.new') } },
      new Set([keyToElmIdent('toolbar.new')]),
      new Set(),
    );
    expect(r.failures.some((f) => f.kind === 'missing_on_web')).toBe(true);
  });

  it("fails when 'desktop' entry leaks into web", () => {
    const r = checkParity(
      { entries: { 'menu.exit': desk('menu.exit') } },
      new Set([keyToElmIdent('menu.exit')]),
      new Set([keyToElmIdent('menu.exit')]),
    );
    expect(r.failures.some((f) => f.kind === 'leaked_to_web')).toBe(true);
  });

  it("fails when 'web' entry leaks into desktop", () => {
    const r = checkParity(
      { entries: { 'web.specific': web('web.specific') } },
      new Set([keyToElmIdent('web.specific')]),
      new Set([keyToElmIdent('web.specific')]),
    );
    expect(r.failures.some((f) => f.kind === 'leaked_to_desktop')).toBe(true);
  });

  it('fails when source references unknown key', () => {
    const r = checkParity({ entries: {} }, new Set(['unknownKey']), new Set());
    expect(r.failures.some((f) => f.kind === 'unknown_reference')).toBe(true);
  });
});
