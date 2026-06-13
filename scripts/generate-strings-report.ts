import { writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadCatalog, Entry } from './lib/catalog.ts';

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
  const suffix = entry.template && entry.params && entry.params.length > 0
    ? ` [${entry.params.length} param${entry.params.length > 1 ? 's' : ''}]`
    : '';
  const full = `${escaped}${suffix}`;
  return full.length > 60 ? full.slice(0, 57) + '…' : full;
}

type ConceptRow = {
  concept: string;
  desktop: string | null;
  web: string | null;
};

type ComponentTable = {
  component: string;
  rows: ConceptRow[];
  hasDesktop: boolean;
  hasWeb: boolean;
};

type Suggestion = 'NORMALIZE' | 'PORT→desk' | 'PORT→web' | 'ACCEPT';

type RowWithSuggestion = ConceptRow & { suggest: Suggestion };

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

      rows.push({ concept, desktop, web });
    }

    // Sort concepts alphabetically
    rows.sort((a, b) => a.concept.localeCompare(b.concept));

    tables.push({ component, rows, hasDesktop, hasWeb });
  }

  // Sort components alphabetically
  tables.sort((a, b) => a.component.localeCompare(b.component));

  return tables;
}

/** Auto-suggest disposition for a row */
function suggestDisposition(
  row: ConceptRow,
  componentHasDesktop: boolean,
  componentHasWeb: boolean
): Suggestion {
  const { desktop, web } = row;

  // Both have values
  if (desktop && web) {
    // If identical, we'll skip this row (caller handles)
    if (desktop === web) return 'ACCEPT'; // placeholder, won't be shown
    // Different wording → normalize
    return 'NORMALIZE';
  }

  // Only desktop has value
  if (desktop && !web) {
    // If component has other web entries → port to web
    if (componentHasWeb) return 'PORT→web';
    // Component is desktop-only architectural → accept
    return 'ACCEPT';
  }

  // Only web has value
  if (!desktop && web) {
    // If component has other desktop entries → port to desktop
    if (componentHasDesktop) return 'PORT→desk';
    // Component is web-only architectural → accept
    return 'ACCEPT';
  }

  // Neither has value (shouldn't happen)
  return 'ACCEPT';
}

/** Generate Markdown section for a component table */
function generateComponentSection(table: ComponentTable): string | null {
  const { component, rows, hasDesktop, hasWeb } = table;

  // Filter out rows where desktop === web (symmetric, no decision needed)
  const asymmetricRows: RowWithSuggestion[] = [];
  for (const row of rows) {
    if (row.desktop === row.web && row.desktop !== null) {
      // Skip symmetric rows
      continue;
    }
    const suggest = suggestDisposition(row, hasDesktop, hasWeb);
    asymmetricRows.push({ ...row, suggest });
  }

  // If no asymmetric rows, skip this component entirely
  if (asymmetricRows.length === 0) return null;

  const lines: string[] = [];

  // Component header with count
  lines.push(`### ${component}  (${asymmetricRows.length} ${asymmetricRows.length === 1 ? 'entry' : 'entries'})`);
  lines.push('');

  // Add note if all entries are one-sided
  const allOneSided = asymmetricRows.every(r => !r.desktop || !r.web);
  if (allOneSided) {
    const side = hasDesktop && !hasWeb ? 'desktop-only' : !hasDesktop && hasWeb ? 'web-only' : '';
    if (side) {
      lines.push(`*(All entries in this component are ${side} architectural — consider bulk ACCEPT.)*`);
      lines.push('');
    }
  }

  // Table header
  lines.push('| Concept | Desktop | Web | Suggest |');
  lines.push('| ------- | ------- | --- | ------- |');

  // Table rows
  for (const row of asymmetricRows) {
    const concept = escapeMarkdown(row.concept);
    const desktop = row.desktop ?? '(none)';
    const web = row.web ?? '(none)';
    lines.push(`| ${concept} | ${desktop} | ${web} | ${row.suggest} |`);
  }

  lines.push('');
  return lines.join('\n');
}

/** Calculate summary stats */
function calculateStats(tables: ComponentTable[]): {
  shared: number;
  normalize: number;
  portDesk: number;
  portWeb: number;
  accept: number;
  total: number;
} {
  let shared = 0;
  let normalize = 0;
  let portDesk = 0;
  let portWeb = 0;
  let accept = 0;

  for (const table of tables) {
    const { rows, hasDesktop, hasWeb } = table;
    for (const row of rows) {
      if (row.desktop === row.web && row.desktop !== null) {
        shared++;
        continue;
      }
      const suggest = suggestDisposition(row, hasDesktop, hasWeb);
      switch (suggest) {
        case 'NORMALIZE':
          normalize++;
          break;
        case 'PORT→desk':
          portDesk++;
          break;
        case 'PORT→web':
          portWeb++;
          break;
        case 'ACCEPT':
          accept++;
          break;
      }
    }
  }

  return {
    shared,
    normalize,
    portDesk,
    portWeb,
    accept,
    total: shared + normalize + portDesk + portWeb + accept
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

## How to use this report

Walk through component tables. For each row, the **Suggest** column provides a heuristic default based on:

- **NORMALIZE** — Both platforms have the concept but with different wording; pick one to adopt.
- **PORT→desk** — Web has it, desktop doesn't, but the component exists on desktop; likely should be added.
- **PORT→web** — Desktop has it, web doesn't, but the component exists on web; likely should be added.
- **ACCEPT** — Platform-specific architectural difference; keep as-is.

These suggestions are **heuristics**, not authoritative. Override any suggestion by telling me the disposition you prefer (e.g., "for \`dialog.about.title\`, use NORMALIZE→'About Sangeet Notes Editor'" or "all \`googleDrive.*\` are ACCEPT").

Rows where Desktop and Web have identical values are hidden from this report — they're already symmetric.

## Components

${componentSections.join('\n')}
`;

  writeFileSync(resolve(root, 'docs/strings-parity-report.md'), out, 'utf-8');
  console.log('✓ Wrote docs/strings-parity-report.md');
}
