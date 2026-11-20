import { describe, expect, it } from 'vitest';
import { lka01 } from '../../src/rules/lka-01.ts';
import { ev, scenarioEnd, type AdasEvent } from '../../src/schema/events.ts';
import { brief, run } from '../helpers.ts';

const S = 'S1';
const signal = (ts: number, side: 'LEFT' | 'RIGHT' | 'OFF') => ev('VEH/TURN_SIGNAL', ts, S, { side });
const steer = (ts: number, torque: number) => ev('LKA/STEER', ts, S, { torque });

describe('LKA-01 steering intervention with turn signal on', () => {
  it('fires for strong torque while the turn signal is LEFT or RIGHT', () => {
    for (const side of ['LEFT', 'RIGHT'] as const) {
      const out = run([lka01], [signal(0, side), steer(1000, 0.9), scenarioEnd(S, 2000)]);
      expect(brief(out)).toEqual([{ ruleId: 'LKA-01', scenarioId: S, startTs: 1000, endTs: 1000, detectTs: 2000 }]);
    }
  });

  it('does not fire when the turn signal is OFF or was never set', () => {
    expect(run([lka01], [steer(1000, 2), scenarioEnd(S, 2000)])).toEqual([]);
    expect(run([lka01], [signal(0, 'LEFT'), signal(500, 'OFF'), steer(1000, 2), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('uses |torque| > 0.5 strictly, for both directions', () => {
    const at = (t: number) => run([lka01], [signal(0, 'LEFT'), steer(1000, t), scenarioEnd(S, 2000)]).length;
    expect([at(0.5), at(-0.5), at(0.51), at(-0.51)]).toEqual([0, 0, 1, 1]);
  });

  it('merges consecutive hits and closes after the merge gap', () => {
    const events: AdasEvent[] = [signal(0, 'LEFT'), steer(1000, 0.9), steer(1100, 0.8), steer(1200, 0.1), steer(1400, 0.1), steer(1500, 0.1), scenarioEnd(S, 3000)];
    const out = run([lka01], events);
    expect(brief(out)).toEqual([{ ruleId: 'LKA-01', scenarioId: S, startTs: 1000, endTs: 1100, detectTs: 1400 }]);
    expect(out[0]?.evidence).toEqual({ maxTorque: 0.9, threshold: 0.5 });
    expect(out[0]?.message).toBe('Steering torque 0.90 Nm above threshold 0.50 Nm with turn signal on');
  });

  it('splits hits separated by more than mergeGapMs', () => {
    const out = run([lka01], [signal(0, 'LEFT'), steer(1000, 1), steer(1300, 1), scenarioEnd(S, 2000)]);
    expect(out.map((a) => [a.startTs, a.endTs])).toEqual([[1000, 1000], [1300, 1300]]);
  });

  it('closes the episode when the turn signal goes OFF', () => {
    const out = run([lka01], [signal(0, 'LEFT'), steer(1000, 1), signal(1100, 'OFF'), scenarioEnd(S, 2000)]);
    expect(brief(out)).toEqual([{ ruleId: 'LKA-01', scenarioId: S, startTs: 1000, endTs: 1000, detectTs: 1100 }]);
  });

  it('keeps the episode open when the signal switches LEFT to RIGHT', () => {
    const out = run([lka01], [signal(0, 'LEFT'), steer(1000, 1), signal(1050, 'RIGHT'), steer(1100, 1), scenarioEnd(S, 2000)]);
    expect(out.map((a) => [a.startTs, a.endTs])).toEqual([[1000, 1100]]);
  });

  it('honours config overrides', () => {
    const events = [signal(0, 'LEFT'), steer(1000, 0.3), scenarioEnd(S, 2000)];
    expect(run([lka01], events, { rules: { 'LKA-01': { torqueNm: 0.2 } } })).toHaveLength(1);
  });

  it('does not split an episode when a late hit arrives', () => {
    const events: AdasEvent[] = [signal(0, 'LEFT'), steer(1000, 1), steer(1100, 1), steer(1050, 1), steer(1300, 1), scenarioEnd(S, 3000)];
    expect(brief(run([lka01], events))).toEqual([{ ruleId: 'LKA-01', scenarioId: S, startTs: 1000, endTs: 1300, detectTs: 3000 }]);
  });
});
