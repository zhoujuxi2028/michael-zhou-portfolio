import { describe, expect, it } from 'vitest';
import { acc02 } from '../../src/rules/acc-02.ts';
import { ev, scenarioEnd } from '../../src/schema/events.ts';
import { brief, run } from '../helpers.ts';

const S = 'S1';
const state = (ts: number, from: 'OFF' | 'STANDBY' | 'ACTIVE', to: 'OFF' | 'STANDBY' | 'ACTIVE') =>
  ev('ACC/STATE', ts, S, { from, to, reason: 't' });
const brake = (ts: number, pressed = true) => ev('ACC/BRAKE_PEDAL', ts, S, { pressed });
const tick = (ts: number) => ev('ACC/TRACK', ts, S, { egoSpeed: 10, setSpeed: 10, leadDistance: null, leadRelSpeed: 0 });

describe('ACC-02 no exit after brake pedal', () => {
  it('does not fire when ACC exits in time', () => {
    expect(run([acc02], [state(0, 'OFF', 'ACTIVE'), brake(1000), state(1100, 'ACTIVE', 'STANDBY'), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('fires once the deadline passes without exit', () => {
    const out = run([acc02], [state(0, 'OFF', 'ACTIVE'), brake(1000), tick(1200), tick(1250), state(1500, 'ACTIVE', 'STANDBY'), scenarioEnd(S, 2000)]);
    expect(brief(out)).toEqual([{ ruleId: 'ACC-02', scenarioId: S, startTs: 1000, endTs: 1200, detectTs: 1250 }]);
    expect(out[0]?.message).toBe('ACC did not exit within 200 ms after brake pedal');
  });

  it('treats an exit exactly at the deadline as on time', () => {
    expect(run([acc02], [state(0, 'OFF', 'ACTIVE'), brake(1000), state(1200, 'ACTIVE', 'OFF'), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('ignores brake release and brakes while not ACTIVE', () => {
    expect(run([acc02], [state(0, 'OFF', 'ACTIVE'), brake(1000, false), scenarioEnd(S, 2000)])).toEqual([]);
    expect(run([acc02], [state(0, 'OFF', 'STANDBY'), brake(1000), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('fires at scenario end if still pending', () => {
    expect(brief(run([acc02], [state(0, 'OFF', 'ACTIVE'), brake(1000), scenarioEnd(S, 1100)]))).toEqual([
      { ruleId: 'ACC-02', scenarioId: S, startTs: 1000, endTs: 1200, detectTs: 1100 },
    ]);
  });
});
