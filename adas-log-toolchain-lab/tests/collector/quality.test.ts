import { describe, expect, it } from 'vitest';
import { createQualityMonitor } from '../../src/collector/quality.ts';
import { ev, scenarioEnd, type AdasEvent } from '../../src/schema/events.ts';

const track = (ts: number, s = 'S1') =>
  ev('ACC/TRACK', ts, s, { egoSpeed: 20, setSpeed: 20, leadDistance: null, leadRelSpeed: 0 });
const brake = (ts: number, s = 'S1') => ev('ACC/BRAKE_PEDAL', ts, s, { pressed: true });

function measure(events: readonly AdasEvent[]) {
  const q = createQualityMonitor();
  for (const e of events) q.push(e);
  return q.report();
}

describe('quality monitor', () => {
  it('counts dropped ACC/TRACK frames at a 50 ms period', () => {
    // gaps: 50, 50, 150 (2 dropped), 75 (not > 1.5 periods), 76 (round(76/50)-1 = 1)
    const r = measure([track(0), track(50), track(100), track(250), track(325), track(401)]);
    expect(r.trackPeriodMs).toBe(50);
    expect(r.totals).toMatchObject({ trackFrames: 6, droppedFrames: 3 });
    expect(r.totals.dropRate).toBeCloseTo(3 / 9);
  });

  it('ignores non-TRACK events between frames when counting drops', () => {
    const r = measure([track(0), brake(20), track(50)]);
    expect(r.totals.droppedFrames).toBe(0);
  });

  it('counts exact duplicates of the previous line in the same scenario', () => {
    const r = measure([track(0), track(0), track(0, 'S2'), track(0), brake(10), track(0)]);
    expect(r.totals.duplicates).toBe(2);
  });

  it('uses the raw line for duplicates when given', () => {
    const q = createQualityMonitor();
    q.push(track(0), '{"a":1}');
    q.push(track(0), '{"a":2}');
    expect(q.report().totals.duplicates).toBe(0);
  });

  it('counts ts going backwards and gaps over 1000 ms', () => {
    // 0 -> 1000 ok (exactly 1000), 1000 -> 2001 gap, 2001 -> 1500 backwards, 1500 -> 2100 fine (600 ms step)
    const r = measure([brake(0), brake(1000), brake(2001), brake(1500), brake(2100)]);
    expect(r.totals.tsAnomalies).toBe(2);
  });

  it('compares each event with the previous one, so a single ts spike is exactly 2 anomalies', () => {
    // 100 -> 1e9 jumps up, 1e9 -> 200 jumps back; the events after the spike are normal again.
    const r = measure([brake(0), brake(100), brake(1e9), brake(200), brake(300), brake(400)]);
    expect(r.totals.tsAnomalies).toBe(2);
  });

  it('reports per scenario, sorted by scenario id, with zero drop rate when there are no frames', () => {
    const r = measure([brake(0, 'S2'), track(0, 'S1'), track(150, 'S1'), scenarioEnd('S1', 200), scenarioEnd('S2', 100)]);
    expect(r.byScenario).toEqual([
      { scenarioId: 'S1', trackFrames: 2, droppedFrames: 2, dropRate: 0.5, duplicates: 0, tsAnomalies: 0 },
      { scenarioId: 'S2', trackFrames: 0, droppedFrames: 0, dropRate: 0, duplicates: 0, tsAnomalies: 0 },
    ]);
    expect(r.totals).toEqual({ trackFrames: 2, droppedFrames: 2, dropRate: 0.5, duplicates: 0, tsAnomalies: 0 });
  });

  it('releases per-scenario tracking at SCENARIO_END (a new instance starts fresh)', () => {
    const q = createQualityMonitor();
    for (const e of [track(0), track(9000), scenarioEnd('S1', 9000), track(0), track(50)]) q.push(e);
    expect(q.openScenarios()).toBe(1);
    // 0 -> 9000 is one gap anomaly; the restarted instance at ts 0 is not "backwards"
    expect(q.report().byScenario[0]).toMatchObject({ trackFrames: 4, tsAnomalies: 1 });
  });
});
