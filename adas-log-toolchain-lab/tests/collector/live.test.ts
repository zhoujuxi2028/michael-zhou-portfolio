import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { createVirtualClock } from '../../src/collector/clock.ts';
import { recordSession } from '../../src/collector/session.ts';
import { fileSource, liveSource } from '../../src/collector/source.ts';
import { generate } from '../../src/generator/generate.ts';
import { toJsonl } from '../../src/io/jsonl.ts';
import type { AdasEvent } from '../../src/schema/events.ts';

const tmp = () => mkdtempSync(join(tmpdir(), 'adas-live-'));

/** Expected paced duration: forward ts steps summed, divided by speed. */
function streamMs(events: readonly AdasEvent[], speed: number): number {
  let total = 0;
  events.forEach((e, i) => {
    const prev = events[i - 1];
    if (prev && e.ts > prev.ts) total += e.ts - prev.ts;
  });
  return total / speed;
}

describe('record session: live source', () => {
  const gen = { seed: 7, scenarios: 8 };

  it('paces generator output on the injected clock and records the same clips as the file source', async () => {
    const clock = createVirtualClock();
    const live = await recordSession(liveSource({ ...gen, speed: 10, clock }), { out: tmp(), source: 'live' });
    const { events } = generate(gen);
    expect(clock.now()).toBe(streamMs(events, 10));
    expect(live.events).toBe(events.length);
    expect(live.index.meta.source).toBe('live');

    const dir = tmp(), input = join(dir, 'events.jsonl');
    writeFileSync(input, toJsonl(events));
    const file = await recordSession(fileSource(input), { out: join(dir, 'clips'), source: input });
    expect(live.index.clips).toEqual(file.index.clips);
    expect(live.quality).toEqual(file.quality);
    expect(live.index.clips.length).toBeGreaterThan(0);
  });
});
