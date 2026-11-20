import type { AdasEvent } from '../schema/events.ts';

/** State dimensions a replayed clip needs to reach the rule's precondition (contract C1/C3). */
export const STATE_KEYS = ['ACC/STATE', 'APA/STEP', 'LKA/STATE', 'VEH/TURN_SIGNAL'] as const;

export function stateKey(e: AdasEvent): string | null {
  const key = `${e.module}/${e.type}`;
  return (STATE_KEYS as readonly string[]).includes(key) ? key : null;
}

/** Remembers the latest evicted state event per dimension, so preconditions survive the ring buffer. */
export interface PreconditionTracker {
  evicted(e: AdasEvent): void;
  /** Latest state event per dimension with ts < `startTs` (or lost to eviction), sorted by ts. */
  before(startTs: number, buffered: readonly AdasEvent[]): AdasEvent[];
}

const later = (a: AdasEvent | undefined, b: AdasEvent) => (a === undefined || b.ts >= a.ts ? b : a);

export function createPreconditionTracker(): PreconditionTracker {
  const lastEvicted = new Map<string, AdasEvent>();
  return {
    evicted(e) {
      const key = stateKey(e);
      if (key) lastEvicted.set(key, later(lastEvicted.get(key), e));
    },
    before(startTs, buffered) {
      const latest = new Map(lastEvicted);
      for (const e of buffered) {
        const key = stateKey(e);
        if (key && e.ts < startTs) latest.set(key, later(latest.get(key), e));
      }
      return [...latest.values()].sort((a, b) => a.ts - b.ts);
    },
  };
}
