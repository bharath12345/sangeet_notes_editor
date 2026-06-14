import { readFileSync } from 'node:fs';

export type Param = { name: string; type: 'int' | 'string' };
export type Platform = 'both' | 'desktop' | 'web';
export type Disposition = 'port-to-web' | 'port-to-desk' | 'accept' | 'normalize';

export type Entry = {
  value?: string;
  template?: string;
  params?: Param[];
  platform: Platform;
  description: string;
  /** Authoritative parity decision for this entry, set after user review.
   *  Overrides the heuristic Suggest column in the parity report. */
  disposition?: Disposition;
  /** Optional rationale or "ported in commit X" / "merged from desktop+web variants".
   *  When present, the parity report treats the disposition as DONE rather than PENDING. */
  dispositionNote?: string;
};

export type Catalog = {
  $comment?: string;
  entries: Record<string, Entry>;
};

const VALID_DISPOSITIONS: Disposition[] = ['port-to-web', 'port-to-desk', 'accept', 'normalize'];

export function loadCatalog(path: string): Catalog {
  const raw = JSON.parse(readFileSync(path, 'utf-8')) as Partial<Catalog>;
  if (!raw.entries || typeof raw.entries !== 'object') {
    throw new Error(`Invalid catalog at ${path}: missing 'entries' object`);
  }
  // normalize: default platform=both
  for (const [k, v] of Object.entries(raw.entries)) {
    v.platform ??= 'both';
    v.description ??= '';
    if (v.template && !v.params) v.params = [];

    // Reject literal '$' in values/templates (Scala interpolation safety)
    const text = v.value || v.template || '';
    if (text.includes('$')) {
      throw new Error(
        `Catalog string contains unsupported '$' character: "${text}". ` +
          `Use parameterized templates ({name}) for dynamic values.`,
      );
    }

    if (v.disposition !== undefined && !VALID_DISPOSITIONS.includes(v.disposition)) {
      throw new Error(
        `Catalog entry "${k}" has invalid disposition "${v.disposition}". ` +
          `Must be one of: ${VALID_DISPOSITIONS.join(', ')}.`,
      );
    }
  }
  return raw as Catalog;
}

export function keyToElmIdent(key: string): string {
  const parts = key.split('.');
  return (
    parts[0] +
    parts
      .slice(1)
      .map((p) => p[0].toUpperCase() + p.slice(1))
      .join('')
  );
}

export function typeToElm(t: Param['type']): string {
  return t === 'int' ? 'Int' : 'String';
}
