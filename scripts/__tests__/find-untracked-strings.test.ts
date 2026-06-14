import { describe, it, expect } from 'vitest';

// Heuristic regex to match "likely UI text":
// - 3+ characters, contains a space OR is TitleCase OR ends with punctuation
// - Excludes CSS class names (contains "-"), file paths ("/"), HTTP URLs, etc.
const LIKELY_UI =
  /^(?=.{3,})(?:[A-Z][a-z]+(?:\s+\S+){1,}|[A-Z][a-zA-Z]{2,}(?:\s+\S+)+|[A-Za-z][^"]{3,}[.!?])$/;

function isLikelyUi(literal: string, context: string): boolean {
  return (
    LIKELY_UI.test(literal) &&
    !context.includes('class=') &&
    !context.includes('className') &&
    !context.includes('href=') &&
    !context.includes('logger.') &&
    !context.includes('Logger.') &&
    !context.includes('log.') &&
    !context.includes('println') &&
    !context.includes('Debug.log') &&
    !context.includes('Debug.todo')
  );
}

describe('find-untracked-strings heuristic', () => {
  describe('LIKELY_UI regex', () => {
    it('accepts sentence with multiple words', () => {
      expect(LIKELY_UI.test('New Composition')).toBe(true);
      expect(LIKELY_UI.test('Save as Draft')).toBe(true);
      expect(LIKELY_UI.test('Click here to continue')).toBe(true);
    });

    it('accepts sentence ending with punctuation', () => {
      expect(LIKELY_UI.test('Saved.')).toBe(true);
      expect(LIKELY_UI.test('Are you sure?')).toBe(true);
      expect(LIKELY_UI.test('Done!')).toBe(true);
    });

    it('accepts TitleCase multi-word', () => {
      expect(LIKELY_UI.test('SaveComposition')).toBe(false); // no spaces, not ending with punctuation
      expect(LIKELY_UI.test('Save Composition Now')).toBe(true);
    });

    it('rejects short strings (< 3 chars)', () => {
      expect(LIKELY_UI.test('OK')).toBe(false);
      expect(LIKELY_UI.test('No')).toBe(false);
      expect(LIKELY_UI.test('s')).toBe(false);
    });

    it('rejects CSS class names (contain hyphens)', () => {
      // These should be caught by context check, not regex
      expect(LIKELY_UI.test('toolbar-button')).toBe(false);
      expect(LIKELY_UI.test('modal-dialog')).toBe(false);
    });

    it('rejects file paths', () => {
      expect(LIKELY_UI.test('/path/to/file')).toBe(false);
      expect(LIKELY_UI.test('../relative/path')).toBe(false);
    });

    it('rejects single words without spaces or punctuation', () => {
      expect(LIKELY_UI.test('toolbar')).toBe(false);
      expect(LIKELY_UI.test('button')).toBe(false);
      expect(LIKELY_UI.test('reset')).toBe(false);
    });

    it('accepts error messages', () => {
      expect(LIKELY_UI.test('Failed to save composition.')).toBe(true);
      expect(LIKELY_UI.test('Invalid taal selected')).toBe(true);
    });
  });

  describe('context filtering', () => {
    it('rejects CSS class context', () => {
      const literal = 'New Composition';
      expect(isLikelyUi(literal, 'class="new-composition"')).toBe(false);
      expect(isLikelyUi(literal, 'className "toolbar-button"')).toBe(false);
    });

    it('rejects href context', () => {
      const literal = 'Click here';
      expect(isLikelyUi(literal, 'href="http://example.com"')).toBe(false);
    });

    it('rejects logger context (Scala)', () => {
      const literal = 'Saving composition';
      expect(isLikelyUi(literal, 'logger.info("Saving composition")')).toBe(false);
      expect(isLikelyUi(literal, 'Logger.debug("Saving composition")')).toBe(false);
    });

    it('rejects println context', () => {
      const literal = 'Debug output';
      expect(isLikelyUi(literal, 'println("Debug output")')).toBe(false);
    });

    it('rejects Debug.log context (Elm)', () => {
      const literal = 'Current state';
      expect(isLikelyUi(literal, 'Debug.log "Current state"')).toBe(false);
      expect(isLikelyUi(literal, 'Debug.todo "Current state"')).toBe(false);
    });

    it('accepts legitimate UI literal context', () => {
      const literal = 'New Composition';
      expect(isLikelyUi(literal, 'text "New Composition"')).toBe(true);
      expect(isLikelyUi(literal, 'Button { text = "New Composition" }')).toBe(true);
      expect(isLikelyUi(literal, 'setText("New Composition")')).toBe(true);
    });
  });

  describe('edge cases', () => {
    it('rejects JSON keys', () => {
      // These typically appear as: "key": "value"
      // The regex might accept them, but they're single words
      expect(LIKELY_UI.test('title')).toBe(false);
      expect(LIKELY_UI.test('raag')).toBe(false);
      expect(LIKELY_UI.test('taal')).toBe(false);
    });

    it('accepts multi-word labels', () => {
      expect(LIKELY_UI.test('Composition Title:')).toBe(true);
      expect(LIKELY_UI.test('Select Raag')).toBe(true);
      expect(LIKELY_UI.test('Starting Beat')).toBe(true);
    });

    it('rejects HTTP URLs', () => {
      expect(LIKELY_UI.test('http://example.com')).toBe(false);
      expect(LIKELY_UI.test('https://github.com/user/repo')).toBe(false);
    });

    it('rejects log tags', () => {
      expect(LIKELY_UI.test('[debug]')).toBe(false);
      expect(LIKELY_UI.test('[info]')).toBe(false);
    });
  });
});
