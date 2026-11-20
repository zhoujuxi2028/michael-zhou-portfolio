import type { z } from 'zod';
import { dataSchemas, Envelope, type AdasEvent } from './events.ts';

export type LineResult =
  | { ok: true; event: AdasEvent }
  | { ok: false; kind: 'invalidJson' | 'schemaError' | 'unknownEvent'; detail: string };

function formatIssues(error: z.ZodError, prefix?: string): string {
  return error.issues
    .map((i) => `${[prefix, ...i.path.map(String)].filter(Boolean).join('.')}: ${i.message}`)
    .join('; ');
}

/** Parse one JSON Lines record into a typed event or a classified error. */
export function parseLine(line: string): LineResult {
  let raw: unknown;
  try {
    raw = JSON.parse(line);
  } catch {
    return { ok: false, kind: 'invalidJson', detail: 'invalid JSON' };
  }
  const env = Envelope.safeParse(raw);
  if (!env.success) return { ok: false, kind: 'schemaError', detail: formatIssues(env.error) };
  const key = `${env.data.module}/${env.data.type}`;
  const schema = (dataSchemas as Record<string, z.ZodType | undefined>)[key];
  if (!schema) return { ok: false, kind: 'unknownEvent', detail: key };
  const data = schema.safeParse(env.data.data);
  if (!data.success) return { ok: false, kind: 'schemaError', detail: formatIssues(data.error, 'data') };
  return { ok: true, event: { ...env.data, data: data.data } as unknown as AdasEvent };
}
