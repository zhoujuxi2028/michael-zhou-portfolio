import { anomaly, type Anomaly, type Rule, type RuleCtx } from '../engine/types.ts';
import { ev, scenarioEnd, type AdasEvent, type HmiFeature } from '../schema/events.ts';
import { triplet } from './boundary.ts';

type Cfg = { displayWindowMs: number };
type Pending = { expected: string; startTs: number; deadline: number; lastShown: string | null; lastShownTs: number };
type State = { pending: Map<HmiFeature, Pending> };

const meta = { id: 'HMI-01', module: 'HMI', severity: 'P2' } as const;

function judge(ctx: RuleCtx<State, Cfg>, feature: HmiFeature, p: Pending): Anomaly[] {
  if (p.lastShown === p.expected) return [];
  return [anomaly(meta, ctx, {
    startTs: p.startTs,
    endTs: p.deadline,
    message: p.lastShown === null
      ? `HMI did not show ${feature} state ${p.expected} within ${ctx.cfg.displayWindowMs} ms`
      : `HMI showed ${feature} state ${p.lastShown}, expected ${p.expected}`,
    evidence: { feature, expected: p.expected, shown: p.lastShown },
  })];
}

function stateChange(e: AdasEvent): { feature: HmiFeature; value: string } | null {
  if (e.module === 'ACC' && e.type === 'STATE') return { feature: 'ACC', value: e.data.to };
  if (e.module === 'APA' && e.type === 'STEP') return { feature: 'APA', value: e.data.step };
  if (e.module === 'LKA' && e.type === 'STATE') return { feature: 'LKA', value: e.data.to };
  return null;
}

export const hmi01: Rule<State, Cfg> = {
  ...meta,
  title: 'HMI state mismatch',
  defaults: { displayWindowMs: 300 },
  createState: () => ({ pending: new Map() }),

  onEvent(e, ctx) {
    const out: Anomaly[] = [];
    const pending = ctx.state.pending;
    for (const [feature, p] of pending) {
      if (ctx.now > p.deadline) {
        out.push(...judge(ctx, feature, p));
        pending.delete(feature);
      }
    }
    const change = stateChange(e);
    if (change) {
      pending.set(change.feature, { expected: change.value, startTs: e.ts, deadline: e.ts + ctx.cfg.displayWindowMs, lastShown: null, lastShownTs: -1 });
    } else if (e.module === 'HMI' && e.type === 'DISPLAY' && e.data.item === 'STATE') {
      const p = pending.get(e.data.feature);
      // Only displays inside [startTs, deadline] count; the one with the latest ts wins.
      if (p && e.ts >= p.startTs && e.ts <= p.deadline && e.ts >= p.lastShownTs) {
        p.lastShown = e.data.value;
        p.lastShownTs = e.ts;
      }
    }
    return out;
  },

  onScenarioEnd(ctx) {
    const out = [...ctx.state.pending].flatMap(([feature, p]) => judge(ctx, feature, p));
    ctx.state.pending.clear();
    return out;
  },

  boundaryCases(cfg, eps) {
    const window = triplet('HMI-01-window', 'plus', (sign, id) => {
      const showTs = 1000 + cfg.displayWindowMs + sign * eps.time;
      return {
        description: `HMI shows ACC ACTIVE ${showTs - 1000} ms after change (window ${cfg.displayWindowMs} ms)`,
        events: [
          ev('ACC/STATE', 1000, id, { from: 'OFF', to: 'ACTIVE', reason: 'boundary' }),
          ev('HMI/DISPLAY', showTs, id, { feature: 'ACC', item: 'STATE', value: 'ACTIVE' }),
          scenarioEnd(id, 3000),
        ],
      };
    });
    const id = 'HMI-01-wrong-value';
    return [...window, {
      caseId: id,
      description: 'HMI shows STANDBY while ACC is ACTIVE',
      expectTriggered: true,
      events: [
        ev('ACC/STATE', 1000, id, { from: 'OFF', to: 'ACTIVE', reason: 'boundary' }),
        ev('HMI/DISPLAY', 1100, id, { feature: 'ACC', item: 'STATE', value: 'STANDBY' }),
        scenarioEnd(id, 3000),
      ],
    }];
  },
};
