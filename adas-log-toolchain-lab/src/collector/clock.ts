import { setTimeout as delay } from 'node:timers/promises';
import type { AdasEvent } from '../schema/events.ts';

/** Injectable time source so live recording can be tested without real waiting. */
export interface Clock {
  now(): number;
  sleep(ms: number): Promise<void>;
}

export const realClock: Clock = {
  now: () => performance.now(),
  sleep: async (ms) => { await delay(ms); },
};

/** A clock whose `sleep` advances time immediately. */
export function createVirtualClock(start = 0): Clock {
  let t = start;
  return {
    now: () => t,
    sleep: async (ms) => { t += Math.max(0, ms); },
  };
}

/**
 * Yield events no faster than scenario time / `speed`. Stream time advances by the forward
 * ts delta between consecutive events; a ts reset (next scenario) or step back adds no delay.
 */
export async function* paced(events: Iterable<AdasEvent>, speed: number, clock: Clock): AsyncIterable<AdasEvent> {
  const t0 = clock.now();
  let streamMs = 0;
  let last: number | null = null;
  for (const e of events) {
    if (last !== null && e.ts > last) streamMs += e.ts - last;
    last = e.ts;
    const wait = t0 + streamMs / speed - clock.now();
    if (wait > 0) await clock.sleep(wait);
    yield e;
  }
}
