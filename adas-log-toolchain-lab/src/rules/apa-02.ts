import { anomaly, type Anomaly, type Rule, type RuleCtx } from '../engine/types.ts';
import { ev, scenarioEnd } from '../schema/events.ts';
import { triplet } from './boundary.ts';

type Cfg = { timeoutMs: number };
type Maneuver = { startTs: number; deadline: number; reported: boolean };
type State = { maneuver: Maneuver | null };

const meta = { id: 'APA-02', module: 'APA', severity: 'P2' } as const;

const timeout = (ctx: RuleCtx<State, Cfg>, m: Maneuver): Anomaly =>
  anomaly(meta, ctx, { startTs: m.startTs, endTs: m.deadline, message: `Parking maneuver exceeded ${ctx.cfg.timeoutMs} ms` });

export const apa02: Rule<State, Cfg> = {
  ...meta,
  title: 'Parking timeout',
  defaults: { timeoutMs: 60000 },
  createState: () => ({ maneuver: null }),

  onEvent(e, ctx) {
    const out: Anomaly[] = [];
    const m = ctx.state.maneuver;
    if (m && !m.reported && ctx.now > m.deadline) {
      out.push(timeout(ctx, m));
      m.reported = true;
    }
    if (e.module === 'APA' && e.type === 'STEP') {
      if (e.data.step === 'MANEUVERING') {
        ctx.state.maneuver ??= { startTs: e.ts, deadline: e.ts + ctx.cfg.timeoutMs, reported: false };
      } else if (e.data.step === 'COMPLETED' || e.data.step === 'ABORTED') {
        ctx.state.maneuver = null;
      }
    }
    return out;
  },

  onScenarioEnd(ctx) {
    const m = ctx.state.maneuver;
    ctx.state.maneuver = null;
    if (!m || m.reported) return [];
    if (ctx.now > m.deadline) return [timeout(ctx, m)];
    return [anomaly(meta, ctx, { startTs: m.startTs, endTs: ctx.now, message: 'Parking maneuver unfinished at scenario end' })];
  },

  boundaryCases(cfg, eps) {
    const window = triplet('APA-02-timeout', 'plus', (sign, id) => {
      const doneTs = cfg.timeoutMs + sign * eps.time;
      return {
        description: `maneuver completes after ${doneTs} ms (timeout ${cfg.timeoutMs} ms)`,
        events: [
          ev('APA/STEP', 0, id, { step: 'MANEUVERING', reason: 'boundary' }),
          ev('APA/STEP', doneTs, id, { step: 'COMPLETED', reason: 'parked' }),
          scenarioEnd(id, doneTs + 1000),
        ],
      };
    });
    const id = 'APA-02-unfinished';
    return [...window, {
      caseId: id,
      description: 'scenario ends while still maneuvering',
      expectTriggered: true,
      events: [ev('APA/STEP', 0, id, { step: 'MANEUVERING', reason: 'boundary' }), scenarioEnd(id, 10000)],
    }];
  },
};
