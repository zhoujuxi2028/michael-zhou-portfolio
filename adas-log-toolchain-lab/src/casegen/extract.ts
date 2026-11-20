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

/** State dimensions whose latest value before the window is replayed as a precondition. */
export const PRECONDITION_KEYS: readonly string[] = ['ACC/STATE', 'APA/STEP', 'LKA/STATE', 'VEH/TURN_SIGNAL'];

interface Slot { req: WindowRequest; pre: Map<string, AdasEvent>; events: AdasEvent[] }

/**
 * Cut every requested window out of a JSON Lines log in a single streaming pass.
 * Only events of requested scenarios inside a window are kept in memory.
 * Invalid lines are skipped so the extracted data is clean.
 *
 * Preconditions: for each state dimension in `PRECONDITION_KEYS`, the latest event before the
 * window (original `ts`) is prepended, and `window[0]` is lowered to the earliest of them so a
 * replay with the window does not cut them off. A `SCENARIO_END` before the window resets them
 * (states of an earlier, ended instance of the same id do not carry over).
 */
export async function extractWindows(path: string, requests: readonly WindowRequest[]): Promise<ExtractedWindow[]> {
  const slots: Slot[] = requests.map((req) => ({ req, pre: new Map(), events: [] }));
  const byScenario = Map.groupBy(slots, (s) => s.req.scenarioId);
  for await (const line of readLines(path)) {
    const r = parseLine(line);
    if (!r.ok) continue;
    const e = r.event;
    for (const s of byScenario.get(e.scenarioId) ?? []) {
      if (e.ts >= s.req.from && e.ts <= s.req.to) s.events.push(e);
      else if (e.ts < s.req.from) {
        const key = `${e.module}/${e.type}`;
        if (key === 'SYS/SCENARIO_END') s.pre.clear();
        else if (PRECONDITION_KEYS.includes(key)) s.pre.set(key, e);
      }
    }
  }
  return slots.map(({ req, pre, events }) => {
    const preconditions = [...pre.values()].sort((a, b) => a.ts - b.ts);
    const from = Math.min(req.from, ...preconditions.map((e) => e.ts));
    return { window: [from, req.to], events: [...preconditions, ...events] };
  });
}
