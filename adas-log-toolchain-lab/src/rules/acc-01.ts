import { anomaly, type Anomaly, type Rule, type RuleCtx } from '../engine/types.ts';
import { ev, scenarioEnd, type AccState } from '../schema/events.ts';
import { triplet } from './boundary.ts';

type Cfg = { minGapM: number; timeGapS: number; mergeGapMs: number };
type Episode = { startTs: number; lastHitTs: number; minDistance: number; threshold: number };
type State = { acc: AccState; open: Episode | null };

const meta = { id: 'ACC-01', module: 'ACC', severity: 'P1' } as const;

function close(ctx: RuleCtx<State, Cfg>): Anomaly[] {
  const o = ctx.state.open;
  if (!o) return [];
  ctx.state.open = null;
  return [anomaly(meta, ctx, {
    startTs: o.startTs,
    endTs: o.lastHitTs,
    message: `Following distance ${o.minDistance.toFixed(2)} m below threshold ${o.threshold.toFixed(2)} m`,
    evidence: { minDistance: o.minDistance, threshold: o.threshold },
  })];
}

export const acc01: Rule<State, Cfg> = {
  ...meta,
  title: 'Following distance too short',
  defaults: { minGapM: 5, timeGapS: 1.0, mergeGapMs: 200 },
  createState: () => ({ acc: 'OFF', open: null }),

  onEvent(e, ctx) {
    if (e.module === 'ACC' && e.type === 'STATE') {
      ctx.state.acc = e.data.to;
      return e.data.to === 'ACTIVE' ? [] : close(ctx);
    }
    if (e.module !== 'ACC' || e.type !== 'TRACK') return [];
    const out: Anomaly[] = [];
    const open = ctx.state.open;
    if (open && e.ts - open.lastHitTs > ctx.cfg.mergeGapMs) out.push(...close(ctx));
    const d = e.data.leadDistance;
    if (ctx.state.acc !== 'ACTIVE' || d === null) return out;
    const threshold = Math.max(ctx.cfg.minGapM, e.data.egoSpeed * ctx.cfg.timeGapS);
    if (d >= threshold) return out;
    const cur = ctx.state.open;
    if (cur) {
      // Out-of-order hits may only widen the episode, never shrink it.
      cur.startTs = Math.min(cur.startTs, e.ts);
      cur.lastHitTs = Math.max(cur.lastHitTs, e.ts);
      cur.minDistance = Math.min(cur.minDistance, d);
    } else {
      ctx.state.open = { startTs: e.ts, lastHitTs: e.ts, minDistance: d, threshold };
    }
    return out;
  },

  onScenarioEnd: (ctx) => close(ctx),

  boundaryCases(cfg, eps) {
    return [{ name: 'lowspeed', speed: 3 }, { name: 'highspeed', speed: 20 }].flatMap(({ name, speed }) => {
      const threshold = Math.max(cfg.minGapM, speed * cfg.timeGapS);
      return triplet(`ACC-01-${name}`, 'minus', (sign, id) => {
        const lead = threshold + sign * eps.distance;
        return {
          description: `lead distance ${lead.toFixed(2)} m vs threshold ${threshold.toFixed(2)} m at ${speed} m/s`,
          events: [
            ev('ACC/STATE', 0, id, { from: 'OFF', to: 'ACTIVE', reason: 'boundary' }),
            ev('ACC/TRACK', 1000, id, { egoSpeed: speed, setSpeed: speed, leadDistance: lead, leadRelSpeed: 0 }),
            scenarioEnd(id, 2000),
          ],
        };
      });
    });
  },
};
