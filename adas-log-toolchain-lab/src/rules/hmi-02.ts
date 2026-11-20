import { anomaly, type Anomaly, type Rule, type RuleCtx } from '../engine/types.ts';
import { ev, scenarioEnd } from '../schema/events.ts';
import { triplet } from './boundary.ts';

type Cfg = { takeoverWindowMs: number };
type Pending = { startTs: number; deadline: number };
type State = { pending: Pending | null };

const meta = { id: 'HMI-02', module: 'HMI', severity: 'P1' } as const;

const report = (ctx: RuleCtx<State, Cfg>, p: Pending): Anomaly =>
  anomaly(meta, ctx, {
    startTs: p.startTs,
    endTs: p.deadline,
    message: `Takeover request not shown on HMI within ${ctx.cfg.takeoverWindowMs} ms`,
  });

export const hmi02: Rule<State, Cfg> = {
  ...meta,
  title: 'Takeover request not shown',
  defaults: { takeoverWindowMs: 200 },
  createState: () => ({ pending: null }),

  onEvent(e, ctx) {
    const out: Anomaly[] = [];
    const s = ctx.state;
    if (s.pending && ctx.now > s.pending.deadline) {
      out.push(report(ctx, s.pending));
      s.pending = null;
    }
    if (e.module === 'ACC' && e.type === 'TAKEOVER_REQUEST') {
      s.pending ??= { startTs: e.ts, deadline: e.ts + ctx.cfg.takeoverWindowMs };
    } else if (e.module === 'HMI' && e.type === 'DISPLAY' && e.data.item === 'TAKEOVER' && s.pending && e.ts >= s.pending.startTs && e.ts <= s.pending.deadline) {
      s.pending = null;
    }
    return out;
  },

  onScenarioEnd(ctx) {
    const p = ctx.state.pending;
    ctx.state.pending = null;
    return p ? [report(ctx, p)] : [];
  },

  boundaryCases(cfg, eps) {
    return triplet('HMI-02-window', 'plus', (sign, id) => {
      const showTs = 1000 + cfg.takeoverWindowMs + sign * eps.time;
      return {
        description: `takeover shown ${showTs - 1000} ms after request (window ${cfg.takeoverWindowMs} ms)`,
        events: [
          ev('ACC/TAKEOVER_REQUEST', 1000, id, { reason: 'boundary' }),
          ev('HMI/DISPLAY', showTs, id, { feature: 'ACC', item: 'TAKEOVER', value: 'SHOWN' }),
          scenarioEnd(id, 3000),
        ],
      };
    });
  },
};
