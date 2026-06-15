import { writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadCatalog, Entry, Disposition } from './lib/catalog.ts';

/** Extract area.component prefix (first 2 segments of key) */
function extractComponent(key: string): string {
  const parts = key.split('.');
  return parts.length >= 2 ? `${parts[0]}.${parts[1]}` : parts[0];
}

/** Extract concept (remaining segments after area.component) */
function extractConcept(key: string): string {
  const parts = key.split('.');
  return parts.length >= 3 ? parts.slice(2).join('.') : parts[parts.length - 1];
}

/** Escape pipes and other Markdown special chars */
function escapeMarkdown(text: string): string {
  return text.replace(/\|/g, '\\|').replace(/\*/g, '\\*').replace(/`/g, '\\`');
}

/** Format entry value/template, truncating if over 60 chars */
function formatValue(entry: Entry): string {
  const text = entry.value ?? entry.template ?? '';
  const escaped = escapeMarkdown(text);
  const suffix =
    entry.template && entry.params && entry.params.length > 0
      ? ` [${entry.params.length} param${entry.params.length > 1 ? 's' : ''}]`
      : '';
  const full = `${escaped}${suffix}`;
  return full.length > 60 ? full.slice(0, 57) + '…' : full;
}

type ConceptRow = {
  concept: string;
  desktop: string | null;
  web: string | null;
  /** Disposition pulled from the entry that produced this row (if any).
   *  When desktop and web both exist, we pick the one with an explicit disposition
   *  (desktop wins as tiebreaker since desktop is primary). */
  disposition?: Disposition;
  /** True when the disposition has a dispositionNote (treated as resolved). */
  done: boolean;
};

type ComponentTable = {
  component: string;
  rows: ConceptRow[];
  hasDesktop: boolean;
  hasWeb: boolean;
};

type Suggestion = 'NORMALIZE' | 'PORT→desk' | 'PORT→web' | 'ACCEPT';

type RowWithSuggestion = ConceptRow & {
  /** What we display in the Suggest column. */
  suggest: string;
  /** PENDING | DONE | (blank for symmetric/shared). */
  status: string;
};

/** Build component tables by pairing entries */
function buildComponentTables(catalog: Record<string, Entry>): ComponentTable[] {
  const components = new Map<string, Map<string, { desktop?: Entry; web?: Entry }>>();

  // Group all entries by component, then by concept
  for (const [key, entry] of Object.entries(catalog)) {
    const component = extractComponent(key);
    const concept = extractConcept(key);

    if (!components.has(component)) {
      components.set(component, new Map());
    }
    const conceptMap = components.get(component)!;
    if (!conceptMap.has(concept)) {
      conceptMap.set(concept, {});
    }
    const pair = conceptMap.get(concept)!;

    // Entries with platform:'both' appear in both columns
    if (entry.platform === 'both') {
      pair.desktop = entry;
      pair.web = entry;
    } else if (entry.platform === 'desktop') {
      pair.desktop = entry;
    } else if (entry.platform === 'web') {
      pair.web = entry;
    }
  }

  // Convert to ComponentTable array
  const tables: ComponentTable[] = [];
  for (const [component, conceptMap] of components.entries()) {
    const rows: ConceptRow[] = [];
    let hasDesktop = false;
    let hasWeb = false;

    for (const [concept, pair] of conceptMap.entries()) {
      const desktop = pair.desktop ? formatValue(pair.desktop) : null;
      const web = pair.web ? formatValue(pair.web) : null;

      if (desktop) hasDesktop = true;
      if (web) hasWeb = true;

      // Prefer desktop entry's disposition (desktop is primary). Fall back to
      // web entry's if desktop has none. For platform=both rows the same
      // entry occupies both pair slots, so this works uniformly.
      const dispEntry =
        (pair.desktop && pair.desktop.disposition && pair.desktop) ||
        (pair.web && pair.web.disposition && pair.web) ||
        undefined;
      const disposition = dispEntry?.disposition;
      const done = Boolean(dispEntry?.dispositionNote);

      rows.push({ concept, desktop, web, disposition, done });
    }

    // Sort concepts alphabetically
    rows.sort((a, b) => a.concept.localeCompare(b.concept));

    tables.push({ component, rows, hasDesktop, hasWeb });
  }

  // Sort components alphabetically
  tables.sort((a, b) => a.component.localeCompare(b.component));

  return tables;
}

/** Auto-suggest disposition for a row (heuristic, used when no explicit
 *  disposition is set on the entry). */
function suggestDisposition(
  row: ConceptRow,
  componentHasDesktop: boolean,
  componentHasWeb: boolean,
): Suggestion {
  const { desktop, web } = row;

  // Both have values
  if (desktop && web) {
    if (desktop === web) return 'ACCEPT'; // placeholder, won't be shown
    return 'NORMALIZE';
  }

  // Only desktop has value
  if (desktop && !web) {
    return 'PORT→web';
  }

  // Only web has value
  if (!desktop && web) {
    if (componentHasDesktop) return 'PORT→desk';
    return 'ACCEPT';
  }

  return 'ACCEPT';
}

/** Map an explicit disposition value to its display label. */
function dispositionLabel(d: Disposition): Suggestion {
  switch (d) {
    case 'port-to-web':
      return 'PORT→web';
    case 'port-to-desk':
      return 'PORT→desk';
    case 'accept':
      return 'ACCEPT';
    case 'normalize':
      return 'NORMALIZE';
  }
}

/** Resolve the Suggest column text and Status for a row, combining the
 *  heuristic suggestion with any explicit disposition stored on the entry. */
function resolveSuggest(
  row: ConceptRow,
  heuristic: Suggestion,
): { suggest: string; status: string } {
  if (!row.disposition) {
    return { suggest: heuristic, status: 'PENDING' };
  }
  const explicit = dispositionLabel(row.disposition);
  const status = row.done ? 'DONE' : 'PENDING';
  if (explicit === heuristic) {
    return { suggest: explicit, status };
  }
  return { suggest: `${explicit} (override)`, status };
}

/** Generate Markdown section for a component table */
function generateComponentSection(table: ComponentTable): string | null {
  const { component, rows, hasDesktop, hasWeb } = table;

  // Filter out rows where desktop === web (symmetric, no decision needed)
  const asymmetricRows: RowWithSuggestion[] = [];
  for (const row of rows) {
    if (row.desktop === row.web && row.desktop !== null) {
      continue;
    }
    const heuristic = suggestDisposition(row, hasDesktop, hasWeb);
    const { suggest, status } = resolveSuggest(row, heuristic);
    asymmetricRows.push({ ...row, suggest, status });
  }

  if (asymmetricRows.length === 0) return null;

  const lines: string[] = [];

  lines.push(
    `### ${component}  (${asymmetricRows.length} ${asymmetricRows.length === 1 ? 'entry' : 'entries'})`,
  );
  lines.push('');

  const allOneSided = asymmetricRows.every((r) => !r.desktop || !r.web);
  if (allOneSided) {
    if (hasDesktop && !hasWeb) {
      lines.push(
        `*(All entries are desktop-only — default suggest is PORT→web. Override to ACCEPT for genuinely desktop-only architectural concepts.)*`,
      );
      lines.push('');
    } else if (!hasDesktop && hasWeb) {
      lines.push(`*(All entries are web-only architectural — consider bulk ACCEPT.)*`);
      lines.push('');
    }
  }

  lines.push('| Concept | Desktop | Web | Suggest | Status |');
  lines.push('| ------- | ------- | --- | ------- | ------ |');

  for (const row of asymmetricRows) {
    const concept = escapeMarkdown(row.concept);
    const desktop = row.desktop ?? '(none)';
    const web = row.web ?? '(none)';
    lines.push(`| ${concept} | ${desktop} | ${web} | ${row.suggest} | ${row.status} |`);
  }

  lines.push('');
  return lines.join('\n');
}

type Stats = {
  shared: number;
  normalize: number;
  portDesk: number;
  portWeb: number;
  accept: number;
  pending: number;
  done: number;
  total: number;
};

/** Calculate summary stats */
function calculateStats(tables: ComponentTable[]): Stats {
  let shared = 0;
  let normalize = 0;
  let portDesk = 0;
  let portWeb = 0;
  let accept = 0;
  let pending = 0;
  let done = 0;

  for (const table of tables) {
    const { rows, hasDesktop, hasWeb } = table;
    for (const row of rows) {
      if (row.desktop === row.web && row.desktop !== null) {
        shared++;
        continue;
      }
      const heuristic = suggestDisposition(row, hasDesktop, hasWeb);
      const { suggest, status } = resolveSuggest(row, heuristic);
      const bucket = suggest.startsWith('PORT→web')
        ? 'portWeb'
        : suggest.startsWith('PORT→desk')
          ? 'portDesk'
          : suggest.startsWith('NORMALIZE')
            ? 'normalize'
            : 'accept';
      switch (bucket) {
        case 'normalize':
          normalize++;
          break;
        case 'portDesk':
          portDesk++;
          break;
        case 'portWeb':
          portWeb++;
          break;
        case 'accept':
          accept++;
          break;
      }
      if (status === 'DONE') done++;
      else if (status === 'PENDING') pending++;
    }
  }

  return {
    shared,
    normalize,
    portDesk,
    portWeb,
    accept,
    pending,
    done,
    total: shared + normalize + portDesk + portWeb + accept,
  };
}

// CLI entry point
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  // Script runs from scripts/ dir, so repo root is one level up
  const root = resolve(process.cwd(), '..');
  const catalog = loadCatalog(resolve(root, 'sangeet-core/src/main/resources/ui-strings.json'));

  const now = new Date().toISOString().slice(0, 10);
  const tables = buildComponentTables(catalog.entries);
  const stats = calculateStats(tables);

  // Generate component sections
  const componentSections: string[] = [];
  for (const table of tables) {
    const section = generateComponentSection(table);
    if (section) componentSections.push(section);
  }

  const out = `# UI Strings Parity Report — Side-by-Side

> Generated: ${now}. Regenerate with \`make strings-report\`.

## Summary

| Bucket                              | Count |
| ----------------------------------- | ----- |
| Shared (identical, hidden below)    | ${stats.shared} |
| NORMALIZE candidates                | ${stats.normalize} |
| PORT→desk candidates                | ${stats.portDesk} |
| PORT→web candidates                 | ${stats.portWeb} |
| ACCEPT candidates                   | ${stats.accept} |
| **Total asymmetric concepts**       | **${stats.normalize + stats.portDesk + stats.portWeb + stats.accept}** |
| Status: DONE                        | ${stats.done} |
| Status: PENDING                     | ${stats.pending} |

## How to use this report

Each row in the per-component tables shows:

- **Suggest** — the authoritative disposition for this entry.
  - When set explicitly on the catalog entry (\`disposition\` field), the explicit value is used.
  - When the heuristic and the explicit disposition disagree, the cell reads \`<explicit> (override)\`.
  - When no explicit disposition is set, the cell shows the heuristic guess.
- **Status** — \`DONE\` when the entry has a \`dispositionNote\` (i.e., the port has landed
  or the decision has been recorded); \`PENDING\` otherwise.

Disposition vocabulary:

- **NORMALIZE** — Both platforms have the concept but with different wording; pick one to adopt.
- **PORT→desk** — Web has it, desktop doesn't, but the component exists on desktop; should be added.
- **PORT→web** — Desktop has it, web doesn't, but the component exists on web; should be added.
- **ACCEPT** — Platform-specific architectural difference; keep as-is.

Rows where Desktop and Web have identical values are hidden from this report — they're already symmetric.

## Components

${componentSections.join('\n')}
`;

  writeFileSync(resolve(root, 'docs/reports/strings-parity-report.md'), out, 'utf-8');
  console.log('✓ Wrote docs/reports/strings-parity-report.md');
}
