import { describe, it, expect } from 'vitest';
import { extractScalaRefs, extractElmRefs } from '../lib/source-scanner.ts';

describe('source-scanner', () => {
  it('extracts UiStrings.foo refs from Scala', () => {
    const src = `
      val b = Button(UiStrings.toolbarFileNew) { ... }
      label.text = UiStrings.dialogAboutTitle
      val msg = UiStrings.toolbarBeatCount(3, 16)
    `;
    expect(extractScalaRefs(src)).toEqual(
      new Set(['toolbarFileNew', 'dialogAboutTitle', 'toolbarBeatCount']),
    );
  });

  it('extracts UiStrings.foo refs from Elm', () => {
    const src = `
      button [ onClick (...) ] [ text UiStrings.toolbarFileNew ]
      span [] [ text (UiStrings.toolbarBeatCount 3 16) ]
    `;
    expect(extractElmRefs(src)).toEqual(new Set(['toolbarFileNew', 'toolbarBeatCount']));
  });

  it('ignores UiStrings inside string literals (false positive guard)', () => {
    expect(extractScalaRefs('val s = "UiStrings.notAnIdent"')).toEqual(new Set());
  });
});
