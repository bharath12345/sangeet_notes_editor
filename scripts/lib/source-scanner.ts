import { readFileSync } from 'node:fs';
import { globSync } from 'glob';

const REF_PATTERN = /\bUiStrings\.([a-zA-Z][a-zA-Z0-9]*)/g;

export function extractScalaRefs(source: string): Set<string> {
  return extract(stripStringLiterals(source));
}

export function extractElmRefs(source: string): Set<string> {
  return extract(stripStringLiterals(source));
}

function extract(source: string): Set<string> {
  const refs = new Set<string>();
  let m: RegExpExecArray | null;
  while ((m = REF_PATTERN.exec(source)) !== null) refs.add(m[1]);
  return refs;
}

// Lightweight string-literal stripper. Not a full parser, but handles the
// common cases of "..." and triple-quoted strings.
function stripStringLiterals(source: string): string {
  return source.replace(/"""[\s\S]*?"""/g, '""').replace(/"(?:[^"\\]|\\.)*"/g, '""');
}

export function scanScalaTree(roots: string[]): Set<string> {
  const all = new Set<string>();
  for (const root of roots) {
    for (const file of globSync(`${root}/**/*.scala`)) {
      const src = readFileSync(file, 'utf-8');
      for (const r of extractScalaRefs(src)) all.add(r);
    }
  }
  return all;
}

export function scanElmTree(roots: string[]): Set<string> {
  const all = new Set<string>();
  for (const root of roots) {
    for (const file of globSync(`${root}/**/*.elm`)) {
      const src = readFileSync(file, 'utf-8');
      for (const r of extractElmRefs(src)) all.add(r);
    }
  }
  return all;
}
