import { describe, it, expect } from 'vitest';
import { emitElm } from '../gen-elm-strings.ts';

describe('emitElm', () => {
  it('emits header for empty catalog', () => {
    const out = emitElm({ entries: {} });
    expect(out).toMatch(/^module UiStrings exposing \(\.\.\)/);
    expect(out).toContain('GENERATED FILE');
  });

  it("emits constant for 'value' entry", () => {
    const out = emitElm({
      entries: { 'toolbar.file.new': { value: 'New', platform: 'both', description: '' } },
    });
    expect(out).toMatch(/toolbarFileNew : String\ntoolbarFileNew =\n    "New"/);
  });

  it('emits typed function for parameterized entry', () => {
    const out = emitElm({
      entries: {
        'toolbar.beatCount': {
          template: 'Beats: {current} / {total}',
          params: [
            { name: 'current', type: 'int' },
            { name: 'total', type: 'int' },
          ],
          platform: 'both',
          description: '',
        },
      },
    });
    expect(out).toMatch(/toolbarBeatCount : Int -> Int -> String/);
    expect(out).toMatch(/toolbarBeatCount current total =/);
    expect(out).toMatch(
      /"Beats: " \+\+ String\.fromInt current \+\+ " \/ " \+\+ String\.fromInt total/,
    );
  });

  it('escapes double quotes and backslashes', () => {
    const out = emitElm({
      entries: { k: { value: '"quoted" \\ back', platform: 'both', description: '' } },
    });
    expect(out).toContain('"\\"quoted\\" \\\\ back"');
  });

  it('sorts entries deterministically', () => {
    const out = emitElm({
      entries: {
        z: { value: 'Z', platform: 'both', description: '' },
        a: { value: 'A', platform: 'both', description: '' },
      },
    });
    expect(out.indexOf('a :')).toBeLessThan(out.indexOf('z :'));
  });

  it('rejects literal newlines in values', () => {
    expect(() =>
      emitElm({
        entries: { k: { value: 'line1\nline2', platform: 'both', description: '' } },
      }),
    ).toThrow(/control characters/);
  });

  it('handles template with only placeholders (no surrounding text)', () => {
    const out = emitElm({
      entries: {
        justCount: {
          template: '{count}',
          params: [{ name: 'count', type: 'int' }],
          platform: 'both',
          description: '',
        },
      },
    });
    expect(out).toMatch(/justCount : Int -> String/);
    expect(out).toMatch(/justCount count =\n    String\.fromInt count/);
  });
});
