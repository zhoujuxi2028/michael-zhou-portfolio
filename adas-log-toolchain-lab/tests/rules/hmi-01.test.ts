import { describe, expect, it } from 'vitest';
import { hmi01 } from '../../src/rules/hmi-01.ts';
import { ev, scenarioEnd } from '../../src/schema/events.ts';
import { brief, run } from '../helpers.ts';

const S = 'S1';
const acc = (ts: number, to: 'STANDBY' | 'ACTIVE' | 'OVERRIDE') => ev('ACC/STATE', ts, S, { from: 'OFF', to, reason: 't' });
const show = (ts: number, feature: 'ACC' | 'APA', value: string) => ev('HMI/DISPLAY', ts, S, { feature, item: 'STATE', value });
const tick = (ts: number) => ev('ACC/BRAKE_PEDAL', ts, S, { pressed: false });

describe('HMI-01 HMI state mismatch', () => {
  it('passes when HMI shows the new state in time', () => {
    expect(run([hmi01], [acc(1000, 'ACTIVE'), show(1100, 'ACC', 'ACTIVE'), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('fires when nothing is shown within the window', () => {
    const out = run([hmi01], [acc(1000, 'ACTIVE'), tick(1350), scenarioEnd(S, 2000)]);
    expect(brief(out)).toEqual([{ ruleId: 'HMI-01', scenarioId: S, startTs: 1000, endTs: 1300, detectTs: 1350 }]);
    expect(out[0]?.message).toBe('HMI did not show ACC state ACTIVE within 300 ms');
  });

  it('fires when the last shown value is wrong', () => {
    const out = run([hmi01], [acc(1000, 'OVERRIDE'), show(1100, 'ACC', 'ACTIVE'), scenarioEnd(S, 2000)]);
    expect(out[0]?.message).toBe('HMI showed ACC state ACTIVE, expected OVERRIDE');
    expect(out[0]?.evidence).toEqual({ feature: 'ACC', expected: 'OVERRIDE', shown: 'ACTIVE' });
  });

  it('uses the last display in the window', () => {
    expect(run([hmi01], [acc(1000, 'ACTIVE'), show(1050, 'ACC', 'STANDBY'), show(1200, 'ACC', 'ACTIVE'), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('ignores displays after the deadline and of other features', () => {
    expect(run([hmi01], [acc(1000, 'ACTIVE'), show(1100, 'APA', 'ACTIVE'), show(1301, 'ACC', 'ACTIVE'), scenarioEnd(S, 2000)])).toHaveLength(1);
  });

  it('supersedes a pending check when the feature changes again', () => {
    expect(run([hmi01], [acc(1000, 'ACTIVE'), acc(1100, 'STANDBY'), show(1200, 'ACC', 'STANDBY'), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('covers APA STEP changes', () => {
    const step = ev('APA/STEP', 0, S, { step: 'MANEUVERING', reason: 't' });
    expect(run([hmi01], [step, show(100, 'APA', 'MANEUVERING'), scenarioEnd(S, 1000)])).toEqual([]);
    expect(run([hmi01], [step, scenarioEnd(S, 1000)])).toHaveLength(1);
  });

  it('fires at scenario end when unmet', () => {
    expect(brief(run([hmi01], [acc(1000, 'ACTIVE'), scenarioEnd(S, 1100)]))).toEqual([
      { ruleId: 'HMI-01', scenarioId: S, startTs: 1000, endTs: 1300, detectTs: 1100 },
    ]);
  });
});

describe('HMI-01 out-of-order input', () => {
  it('ignores a late-arriving display timestamped before the change', () => {
    const out = run([hmi01], [acc(1000, 'ACTIVE'), show(900, 'ACC', 'ACTIVE'), tick(1350), scenarioEnd(S, 2000)]);
    expect(out.map((a) => a.evidence['shown'])).toEqual([null]);
  });

  it('keeps the display with the latest ts, not the last to arrive', () => {
    const out = run([hmi01], [acc(1000, 'ACTIVE'), show(1200, 'ACC', 'ACTIVE'), show(1100, 'ACC', 'STANDBY'), scenarioEnd(S, 2000)]);
    expect(out).toEqual([]);
  });
});
