import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { globSync } from 'glob';

// Heuristics for "looks like user-facing English text":
// - 3+ characters, contains a space OR is TitleCase OR ends with punctuation
// - Excludes CSS class names (contains "-"), file paths (contains "/"), HTTP
//   URLs, log tags ("[debug]"), JSON keys (preceded by `:` or `"`), etc.
const LIKELY_UI =
  /^(?=.{3,})(?:[A-Z][a-z]+(?:\s+\S+){1,}|[A-Z][a-zA-Z]{2,}(?:\s+\S+)+|[A-Za-z][^"]{3,}[.!?])$/;

const SCALA_STRING = /"((?:[^"\\]|\\.)*)"/g;
const ELM_STRING = /"((?:[^"\\]|\\.)*)"/g;

type Hit = { file: string; line: number; literal: string; context: string };

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
    !context.includes('Debug.todo') &&
    !literal.includes('-') && // CSS class names
    !literal.includes('/') // file paths
  );
}

function scan(globPattern: string, regex: RegExp): Hit[] {
  const hits: Hit[] = [];
  for (const file of globSync(globPattern, { nodir: true })) {
    const lines = readFileSync(file, 'utf-8').split('\n');
    lines.forEach((ln, i) => {
      let m: RegExpExecArray | null;
      const localRegex = new RegExp(regex.source, 'g');
      while ((m = localRegex.exec(ln)) !== null) {
        const literal = m[1];
        if (isLikelyUi(literal, ln)) {
          hits.push({ file, line: i + 1, literal, context: ln.trim() });
        }
      }
    });
  }
  return hits;
}

const root = process.cwd().replace(/\/scripts$/, ''); // Handle running from scripts/ or root

// Desktop: editor/ and dialog/ packages + MainApp
const desktopGlobs = [
  'sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/**/*.scala',
  'sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/**/*.scala',
  'sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/MainApp.scala',
];

// Web: View/ and State/ packages (Update.elm, AppAction.elm)
const webGlobs = [
  'sangeet-web/src/View/**/*.elm',
  'sangeet-web/src/State/Update.elm',
  'sangeet-web/src/State/AppAction.elm',
];

let desktopHits: Hit[] = [];
for (const glob of desktopGlobs) {
  desktopHits = desktopHits.concat(scan(resolve(root, glob), SCALA_STRING));
}

let webHits: Hit[] = [];
for (const glob of webGlobs) {
  webHits = webHits.concat(scan(resolve(root, glob), ELM_STRING));
}

// Sort by file, then line number for consistent output
desktopHits.sort((a, b) => a.file.localeCompare(b.file) || a.line - b.line);
webHits.sort((a, b) => a.file.localeCompare(b.file) || a.line - b.line);

console.log('# Untracked string candidates');
console.log('');
console.log(
  `Found ${desktopHits.length} desktop + ${webHits.length} web candidates that look like user-facing text.`,
);
console.log('Triage each: add to catalog if legitimate UI, ignore if CSS/log/internal.');
console.log('');

if (desktopHits.length > 0) {
  console.log('## Desktop');
  console.log('');
  desktopHits.forEach((h) => {
    const relPath = h.file.replace(root + '/', '');
    console.log(`- ${relPath}:${h.line} — "${h.literal}"`);
    console.log(`  Context: ${h.context.substring(0, 120)}`);
    console.log('');
  });
}

if (webHits.length > 0) {
  console.log('## Web');
  console.log('');
  webHits.forEach((h) => {
    const relPath = h.file.replace(root + '/', '');
    console.log(`- ${relPath}:${h.line} — "${h.literal}"`);
    console.log(`  Context: ${h.context.substring(0, 120)}`);
    console.log('');
  });
}

if (desktopHits.length === 0 && webHits.length === 0) {
  console.log('No untracked candidates found. All UI strings appear to be in the catalog.');
}
