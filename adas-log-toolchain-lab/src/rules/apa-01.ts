import { anomaly, type Anomaly, type Rule, type RuleCtx } from '../engine/types.ts';
import { ev, scenarioEnd, type ApaStep } from '../schema/events.ts';
import { triplet } from './boundary.ts';

type Cfg = { minDistanceM: number; abortWindowMs: number };
type Pending = { startTs: number; deadline: number; minDistance: number };
type State = { step: ApaStep | null; inEpisode: boolean; pending: Pending | null };

const meta = { id: 'APA-01', module: 'APA', severity: 'P1' } as const;

const report = (ctx: RuleCtx<State, Cfg>, p: Pending): Anomaly =>
  anomaly(meta, ctx, {
    startTs: p.startTs,
    endTs: p.deadline,
    message: `Obstacle at ${p.minDistance.toFixed(2)} m below ${ctx.cfg.minDistanceM} m without abort within ${ctx.cfg.abortWindowMs} ms`,
    evidence: { minDistance: p.minDistance },
  });

export const apa01: Rule<State, Cfg> = {
  ...meta,
  title: 'Obstacle too close without abort',
  defaults: { minDistanceM: 0.3, abortWindowMs: 200 },
  createState: () => ({ step: null, inEpisode: false, pending: null }),

  onEvent(e, ctx) {
    const out: Anomaly[] = [];
    const s = ctx.state;
    if (s.pending && ctx.now > s.pending.deadline) {
      out.push(report(ctx, s.pending));
      s.pending = null;
    }
    if (e.module !== 'APA') return out;
    if (e.type === 'STEP') {
      s.step = e.data.step;
      if (e.data.step === 'ABORTED') s.pending = null;
      if (e.data.step !== 'MANEUVERING') s.inEpisode = false;
      return out;
    }
    if (s.step !== 'MANEUVERING') return out;
    const d = e.data.minDistance;
    if (d >= ctx.cfg.minDistanceM) {
      s.inEpisode = false;
      return out;
    }
    if (!s.inEpisode) {
      s.inEpisode = true;
      s.pending ??= { startTs: e.ts, deadline: e.ts + ctx.cfg.abortWindowMs, minDistance: d };
    }
    // pending is null when this episode already expired and was reported.
    if (s.pending) s.pending.minDistance = Math.min(s.pending.minDistance, d);
    return out;
  },

  onScenarioEnd(ctx) {
    const p = ctx.state.pending;
    ctx.state.pending = null;
    return p ? [report(ctx, p)] : [];
  },

  boundaryCases(cfg, eps) {
    const distance = triplet('APA-01-distance', 'minus', (sign, id) => {
      const d = cfg.minDistanceM + sign * eps.distance;
      return {
        description: `obstacle at ${d.toFixed(2)} m vs ${cfg.minDistanceM} m, never aborted`,
        events: [
          ev('APA/STEP', 0, id, { step: 'MANEUVERING', reason: 'boundary' }),
          ev('HMI/DISPLAY', 50, id, { feature: 'APA', item: 'STATE', value: 'MANEUVERING' }),
          ev('APA/OBSTACLE', 1000, id, { minDistance: d }),
          ev('APA/OBSTACLE', 1100, id, { minDistance: 0.8 }),
          ev('APA/STEP', 5000, id, { step: 'COMPLETED', reason: 'parked' }),
          ev('HMI/DISPLAY', 5050, id, { feature: 'APA', item: 'STATE', value: 'COMPLETED' }),
          scenarioEnd(id, 6000),
        ],
      };
    });
    const window = triplet('APA-01-window', 'plus', (sign, id) => {
      const abortTs = 1000 + cfg.abortWindowMs + sign * eps.time;
      return {
        description: `abort ${abortTs - 1000} ms after obstacle (window ${cfg.abortWindowMs} ms)`,
        events: [
          ev('APA/STEP', 0, id, { step: 'MANEUVERING', reason: 'boundary' }),
          ev('HMI/DISPLAY', 50, id, { feature: 'APA', item: 'STATE', value: 'MANEUVERING' }),
          ev('APA/OBSTACLE', 1000, id, { minDistance: 0.2 }),
          ev('APA/STEP', abortTs, id, { step: 'ABORTED', reason: 'obstacle' }),
          ev('HMI/DISPLAY', abortTs + 50, id, { feature: 'APA', item: 'STATE', value: 'ABORTED' }),
          scenarioEnd(id, 3000),
        ],
      };
    });
    return [...distance, ...window];
  },
};
