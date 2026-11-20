import type { AdasEvent } from '../schema/events.ts';

/** Per-scenario buffer of the most recent `bufferMs` of scenario time, in arrival order. */
export interface RingBuffer {
  push(e: AdasEvent): void;
  /** Buffered events, oldest first (arrival order). */
  events(): readonly AdasEvent[];
  size(): number;
  /** Largest ts evicted so far (-Infinity if nothing was evicted). */
  evictedUpTo(): number;
  /** Keep events with ts >= `fromTs` while a clip is recording; `null` releases the pin. */
  pin(fromTs: number | null): void;
}

/** Compact the backing array once this many evicted slots accumulate at its head. */
const COMPACT_AT = 1024;

export function createRingBuffer(bufferMs: number, onEvict?: (e: AdasEvent) => void): RingBuffer {
  let items: AdasEvent[] = [];
  let head = 0;
  let now = -Infinity;
  let evicted = -Infinity;
  let pinned: number | null = null;

  const evict = () => {
    const bound = Math.min(now - bufferMs, pinned ?? Infinity);
    while (head < items.length && (items[head] as AdasEvent).ts < bound) {
      const e = items[head++] as AdasEvent;
      evicted = Math.max(evicted, e.ts);
      onEvict?.(e);
    }
    if (head >= COMPACT_AT) {
      items = items.slice(head);
      head = 0;
    }
  };

  return {
    push(e) {
      items.push(e);
      now = Math.max(now, e.ts);
      evict();
    },
    events: () => items.slice(head),
    size: () => items.length - head,
    evictedUpTo: () => evicted,
    pin(fromTs) {
      pinned = fromTs;
      evict();
    },
  };
}
