import { describe, expect, it } from 'vitest';
import { acc01 } from '../../src/rules/acc-01.ts';
import { ev, scenarioEnd, type AdasEvent } from '../../src/schema/events.ts';
import { brief, run } from '../helpers.ts';

const S = 'S1';
const active = (ts = 0) => ev('ACC/STATE', ts, S, { from: 'OFF', to: 'ACTIVE', reason: 't' });
const track = (ts: number, egoSpeed: number, leadDistance: number | null) =>
  ev('ACC/TRACK', ts, S, { egoSpeed, setSpeed: egoSpeed, leadDistance, leadRelSpeed: 0 });

describe('ACC-01 following distance too short', () => {
  it('does not fire when distance is safe', () => {
    expect(run([acc01], [active(), track(100, 20, 25), scenarioEnd(S, 200)])).toEqual([]);
  });

  it('merges consecutive hits into one anomaly and closes on recovery gap', () => {
    const events: AdasEvent[] = [active(), track(1000, 10, 7), track(1050, 10, 6.5), track(1500, 10, 20), scenarioEnd(S, 3000)];
    const out = run([acc01], events);
    expect(brief(out)).toEqual([{ ruleId: 'ACC-01', scenarioId: S, startTs: 1000, endTs: 1050, detectTs: 1500 }]);
    expect(out[0]?.evidence).toEqual({ minDistance: 6.5, threshold: 10 });
    expect(out[0]?.message).toBe('Following distance 6.50 m below threshold 10.00 m');
  });

  it('uses the 5 m floor at low speed', () => {
    expect(run([acc01], [active(), track(100, 3, 4.9), scenarioEnd(S, 200)])).toHaveLength(1);
    expect(run([acc01], [active(), track(100, 3, 5), scenarioEnd(S, 200)])).toHaveLength(0);
  });

  it('ignores frames when not ACTIVE or without a lead vehicle', () => {
    const standby = ev('ACC/STATE', 0, S, { from: 'OFF', to: 'STANDBY', reason: 't' });
    expect(run([acc01], [standby, track(100, 20, 1), scenarioEnd(S, 200)])).toEqual([]);
    expect(run([acc01], [active(), track(100, 20, null), scenarioEnd(S, 200)])).toEqual([]);
  });

  it('closes the episode when ACC leaves ACTIVE', () => {
    const off = ev('ACC/STATE', 1100, S, { from: 'ACTIVE', to: 'STANDBY', reason: 't' });
    const out = run([acc01], [active(), track(1000, 20, 10), off, scenarioEnd(S, 2000)]);
    expect(brief(out)).toEqual([{ ruleId: 'ACC-01', scenarioId: S, startTs: 1000, endTs: 1000, detectTs: 1100 }]);
  });

  it('splits hits separated by more than mergeGapMs', () => {
    const out = run([acc01], [active(), track(1000, 20, 10), track(1300, 20, 10), scenarioEnd(S, 2000)]);
    expect(out.map((a) => [a.startTs, a.endTs])).toEqual([[1000, 1000], [1300, 1300]]);
  });

  it('reports an open episode at scenario end', () => {
    expect(brief(run([acc01], [active(), track(1000, 20, 10), scenarioEnd(S, 1100)]))).toEqual([
      { ruleId: 'ACC-01', scenarioId: S, startTs: 1000, endTs: 1000, detectTs: 1100 },
    ]);
  });

  it('honours config overrides', () => {
    const events = [active(), track(100, 20, 25), scenarioEnd(S, 200)];
    expect(run([acc01], events, { rules: { 'ACC-01': { timeGapS: 1.5 } } })).toHaveLength(1);
  });
});

describe('ACC-01 out-of-order input', () => {
  it('does not split an episode when a late hit arrives', () => {
    const events: AdasEvent[] = [active(), track(1000, 20, 10), track(1100, 20, 10), track(1050, 20, 10), track(1300, 20, 10), scenarioEnd(S, 3000)];
    expect(brief(run([acc01], events))).toEqual([{ ruleId: 'ACC-01', scenarioId: S, startTs: 1000, endTs: 1300, detectTs: 3000 }]);
  });
});
