import { anomaly, type Anomaly, type Rule, type RuleCtx } from '../engine/types.ts';
import { ev, scenarioEnd, type AdasEvent } from '../schema/events.ts';
import { triplet } from './boundary.ts';

type Cfg = { confThreshold: number; persistMs: number; displayWindowMs: number };
type Loss = { startTs: number; minConf: number; open: boolean; confirmed: boolean; shownTs: number | null };
type State = { losses: Loss[] };

const meta = { id: 'LKA-02', module: 'LKA', severity: 'P2' } as const;

/** Confidence boundary step (confidence is unitless, not covered by Epsilon). */
const CONF_EPS = 0.01;

const deadline = (l: Loss, cfg: Cfg) => l.startTs + cfg.persistMs + cfg.displayWindowMs;

function judge(ctx: RuleCtx<State, Cfg>, l: Loss): Anomaly[] {
  const end = deadline(l, ctx.cfg);
  if (l.shownTs !== null && l.shownTs <= end) return [];
  return [anomaly(meta, ctx, {
    startTs: l.startTs,
    endTs: end,
    message: `Lane loss (confidence ${l.minConf.toFixed(2)}) not indicated within ${ctx.cfg.displayWindowMs} ms after ${ctx.cfg.persistMs} ms`,
    evidence: { minConf: l.minConf, threshold: ctx.cfg.confThreshold },
  })];
}

/**
 * Time points (window rule, like HMI-01 / ACC-02):
 * - loss start L = ts of the first sample with min(leftConf, rightConf) < threshold;
 * - persisted once a low sample (without recovery in between) has ts - L > persistMs (strict);
 * - display deadline D = L + persistMs + displayWindowMs; an HMI DISPLAY(LKA, UNAVAILABLE) with L <= ts <= D is on time;
 * - anomaly: startTs = L, endTs = D; judged when time passes D (ts > D) or at scenario end.
 * A recovery sample before persistence discards the loss; after persistence it only closes it (still judged).
 */
export const lka02: Rule<State, Cfg> = {
  ...meta,
  title: 'Lane loss not indicated',
  defaults: { confThreshold: 0.5, persistMs: 500, displayWindowMs: 300 },
  createState: () => ({ losses: [] }),

  onEvent(e, ctx) {
    const out: Anomaly[] = [];
    const s = ctx.state;
    s.losses = s.losses.filter((l) => {
      if (!l.confirmed || ctx.now <= deadline(l, ctx.cfg)) return true;
      out.push(...judge(ctx, l));
      return false;
    });
    if (e.module === 'HMI' && e.type === 'DISPLAY') {
      if (e.data.feature === 'LKA' && e.data.item === 'UNAVAILABLE') {
        for (const l of s.losses) {
          if (e.ts >= l.startTs) l.shownTs = Math.min(l.shownTs ?? e.ts, e.ts);
        }
      }
    } else if (e.module === 'LKA' && e.type === 'LANE') {
      onLane(e, ctx);
    }
    return out;
  },

  onScenarioEnd(ctx) {
    const out = ctx.state.losses.filter((l) => l.confirmed).flatMap((l) => judge(ctx, l));
    ctx.state.losses = [];
    return out;
  },

  boundaryCases(cfg, eps) {
    const samples = (id: string, from: number, to: number, left: number): AdasEvent[] => {
      const out: AdasEvent[] = [];
      for (let t = from; t <= to; t += 100) out.push(ev('LKA/LANE', t, id, { leftConf: left, rightConf: 0.9, offset: 0 }));
      return out;
    };
    const lossStart = 1000;
    const longLoss = lossStart + cfg.persistMs + cfg.displayWindowMs + 500;
    const conf = triplet('LKA-02-conf', 'minus', (sign, id) => {
      const left = cfg.confThreshold + sign * CONF_EPS;
      return {
        description: `lane confidence ${left.toFixed(2)} for ${longLoss - lossStart} ms without display (threshold ${cfg.confThreshold.toFixed(2)})`,
        events: [...samples(id, lossStart, longLoss, left), scenarioEnd(id, longLoss + 1000)],
      };
    });
    const persist = triplet('LKA-02-persist', 'plus', (sign, id) => {
      const last = lossStart + cfg.persistMs + sign * eps.time;
      return {
        description: `lane confidence low for ${last - lossStart} ms without display (persistence ${cfg.persistMs} ms)`,
        events: [
          ...samples(id, lossStart, last - 1, 0.2),
          ev('LKA/LANE', last, id, { leftConf: 0.2, rightConf: 0.9, offset: 0 }),
          ev('LKA/LANE', last + 100, id, { leftConf: 0.9, rightConf: 0.9, offset: 0 }),
          scenarioEnd(id, longLoss + 1000),
        ],
      };
    });
    const window = triplet('LKA-02-window', 'plus', (sign, id) => {
      const showTs = lossStart + cfg.persistMs + cfg.displayWindowMs + sign * eps.time;
      return {
        description: `UNAVAILABLE displayed ${showTs - lossStart} ms after loss start (deadline ${cfg.persistMs + cfg.displayWindowMs} ms)`,
        events: [
          ...samples(id, lossStart, longLoss, 0.2),
          ev('HMI/DISPLAY', showTs, id, { feature: 'LKA', item: 'UNAVAILABLE', value: 'SHOWN' }),
          scenarioEnd(id, longLoss + 1000),
        ].sort((a, b) => a.ts - b.ts),
      };
    });
    return [...conf, ...persist, ...window];
  },
};

function onLane(e: Extract<AdasEvent, { module: 'LKA'; type: 'LANE' }>, ctx: RuleCtx<State, Cfg>): void {
  const conf = Math.min(e.data.leftConf, e.data.rightConf);
  const cur = ctx.state.losses.find((l) => l.open);
  if (conf >= ctx.cfg.confThreshold) {
    if (cur) {
      cur.open = false;
      if (!cur.confirmed) ctx.state.losses = ctx.state.losses.filter((l) => l !== cur);
    }
    return;
  }
  const loss = cur ?? { startTs: e.ts, minConf: conf, open: true, confirmed: false, shownTs: null };
  if (!cur) ctx.state.losses.push(loss);
  loss.startTs = Math.min(loss.startTs, e.ts);
  loss.minConf = Math.min(loss.minConf, conf);
  if (e.ts - loss.startTs > ctx.cfg.persistMs) loss.confirmed = true;
}
