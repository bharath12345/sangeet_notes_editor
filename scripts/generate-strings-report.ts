import { writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadCatalog, Entry } from './lib/catalog.ts';

type GroupedEntries = Map<string, [string, Entry][]>;

function extractPrefix(key: string): string {
  const parts = key.split('.');
  return parts.length >= 2 ? `${parts[0]}.${parts[1]}` : parts[0];
}

function groupByPrefix(entries: [string, Entry][]): GroupedEntries {
  const groups = new Map<string, [string, Entry][]>();
  for (const entry of entries) {
    const prefix = extractPrefix(entry[0]);
    if (!groups.has(prefix)) groups.set(prefix, []);
    groups.get(prefix)!.push(entry);
  }
  // Sort entries within each group
  for (const entries of groups.values()) {
    entries.sort((a, b) => a[0].localeCompare(b[0]));
  }
  return groups;
}

function escapeMarkdown(text: string): string {
  return text.replace(/\|/g, '\\|').replace(/\*/g, '\\*').replace(/`/g, '\\`');
}

function formatValueOrTemplate(entry: Entry): string {
  const text = entry.value ?? entry.template ?? '';
  const escaped = escapeMarkdown(text);
  if (entry.template && entry.params && entry.params.length > 0) {
    return `${escaped} [${entry.params.length} param${entry.params.length > 1 ? 's' : ''}]`;
  }
  return escaped;
}

function generateGroupSection(groups: GroupedEntries): string {
  const sortedPrefixes = Array.from(groups.keys()).sort();
  const sections: string[] = [];

  for (const prefix of sortedPrefixes) {
    const entries = groups.get(prefix)!;
    sections.push(`### ${prefix}\n`);
    sections.push('| Key | Value / Template | Description | Disposition |');
    sections.push('| --- | ---------------- | ----------- | ----------- |');
    for (const [key, entry] of entries) {
      const value = formatValueOrTemplate(entry);
      const desc = escapeMarkdown(entry.description);
      sections.push(`| \`${key}\` | \`${value}\` | ${desc} | TODO |`);
    }
    sections.push('');
  }

  return sections.join('\n');
}

// CLI entry point
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  // Script runs from scripts/ dir, so repo root is one level up
  const root = resolve(process.cwd(), '..');
  const catalog = loadCatalog(resolve(root, 'sangeet-core/src/main/resources/ui-strings.json'));

  const both = Object.entries(catalog.entries).filter(([, e]) => e.platform === 'both');
  const desktopOnly = Object.entries(catalog.entries).filter(([, e]) => e.platform === 'desktop');
  const webOnly = Object.entries(catalog.entries).filter(([, e]) => e.platform === 'web');

  const now = new Date().toISOString().slice(0, 10);

  const desktopGroups = groupByPrefix(desktopOnly);
  const webGroups = groupByPrefix(webOnly);

  const out = `# UI Strings Parity Report

> Generated: ${now}. Regenerate with \`make strings-report\`.

## Summary

| Bucket                         | Count |
| ------------------------------ | ----- |
| Shared (\`platform: both\`)    | ${both.length} |
| Desktop-only                   | ${desktopOnly.length} |
| Web-only                       | ${webOnly.length} |
| **Total**                      | **${Object.keys(catalog.entries).length}** |

**Goal:** Minimize Desktop-only and Web-only buckets toward zero by dispositioning each entry:
- **PORT** — add equivalent UI to the missing side
- **REMOVE** — delete from the side that has it
- **ACCEPT** — keep as justified platform-specific

## Desktop-only entries (review one-by-one)

Grouped by \`area.component\` prefix for easier review.

${generateGroupSection(desktopGroups)}

## Web-only entries (review one-by-one)

Grouped by \`area.component\` prefix for easier review.

${generateGroupSection(webGroups)}

## Shared entries summary

${both.length} shared entries. Full list omitted; query the catalog directly:
\`\`\`bash
jq '.entries | to_entries[] | select(.value.platform=="both") | .key' \\
  sangeet-core/src/main/resources/ui-strings.json
\`\`\`
`;

  writeFileSync(resolve(root, 'docs/strings-parity-report.md'), out, 'utf-8');
  console.log('✓ Wrote docs/strings-parity-report.md');
}
