import { writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadCatalog, keyToElmIdent, typeToElm, Catalog, Entry, Param } from './lib/catalog.ts';

export function emitElm(catalog: Catalog): string {
  const header = `module UiStrings exposing (..)

-- GENERATED FILE — DO NOT EDIT MANUALLY.
-- Source:    sangeet-core/src/main/resources/ui-strings.json
-- Regenerate: cd scripts && npm run gen   (or: make gen-strings)
--
-- To add or change a string: edit ui-strings.json, then run \`make gen-strings\`,
-- then use \`UiStrings.<key>\` on both desktop and web. See
-- docs/developer/architecture/ui-strings-catalog.md for the full guide.

`;

  const sorted = Object.entries(catalog.entries).sort(([a], [b]) => a.localeCompare(b));
  const constants = sorted.filter(([, e]) => !e.template);
  const functions = sorted.filter(([, e]) => e.template);

  const constLines = constants.map(([key, e]) => emitConstant(key, e));
  const funcLines = functions.map(([key, e]) => emitFunction(key, e));

  return (
    header +
    constLines.join('\n\n') +
    (constLines.length && funcLines.length ? '\n\n' : '') +
    funcLines.join('\n\n') +
    (sorted.length ? '\n' : '')
  );
}

function emitConstant(key: string, e: Entry): string {
  const ident = keyToElmIdent(key);
  const escaped = escapeElm(e.value!);
  return `${ident} : String\n${ident} =\n    "${escaped}"`;
}

function emitFunction(key: string, e: Entry): string {
  const ident = keyToElmIdent(key);
  const params = e.params!;
  const argTypes = [...params.map((p) => typeToElm(p.type)), 'String'].join(' -> ');
  const argNames = params.map((p) => p.name).join(' ');
  const body = emitElmTemplateBody(e.template!, params);
  return `${ident} : ${argTypes}\n${ident} ${argNames} =\n    ${body}`;
}

function escapeElm(s: string): string {
  if (/[\n\r\t]/.test(s)) {
    throw new Error(
      `String contains literal control characters (newline/tab/CR). ` +
      `UI strings should be single-line. Value: ${JSON.stringify(s)}`
    );
  }
  return s.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
}

function emitElmTemplateBody(template: string, params: Param[]): string {
  // Split template by placeholders, intersperse with String concatenation.
  // "Beats: {current} / {total}" -> "Beats: " ++ String.fromInt current ++ " / " ++ String.fromInt total
  let remaining = template;
  const pieces: string[] = [];
  while (remaining.length) {
    const m = remaining.match(/\{([a-zA-Z_][a-zA-Z0-9_]*)\}/);
    if (!m) {
      if (remaining) pieces.push(`"${escapeElm(remaining)}"`);
      break;
    }
    const before = remaining.slice(0, m.index);
    if (before) pieces.push(`"${escapeElm(before)}"`);
    const p = params.find((x) => x.name === m[1]);
    if (!p) throw new Error(`Template references unknown param {${m[1]}}`);
    pieces.push(p.type === 'int' ? `String.fromInt ${p.name}` : p.name);
    remaining = remaining.slice(m.index! + m[0].length);
  }
  return pieces.join(' ++ ');
}

// CLI entry point
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const catalogPath = resolve(process.cwd(), '../sangeet-core/src/main/resources/ui-strings.json');
  const outputPath = resolve(process.cwd(), '../sangeet-web/src/UiStrings.elm');
  const catalog = loadCatalog(catalogPath);
  writeFileSync(outputPath, emitElm(catalog), 'utf-8');
  console.log(`Wrote ${outputPath} (${Object.keys(catalog.entries).length} entries)`);
}
