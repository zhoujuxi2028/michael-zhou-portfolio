// tests/rules/hmi-02.test.ts
import { describe, expect, it } from 'vitest';
import { hmi02 } from '../../src/rules/hmi-02.ts';
import { ev, scenarioEnd } from '../../src/schema/events.ts';
import { brief, run } from '../helpers.ts';

const S = 'S1';
const req = (ts: number) => ev('ACC/TAKEOVER_REQUEST', ts, S, { reason: 't' });
const shown = (ts: number) => ev('HMI/DISPLAY', ts, S, { feature: 'ACC', item: 'TAKEOVER', value: 'SHOWN' });
const tick = (ts: number) => ev('ACC/BRAKE_PEDAL', ts, S, { pressed: false });

describe('HMI-02 takeover request not shown', () => {
  it('passes when shown in time (exactly at the deadline is on time)', () => {
    expect(run([hmi02], [req(1000), shown(1200), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('fires when shown too late', () => {
    const out = run([hmi02], [req(1000), tick(1250), shown(1450), scenarioEnd(S, 2000)]);
    expect(brief(out)).toEqual([{ ruleId: 'HMI-02', scenarioId: S, startTs: 1000, endTs: 1200, detectTs: 1250 }]);
    expect(out[0]?.message).toBe('Takeover request not shown on HMI within 200 ms');
  });

  it('ignores state displays and keeps the first pending request', () => {
    const state = ev('HMI/DISPLAY', 1100, S, { feature: 'ACC', item: 'STATE', value: 'ACTIVE' });
    expect(run([hmi02], [req(1000), req(1050), state, scenarioEnd(S, 2000)]).map((a) => a.startTs)).toEqual([1000]);
  });

  it('fires at scenario end if still pending', () => {
    expect(run([hmi02], [req(1000), scenarioEnd(S, 1100)])).toHaveLength(1);
  });
});

describe('HMI-02 out-of-order input', () => {
  it('ignores a late-arriving display timestamped before the request', () => {
    expect(run([hmi02], [req(1000), shown(900), scenarioEnd(S, 2000)])).toHaveLength(1);
  });
});
