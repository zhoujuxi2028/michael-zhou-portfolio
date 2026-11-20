import { describe, expect, it } from 'vitest';
import { createEngine } from '../../src/engine/engine.ts';
import { anomaly, type Rule } from '../../src/engine/types.ts';
import { ev, scenarioEnd } from '../../src/schema/events.ts';
import { triplet } from '../../src/rules/boundary.ts';

type ProbeState = { seen: number; nows: number[] };
type ProbeCfg = { limit: number };

/** Emits one anomaly per scenario at end: evidence = events seen and observed `now` values. */
const probe: Rule<ProbeState, ProbeCfg> = {
  id: 'PROBE', module: 'SYS', severity: 'P2', title: 'probe',
  defaults: { limit: 1 },
  createState: () => ({ seen: 0, nows: [] }),
  onEvent(_e, ctx) {
    ctx.state.seen++;
    ctx.state.nows.push(ctx.now);
    return [];
  },
  onScenarioEnd(ctx) {
    return [anomaly(this, ctx, { startTs: 0, endTs: ctx.now, message: 'end', evidence: { seen: ctx.state.seen, nows: ctx.state.nows, limit: ctx.cfg.limit } })];
  },
  boundaryCases: () => [],
};

const brake = (ts: number, id: string) => ev('ACC/BRAKE_PEDAL', ts, id, { pressed: false });

describe('createEngine', () => {
  it('isolates state per scenario and releases it on SCENARIO_END', () => {
    const engine = createEngine([probe]);
    engine.push(brake(0, 'A'));
    engine.push(brake(5, 'B'));
    engine.push(brake(10, 'A'));
    expect(engine.activeScenarios()).toBe(2);
    const out = engine.push(scenarioEnd('A', 20));
    expect(out).toHaveLength(1);
    expect(out[0]).toMatchObject({ scenarioId: 'A', detectTs: 20, evidence: { seen: 3 } });
    expect(engine.activeScenarios()).toBe(1);
  });

  it('flushes unfinished scenarios at EOF', () => {
    const engine = createEngine([probe]);
    engine.push(brake(0, 'A'));
    const out = engine.end();
    expect(out.map((a) => a.scenarioId)).toEqual(['A']);
    expect(engine.activeScenarios()).toBe(0);
  });

  it('never moves scenario time backwards on out-of-order events', () => {
    const engine = createEngine([probe]);
    engine.push(brake(100, 'A'));
    engine.push(brake(50, 'A'));
    const [a] = engine.end();
    expect(a?.evidence['nows']).toEqual([100, 100]);
  });

  it('merges rule config overrides over defaults', () => {
    const engine = createEngine([probe], { rules: { PROBE: { limit: 9 } } });
    engine.push(brake(0, 'A'));
    expect(engine.end()[0]?.evidence['limit']).toBe(9);
  });
});

describe('triplet', () => {
  it('builds minus / eq / plus cases and marks the trigger side', () => {
    const cases = triplet('X-01-dist', 'minus', (sign, caseId) => ({ description: `${sign}`, events: [scenarioEnd(caseId, 0)] }));
    expect(cases.map((c) => [c.caseId, c.expectTriggered, c.description])).toEqual([
      ['X-01-dist-minus', true, '-1'],
      ['X-01-dist-eq', false, '0'],
      ['X-01-dist-plus', false, '1'],
    ]);
  });
});
