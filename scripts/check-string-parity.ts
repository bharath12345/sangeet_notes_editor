import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadCatalog, keyToElmIdent, Catalog } from './lib/catalog.ts';
import { scanScalaTree, scanElmTree } from './lib/source-scanner.ts';

export type Failure =
  | { kind: 'missing_on_desktop'; key: string }
  | { kind: 'missing_on_web'; key: string }
  | { kind: 'leaked_to_desktop'; key: string }
  | { kind: 'leaked_to_web'; key: string }
  | { kind: 'unknown_reference'; ident: string; platform: 'desktop' | 'web' };

export type ParityResult = {
  failures: Failure[];
  formatReport(): string;
};

export function checkParity(
  catalog: Catalog,
  desktopRefs: Set<string>,
  webRefs: Set<string>,
): ParityResult {
  const failures: Failure[] = [];
  const knownIdents = new Set<string>();

  for (const [key, entry] of Object.entries(catalog.entries)) {
    const ident = keyToElmIdent(key);
    knownIdents.add(ident);

    const onDesktop = desktopRefs.has(ident);
    const onWeb = webRefs.has(ident);

    switch (entry.platform) {
      case 'both':
        if (!onDesktop) failures.push({ kind: 'missing_on_desktop', key });
        if (!onWeb) failures.push({ kind: 'missing_on_web', key });
        break;
      case 'desktop':
        if (!onDesktop) failures.push({ kind: 'missing_on_desktop', key });
        if (onWeb) failures.push({ kind: 'leaked_to_web', key });
        break;
      case 'web':
        if (!onWeb) failures.push({ kind: 'missing_on_web', key });
        if (onDesktop) failures.push({ kind: 'leaked_to_desktop', key });
        break;
    }
  }

  for (const ident of desktopRefs) {
    if (!knownIdents.has(ident))
      failures.push({ kind: 'unknown_reference', ident, platform: 'desktop' });
  }
  for (const ident of webRefs) {
    if (!knownIdents.has(ident))
      failures.push({ kind: 'unknown_reference', ident, platform: 'web' });
  }

  return {
    failures,
    formatReport() {
      if (!failures.length) return 'Strings parity OK.';
      return [
        `❌ ${failures.length} strings parity failure(s):`,
        ...failures.map((f) => {
          switch (f.kind) {
            case 'missing_on_desktop':
              return `  - MISSING on desktop:  ${f.key}`;
            case 'missing_on_web':
              return `  - MISSING on web:      ${f.key}`;
            case 'leaked_to_desktop':
              return `  - LEAKED to desktop:   ${f.key} (declared platform:web)`;
            case 'leaked_to_web':
              return `  - LEAKED to web:       ${f.key} (declared platform:desktop)`;
            case 'unknown_reference':
              return `  - UNKNOWN reference:   UiStrings.${f.ident} on ${f.platform} (not in catalog)`;
          }
        }),
      ].join('\n');
    },
  };
}

// CLI entry point
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  // Script runs from scripts/ dir, so repo root is one level up
  const root = resolve(process.cwd(), '..');
  const catalog = loadCatalog(resolve(root, 'sangeet-core/src/main/resources/ui-strings.json'));
  const desktopRefs = scanScalaTree([
    resolve(root, 'sangeet-desktop/src/main'),
    resolve(root, 'sangeet-server/src/main'),
    resolve(root, 'sangeet-core/src/main'),
  ]);
  const webRefs = scanElmTree([
    resolve(root, 'sangeet-web/src'),
    resolve(root, 'sangeet-web/tests'),
  ]);
  const result = checkParity(catalog, desktopRefs, webRefs);
  console.log(result.formatReport());
  if (result.failures.length > 0) process.exit(1);
}
