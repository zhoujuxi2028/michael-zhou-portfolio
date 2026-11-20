import { readFile } from 'node:fs/promises';
import { z } from 'zod';
import type { EngineConfig } from './engine/engine.ts';
import { RULES } from './rules/index.ts';

/** Errors that map to exit code 2. */
export class UsageError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'UsageError';
  }
}

const ConfigSchema = z.object({
  rules: z.record(z.string(), z.record(z.string(), z.number())).optional(),
  epsilon: z.object({ distance: z.number().positive(), time: z.number().positive(), torque: z.number().positive() }).partial().optional(),
}).strict();

export async function loadConfig(path: string): Promise<EngineConfig> {
  let text: string;
  try {
    text = await readFile(path, 'utf8');
  } catch {
    throw new UsageError(`config file not found: ${path}`);
  }
  let raw: unknown;
  try {
    raw = JSON.parse(text);
  } catch {
    throw new UsageError(`config is not valid JSON: ${path}`);
  }
  const r = ConfigSchema.safeParse(raw);
  if (!r.success) {
    throw new UsageError(`invalid config: ${r.error.issues.map((i) => `${i.path.map(String).join('.')}: ${i.message}`).join('; ')}`);
  }
  for (const [id, overrides] of Object.entries(r.data.rules ?? {})) {
    const rule = RULES.find((x) => x.id === id);
    if (!rule) throw new UsageError(`invalid config: unknown rule id "${id}"`);
    for (const key of Object.keys(overrides)) {
      if (!(key in rule.defaults)) throw new UsageError(`invalid config: unknown key "${key}" for rule ${id}`);
    }
  }
  return r.data;
}
