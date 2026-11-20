import { describe, expect, it } from 'vitest';
import { apa01 } from '../../src/rules/apa-01.ts';
import { ev, scenarioEnd, type ApaStep } from '../../src/schema/events.ts';
import { brief, run } from '../helpers.ts';

const S = 'S1';
const step = (ts: number, s: ApaStep) => ev('APA/STEP', ts, S, { step: s, reason: 't' });
const obs = (ts: number, minDistance: number) => ev('APA/OBSTACLE', ts, S, { minDistance });

describe('APA-01 obstacle too close without abort', () => {
  it('does not fire when aborted in time', () => {
    expect(run([apa01], [step(0, 'MANEUVERING'), obs(1000, 0.2), step(1150, 'ABORTED'), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('fires after the window and tracks the minimum distance', () => {
    const out = run([apa01], [step(0, 'MANEUVERING'), obs(1000, 0.25), obs(1100, 0.2), obs(1300, 0.9), step(1400, 'ABORTED'), scenarioEnd(S, 2000)]);
    expect(brief(out)).toEqual([{ ruleId: 'APA-01', scenarioId: S, startTs: 1000, endTs: 1200, detectTs: 1300 }]);
    expect(out[0]?.evidence).toEqual({ minDistance: 0.2 });
  });

  it('ignores obstacles outside MANEUVERING and safe distances', () => {
    expect(run([apa01], [step(0, 'SEARCHING'), obs(1000, 0.1), scenarioEnd(S, 2000)])).toEqual([]);
    expect(run([apa01], [step(0, 'MANEUVERING'), obs(1000, 0.3), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('keeps the first pending when a new episode starts inside the window', () => {
    const out = run([apa01], [step(0, 'MANEUVERING'), obs(1000, 0.2), obs(1050, 0.5), obs(1100, 0.1), scenarioEnd(S, 3000)]);
    expect(out.map((a) => [a.startTs, a.evidence['minDistance']])).toEqual([[1000, 0.1]]);
  });

  it('reports once for a close episode that continues past the deadline', () => {
    const frames = [1000, 1100, 1200, 1300, 1400].map((t) => obs(t, 0.2));
    const out = run([apa01], [step(0, 'MANEUVERING'), ...frames, step(1450, 'ABORTED'), scenarioEnd(S, 3000)]);
    expect(brief(out)).toEqual([{ ruleId: 'APA-01', scenarioId: S, startTs: 1000, endTs: 1200, detectTs: 1300 }]);
  });

  it('fires at scenario end if still pending', () => {
    expect(brief(run([apa01], [step(0, 'MANEUVERING'), obs(1000, 0.2), scenarioEnd(S, 1100)]))).toEqual([
      { ruleId: 'APA-01', scenarioId: S, startTs: 1000, endTs: 1200, detectTs: 1100 },
    ]);
  });
});
