import { anomaly, type Anomaly, type Rule, type RuleCtx } from '../engine/types.ts';
import { ev, scenarioEnd } from '../schema/events.ts';
import { triplet } from './boundary.ts';

type Cfg = { torqueNm: number; mergeGapMs: number };
type Episode = { startTs: number; lastHitTs: number; maxTorque: number };
type State = { signalOn: boolean; open: Episode | null };

const meta = { id: 'LKA-01', module: 'LKA', severity: 'P1' } as const;

function close(ctx: RuleCtx<State, Cfg>): Anomaly[] {
  const o = ctx.state.open;
  if (!o) return [];
  ctx.state.open = null;
  return [anomaly(meta, ctx, {
    startTs: o.startTs,
    endTs: o.lastHitTs,
    message: `Steering torque ${o.maxTorque.toFixed(2)} Nm above threshold ${ctx.cfg.torqueNm.toFixed(2)} Nm with turn signal on`,
    evidence: { maxTorque: o.maxTorque, threshold: ctx.cfg.torqueNm },
  })];
}

/** Threshold rule: startTs = first hit, endTs = last hit (like ACC-01); closed by merge gap, signal OFF, or scenario end. */
export const lka01: Rule<State, Cfg> = {
  ...meta,
  title: 'Steering intervention with turn signal on',
  defaults: { torqueNm: 0.5, mergeGapMs: 200 },
  createState: () => ({ signalOn: false, open: null }),

  onEvent(e, ctx) {
    if (e.module === 'VEH' && e.type === 'TURN_SIGNAL') {
      ctx.state.signalOn = e.data.side !== 'OFF';
      return ctx.state.signalOn ? [] : close(ctx);
    }
    if (e.module !== 'LKA' || e.type !== 'STEER') return [];
    const out: Anomaly[] = [];
    const open = ctx.state.open;
    if (open && e.ts - open.lastHitTs > ctx.cfg.mergeGapMs) out.push(...close(ctx));
    const torque = Math.abs(e.data.torque);
    if (!ctx.state.signalOn || torque <= ctx.cfg.torqueNm) return out;
    const cur = ctx.state.open;
    if (cur) {
      // Out-of-order hits may only widen the episode, never shrink it.
      cur.startTs = Math.min(cur.startTs, e.ts);
      cur.lastHitTs = Math.max(cur.lastHitTs, e.ts);
      cur.maxTorque = Math.max(cur.maxTorque, torque);
    } else {
      ctx.state.open = { startTs: e.ts, lastHitTs: e.ts, maxTorque: torque };
    }
    return out;
  },

  onScenarioEnd: (ctx) => close(ctx),

  boundaryCases(cfg, eps) {
    return triplet('LKA-01-torque', 'plus', (sign, id) => {
      const torque = cfg.torqueNm + sign * eps.torque;
      return {
        description: `steering torque ${torque.toFixed(2)} Nm with turn signal LEFT (threshold ${cfg.torqueNm.toFixed(2)} Nm)`,
        events: [
          ev('VEH/TURN_SIGNAL', 0, id, { side: 'LEFT' }),
          ev('LKA/STEER', 1000, id, { torque }),
          scenarioEnd(id, 2000),
        ],
      };
    });
  },
};
