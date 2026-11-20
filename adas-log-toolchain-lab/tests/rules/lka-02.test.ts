import { describe, expect, it } from 'vitest';
import { lka02 } from '../../src/rules/lka-02.ts';
import { ev, scenarioEnd, type AdasEvent } from '../../src/schema/events.ts';
import { brief, run } from '../helpers.ts';

const S = 'S1';
const lane = (ts: number, leftConf: number, rightConf = 0.9) => ev('LKA/LANE', ts, S, { leftConf, rightConf, offset: 0 });
const unavailable = (ts: number) => ev('HMI/DISPLAY', ts, S, { feature: 'LKA', item: 'UNAVAILABLE', value: 'SHOWN' });
/** Low-confidence samples every 100 ms from `from` to `to` inclusive. */
const lowRun = (from: number, to: number, conf = 0.2): AdasEvent[] => {
  const out: AdasEvent[] = [];
  for (let t = from; t <= to; t += 100) out.push(lane(t, conf));
  return out;
};

// Timing: loss start L = first low sample; persisted once a low sample has ts > L + 500;
// display deadline D = L + 500 + 300. Anomaly: startTs = L, endTs = D.
describe('LKA-02 lane loss not indicated', () => {
  it('fires after persistent loss without a display; startTs = loss start, endTs = display deadline', () => {
    const out = run([lka02], [...lowRun(1000, 1700), lane(1800, 0.9), scenarioEnd(S, 5000)]);
    expect(brief(out)).toEqual([{ ruleId: 'LKA-02', scenarioId: S, startTs: 1000, endTs: 1800, detectTs: 5000 }]);
    expect(out[0]?.evidence).toEqual({ minConf: 0.2, threshold: 0.5 });
  });

  it('is judged when time passes the deadline, not only at scenario end', () => {
    const out = run([lka02], [...lowRun(1000, 1700), lane(1900, 0.9), scenarioEnd(S, 5000)]);
    expect(brief(out)).toEqual([{ ruleId: 'LKA-02', scenarioId: S, startTs: 1000, endTs: 1800, detectTs: 1900 }]);
  });

  it('does not fire when the loss lasts exactly 500 ms or less', () => {
    expect(run([lka02], [...lowRun(1000, 1500), lane(1600, 0.9), scenarioEnd(S, 5000)])).toEqual([]);
    expect(run([lka02], [...lowRun(1000, 1500), scenarioEnd(S, 5000)])).toEqual([]);
  });

  it('fires when the loss is 1 ms longer than 500 ms', () => {
    expect(run([lka02], [lane(1000, 0.2), lane(1501, 0.2), scenarioEnd(S, 5000)])).toHaveLength(1);
  });

  it('uses confidence < 0.5 strictly on the smaller of left / right', () => {
    const at = (l: number, r: number) => run([lka02], [lane(1000, l, r), lane(1600, l, r), scenarioEnd(S, 5000)]).length;
    expect([at(0.5, 0.5), at(0.49, 0.9), at(0.9, 0.49), at(0.9, 0.9)]).toEqual([0, 1, 1, 0]);
  });

  it('resets when confidence recovers, restarting the persistence clock', () => {
    const events = [lane(1000, 0.2), lane(1400, 0.2), lane(1450, 0.9), lane(1600, 0.2), lane(1900, 0.2), scenarioEnd(S, 5000)];
    expect(run([lka02], events)).toEqual([]);
  });

  it('accepts an UNAVAILABLE display up to and including the deadline', () => {
    const loss = lowRun(1000, 1700);
    expect(run([lka02], [...loss, unavailable(1800), lane(1900, 0.9), scenarioEnd(S, 5000)])).toEqual([]);
    expect(run([lka02], [...loss, unavailable(1801), lane(1900, 0.9), scenarioEnd(S, 5000)])).toHaveLength(1);
  });

  it('accepts an early display shown during the loss, but not one from before it', () => {
    const loss = lowRun(1000, 1700);
    expect(run([lka02], [unavailable(1100), ...loss, lane(1800, 0.9), scenarioEnd(S, 5000)].sort((a, b) => a.ts - b.ts))).toEqual([]);
    expect(run([lka02], [unavailable(900), ...loss, lane(1800, 0.9), scenarioEnd(S, 5000)])).toHaveLength(1);
    expect(run([lka02], [...loss, unavailable(900), lane(1800, 0.9), scenarioEnd(S, 5000)])).toHaveLength(1);
  });

  it('keeps the earliest of several displays, whatever the arrival order', () => {
    const loss = lowRun(1000, 1700);
    expect(run([lka02], [...loss, unavailable(1790), unavailable(1100), lane(1800, 0.9), scenarioEnd(S, 5000)])).toEqual([]);
    expect(run([lka02], [...loss, unavailable(1100), unavailable(1790), lane(1800, 0.9), scenarioEnd(S, 5000)])).toEqual([]);
  });

  it('ignores displays for other features or items', () => {
    const other = [
      ev('HMI/DISPLAY', 1700, S, { feature: 'ACC', item: 'UNAVAILABLE', value: 'SHOWN' }),
      ev('HMI/DISPLAY', 1710, S, { feature: 'LKA', item: 'STATE', value: 'ACTIVE' }),
    ];
    expect(run([lka02], [...lowRun(1000, 1600), ...other, lane(1800, 0.9), scenarioEnd(S, 5000)])).toHaveLength(1);
  });

  it('reports a pending judgment at scenario end', () => {
    const out = run([lka02], [...lowRun(1000, 1700), scenarioEnd(S, 1750)]);
    expect(brief(out)).toEqual([{ ruleId: 'LKA-02', scenarioId: S, startTs: 1000, endTs: 1800, detectTs: 1750 }]);
  });

  it('judges two separate losses independently', () => {
    const events = [...lowRun(1000, 1700), lane(1800, 0.9), unavailable(1810), ...lowRun(5000, 5700), lane(5800, 0.9), scenarioEnd(S, 9000)];
    expect(run([lka02], events).map((a) => [a.startTs, a.endTs])).toEqual([[1000, 1800], [5000, 5800]]);
  });

  it('keeps a pending judgment when a new loss starts before its deadline', () => {
    const events = [...lowRun(1000, 1700), lane(1750, 0.9), ...lowRun(1760, 2500), scenarioEnd(S, 9000)];
    expect(run([lka02], events).map((a) => [a.startTs, a.endTs])).toEqual([[1000, 1800], [1760, 2560]]);
  });

  it('honours config overrides', () => {
    const events = [lane(1000, 0.6), lane(1300, 0.6), scenarioEnd(S, 5000)];
    expect(run([lka02], events, { rules: { 'LKA-02': { confThreshold: 0.7, persistMs: 200 } } })).toHaveLength(1);
  });
});
