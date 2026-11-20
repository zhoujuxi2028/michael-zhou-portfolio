import { describe, expect, it } from 'vitest';
import { createVirtualClock, paced, realClock } from '../../src/collector/clock.ts';
import { scenarioEnd } from '../../src/schema/events.ts';

const at = (...ts: number[]) => ts.map((t) => scenarioEnd('S', t));

async function emitTimes(ts: number[], speed: number) {
  const clock = createVirtualClock(1000);
  const out: [number, number][] = [];
  for await (const e of paced(at(...ts), speed, clock)) out.push([e.ts, clock.now() - 1000]);
  expect(out.map(([t]) => t)).toEqual(ts);
  return out.map(([, t]) => t);
}

describe('virtual clock', () => {
  it('advances instantly on sleep', async () => {
    const c = createVirtualClock();
    await c.sleep(250);
    await c.sleep(-5);
    expect(c.now()).toBe(250);
  });
});

describe('paced', () => {
  it('paces by scenario ts / speed', async () => {
    expect(await emitTimes([0, 1000, 3000], 1)).toEqual([0, 1000, 3000]);
    expect(await emitTimes([0, 1000, 3000], 4)).toEqual([0, 250, 750]);
  });

  it('does not wait when ts resets for the next scenario or goes backwards', async () => {
    expect(await emitTimes([0, 1000, 0, 500, 400, 600], 1)).toEqual([0, 1000, 1000, 1500, 1500, 1700]);
  });

  it('does not sleep when the consumer is already behind schedule', async () => {
    const clock = createVirtualClock();
    let sleeps = 0;
    const slow = { now: clock.now, sleep: (ms: number) => { sleeps++; return clock.sleep(ms); } };
    const seen: number[] = [];
    for await (const e of paced(at(0, 10, 20), 1, slow)) {
      seen.push(e.ts);
      await clock.sleep(100);
    }
    expect(seen).toEqual([0, 10, 20]);
    expect(sleeps).toBe(0);
  });
});

describe('real clock', () => {
  it('reads wall time and sleeps with a timer', async () => {
    const t0 = realClock.now();
    await realClock.sleep(5);
    expect(realClock.now()).toBeGreaterThanOrEqual(t0 + 4);
  });
});
