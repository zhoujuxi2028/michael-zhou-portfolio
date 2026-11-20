import { describe, expect, it } from 'vitest';
import { createRingBuffer } from '../../src/collector/ring-buffer.ts';
import { ev } from '../../src/schema/events.ts';

const track = (ts: number) =>
  ev('ACC/TRACK', ts, 'S1', { egoSpeed: 20, setSpeed: 20, leadDistance: null, leadRelSpeed: 0 });
const tsOf = (es: readonly { ts: number }[]) => es.map((e) => e.ts);

describe('ring buffer', () => {
  it('keeps only the last bufferMs of scenario time', () => {
    const b = createRingBuffer(1000);
    for (let ts = 0; ts <= 3000; ts += 500) b.push(track(ts));
    expect(tsOf(b.events())).toEqual([2000, 2500, 3000]);
    expect(b.evictedUpTo()).toBe(1500);
  });

  it('stays bounded over a long stream', () => {
    const b = createRingBuffer(1000);
    let peak = 0;
    for (let ts = 0; ts <= 1_000_000; ts += 50) {
      b.push(track(ts));
      peak = Math.max(peak, b.size());
    }
    expect(peak).toBe(21);
  });

  it('reports nothing evicted for a fresh buffer', () => {
    const b = createRingBuffer(1000);
    b.push(track(100));
    expect(b.evictedUpTo()).toBe(-Infinity);
  });

  it('keeps pinned events even when they are older than bufferMs', () => {
    const b = createRingBuffer(1000);
    for (let ts = 0; ts <= 1000; ts += 500) b.push(track(ts));
    b.pin(500);
    for (let ts = 1500; ts <= 4000; ts += 500) b.push(track(ts));
    expect(tsOf(b.events())[0]).toBe(500);
    b.pin(null);
    b.push(track(4500));
    expect(tsOf(b.events())).toEqual([3500, 4000, 4500]);
  });

  it('notifies evicted events in arrival order', () => {
    const evicted: number[] = [];
    const b = createRingBuffer(1000, (e) => evicted.push(e.ts));
    for (let ts = 0; ts <= 2500; ts += 500) b.push(track(ts));
    expect(evicted).toEqual([0, 500, 1000]);
  });
});
