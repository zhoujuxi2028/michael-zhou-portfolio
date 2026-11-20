import { z } from 'zod';
import { UsageError } from '../config.ts';

export interface Ticket { key: string; summary: string; description: string; components: string[]; labels: string[] }
export interface Sections { steps: string[]; expected: string[] }

const ExportSchema = z.object({
  issues: z.array(z.object({
    key: z.string().regex(/^[A-Za-z][A-Za-z0-9_]*-\d+$/),
    fields: z.object({
      summary: z.string(),
      description: z.string().nullish(),
      components: z.array(z.object({ name: z.string() })).nullish(),
      labels: z.array(z.string()).nullish(),
    }),
  })),
});

/** Parse a Jira REST v2 style export (field subset). Invalid input throws UsageError. */
export function parseExport(text: string): Ticket[] {
  let raw: unknown;
  try {
    raw = JSON.parse(text);
  } catch {
    throw new UsageError('ticket export is not valid JSON');
  }
  const r = ExportSchema.safeParse(raw);
  if (!r.success) {
    throw new UsageError(`invalid ticket export: ${r.error.issues.map((i) => `${i.path.map(String).join('.')}: ${i.message}`).join('; ')}`);
  }
  const seen = new Set<string>();
  return r.data.issues.map((i) => {
    if (seen.has(i.key)) throw new UsageError(`invalid ticket export: duplicate issue key ${i.key}`);
    seen.add(i.key);
    return {
      key: i.key,
      summary: i.fields.summary,
      description: i.fields.description ?? '',
      components: (i.fields.components ?? []).map((c) => c.name),
      labels: i.fields.labels ?? [],
    };
  });
}

const LIST_ITEM = /^\s*(?:#+|\*+|-+|\d+[.)])\s+(.*\S)\s*$/;
const HEADINGS: readonly RegExp[] = [
  /^\s*h[1-6]\.\s*([^:]*?)\s*(?::\s*(.*))?$/i, // h3. Steps
  /^\s*\*([^*]+)\*\s*:?\s*(.*)$/, // *Steps*
  /^\s*([A-Za-z][A-Za-z ]{0,30}?)\s*:\s*(.*)$/, // Steps:
];

/** Extract `Steps` / `Expected` sections from a Jira wiki or plain-text description. */
export function parseDescription(description: string | null | undefined): Sections {
  const out: Sections = { steps: [], expected: [] };
  let current: keyof Sections | undefined;
  for (const line of (description ?? '').split(/\r?\n/)) {
    if (line.trim() === '') continue;
    const item = LIST_ITEM.exec(line);
    if (item) {
      if (current) out[current].push(item[1] as string);
      continue;
    }
    const m = HEADINGS.map((h) => h.exec(line)).find((x) => x !== null && x !== undefined);
    if (!m) {
      if (current) out[current].push(line.trim());
      continue;
    }
    const name = (m[1] ?? '').trim().toLowerCase();
    current = name === 'steps' ? 'steps' : name === 'expected' ? 'expected' : undefined;
    const rest = (m[2] ?? '').trim();
    if (current && rest) out[current].push(rest);
  }
  return out;
}
