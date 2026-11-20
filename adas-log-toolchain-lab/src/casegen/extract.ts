import { readLines } from '../io/jsonl.ts';
import type { AdasEvent } from '../schema/events.ts';
import { parseLine } from '../schema/validate.ts';

/** One slice to cut from a log: events of `scenarioId` with `from <= ts <= to`. */
export interface WindowRequest { scenarioId: string; from: number; to: number }

export interface ExtractedWindow {
  /** Inclusive replay window. */
  window: [number, number];
  /** Self-contained events in replay order. */
  events: AdasEvent[];
}

interface Slot { req: WindowRequest; events: AdasEvent[] }

/**
 * Cut every requested window out of a JSON Lines log in a single streaming pass.
 * Only events of requested scenarios inside a window are kept in memory.
 * Invalid lines are skipped so the extracted data is clean.
 */
export async function extractWindows(path: string, requests: readonly WindowRequest[]): Promise<ExtractedWindow[]> {
  const slots: Slot[] = requests.map((req) => ({ req, events: [] }));
  const byScenario = Map.groupBy(slots, (s) => s.req.scenarioId);
  for await (const line of readLines(path)) {
    const r = parseLine(line);
    if (!r.ok) continue;
    const e = r.event;
    for (const s of byScenario.get(e.scenarioId) ?? []) {
      if (e.ts >= s.req.from && e.ts <= s.req.to) s.events.push(e);
    }
  }
  return slots.map((s) => ({ window: [s.req.from, s.req.to], events: s.events }));
}
