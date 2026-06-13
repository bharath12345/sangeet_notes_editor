import { readFileSync } from 'node:fs';

export type Param = { name: string; type: 'int' | 'string' };
export type Platform = 'both' | 'desktop' | 'web';

export type Entry = {
  value?: string;
  template?: string;
  params?: Param[];
  platform: Platform;
  description: string;
};

export type Catalog = {
  $comment?: string;
  entries: Record<string, Entry>;
};

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
