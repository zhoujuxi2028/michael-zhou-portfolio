import { anomaly, type Anomaly, type Rule, type RuleCtx } from '../engine/types.ts';
import { ev, scenarioEnd, type AccState } from '../schema/events.ts';
import { triplet } from './boundary.ts';

type Cfg = { exitWindowMs: number };
type Pending = { startTs: number; deadline: number };
type State = { acc: AccState; pending: Pending | null };

const meta = { id: 'ACC-02', module: 'ACC', severity: 'P1' } as const;

const report = (ctx: RuleCtx<State, Cfg>, p: Pending): Anomaly =>
  anomaly(meta, ctx, {
    startTs: p.startTs,
    endTs: p.deadline,
    message: `ACC did not exit within ${ctx.cfg.exitWindowMs} ms after brake pedal`,
  });

export const acc02: Rule<State, Cfg> = {
  ...meta,
  title: 'No exit after brake pedal',
  defaults: { exitWindowMs: 200 },
  createState: () => ({ acc: 'OFF', pending: null }),

  onEvent(e, ctx) {
    const out: Anomaly[] = [];
    const s = ctx.state;
    if (s.pending && ctx.now > s.pending.deadline) {
      out.push(report(ctx, s.pending));
      s.pending = null;
    }
    if (e.module !== 'ACC') return out;
    if (e.type === 'STATE') {
      s.acc = e.data.to;
      if (e.data.to === 'STANDBY' || e.data.to === 'OFF') s.pending = null;
    } else if (e.type === 'BRAKE_PEDAL' && e.data.pressed && s.acc === 'ACTIVE' && !s.pending) {
      s.pending = { startTs: e.ts, deadline: e.ts + ctx.cfg.exitWindowMs };
    }
    return out;
  },

  onScenarioEnd(ctx) {
    const p = ctx.state.pending;
    ctx.state.pending = null;
    return p ? [report(ctx, p)] : [];
  },

  boundaryCases(cfg, eps) {
    return triplet('ACC-02-window', 'plus', (sign, id) => {
      const exitTs = 1000 + cfg.exitWindowMs + sign * eps.time;
      return {
        description: `ACC exits ${exitTs - 1000} ms after brake pedal (window ${cfg.exitWindowMs} ms)`,
        events: [
          ev('ACC/STATE', 0, id, { from: 'OFF', to: 'ACTIVE', reason: 'boundary' }),
          ev('ACC/BRAKE_PEDAL', 1000, id, { pressed: true }),
          ev('ACC/STATE', exitTs, id, { from: 'ACTIVE', to: 'STANDBY', reason: 'brake' }),
          scenarioEnd(id, 3000),
        ],
      };
    });
  },
};
