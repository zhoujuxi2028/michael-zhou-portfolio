import { readFile } from 'node:fs/promises';
import { dirname, isAbsolute, relative, resolve } from 'node:path';
import { z } from 'zod';
import type { Manifest } from '../casegen/regression.ts';
import { UsageError } from '../config.ts';

const ManifestSchema = z.object({
  meta: z.object({
    toolVersion: z.string(), rulesetVersion: z.string(), anomalies: z.string(), log: z.string(),
    preMs: z.number(), postMs: z.number(),
  }),
  cases: z.array(z.object({
    caseId: z.string().min(1),
    ruleId: z.string().min(1),
    scenarioId: z.string().min(1),
    source: z.string().min(1),
    window: z.tuple([z.number(), z.number()]),
    expect: z.object({ ruleId: z.string().min(1), triggered: z.boolean() }),
  })),
});

/** Read and validate `manifest.json` (contract C1); any problem is a usage error (exit 2). */
export async function loadManifest(path: string): Promise<Manifest> {
  let text: string;
  try {
    text = await readFile(path, 'utf8');
  } catch {
    throw new UsageError(`manifest not found: ${path}`);
  }
  let raw: unknown;
  try {
    raw = JSON.parse(text);
  } catch {
    throw new UsageError(`manifest is not valid JSON: ${path}`);
  }
  const r = ManifestSchema.safeParse(raw);
  if (!r.success) {
    throw new UsageError(`invalid manifest: ${r.error.issues.map((i) => `${i.path.map(String).join('.')}: ${i.message}`).join('; ')}`);
  }
  const base = resolve(dirname(path));
  for (const c of r.data.cases) {
    const rel = relative(base, resolve(base, c.source));
    if (rel === '' || rel.startsWith('..') || isAbsolute(rel)) {
      throw new UsageError(`invalid manifest: case ${c.caseId} source "${c.source}" escapes the manifest directory`);
    }
  }
  return r.data;
}
