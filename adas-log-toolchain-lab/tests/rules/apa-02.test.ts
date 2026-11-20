import { describe, expect, it } from 'vitest';
import { apa02 } from '../../src/rules/apa-02.ts';
import { ev, scenarioEnd, type ApaStep } from '../../src/schema/events.ts';
import { brief, run } from '../helpers.ts';

const S = 'S1';
const step = (ts: number, s: ApaStep) => ev('APA/STEP', ts, S, { step: s, reason: 't' });
const obs = (ts: number) => ev('APA/OBSTACLE', ts, S, { minDistance: 1 });

describe('APA-02 parking timeout', () => {
  it('does not fire when completed in time (exactly at the deadline is on time)', () => {
    expect(run([apa02], [step(0, 'MANEUVERING'), step(60000, 'COMPLETED'), scenarioEnd(S, 61000)])).toEqual([]);
  });

  it('fires once on timeout and not again at completion', () => {
    const out = run([apa02], [step(4000, 'MANEUVERING'), obs(64100), step(69000, 'COMPLETED'), scenarioEnd(S, 71000)]);
    expect(brief(out)).toEqual([{ ruleId: 'APA-02', scenarioId: S, startTs: 4000, endTs: 64000, detectTs: 64100 }]);
    expect(out[0]?.message).toBe('Parking maneuver exceeded 60000 ms');
  });

  it('reports an unfinished maneuver at scenario end', () => {
    const out = run([apa02], [step(4000, 'MANEUVERING'), scenarioEnd(S, 30000)]);
    expect(brief(out)).toEqual([{ ruleId: 'APA-02', scenarioId: S, startTs: 4000, endTs: 30000, detectTs: 30000 }]);
    expect(out[0]?.message).toBe('Parking maneuver unfinished at scenario end');
  });

  it('reports timeout (not unfinished) when the scenario ends after the deadline', () => {
    const out = run([apa02], [step(0, 'MANEUVERING')], undefined);
    expect(out[0]?.message).toBe('Parking maneuver unfinished at scenario end');
    const late = run([apa02], [step(0, 'MANEUVERING'), scenarioEnd(S, 70000)]);
    expect(late.map((a) => [a.endTs, a.message])).toEqual([[60000, 'Parking maneuver exceeded 60000 ms']]);
  });

  it('does not report twice when timed out and never completed', () => {
    expect(run([apa02], [step(0, 'MANEUVERING'), obs(60100), scenarioEnd(S, 70000)])).toHaveLength(1);
  });

  it('keeps the first start on repeated MANEUVERING and resets after ABORTED', () => {
    const out = run([apa02], [step(0, 'MANEUVERING'), step(100, 'MANEUVERING'), step(200, 'ABORTED'), scenarioEnd(S, 70000)]);
    expect(out).toEqual([]);
  });
});
