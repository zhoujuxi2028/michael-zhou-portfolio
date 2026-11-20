import type { AdasEvent } from '../schema/events.ts';
import { DEFAULT_EPSILON, type Anomaly, type AnyRule, type Epsilon } from './types.ts';

export interface EngineConfig {
  rules?: Record<string, Record<string, number>>;
  epsilon?: Partial<Epsilon>;
}

export function resolveRuleConfig(rule: AnyRule, config: EngineConfig): Record<string, number> {
  return { ...rule.defaults, ...(config.rules?.[rule.id] ?? {}) };
}

export function resolveEpsilon(config: EngineConfig): Epsilon {
  return { ...DEFAULT_EPSILON, ...config.epsilon };
}

export interface Engine {
  push(e: AdasEvent): Anomaly[];
  /** EOF fallback: finish all scenarios still open. */
  end(): Anomaly[];
  activeScenarios(): number;
}

interface ScenarioState { now: number; states: Map<string, unknown> }

export function createEngine(rules: readonly AnyRule[], config: EngineConfig = {}): Engine {
  const ids = rules.map((r) => r.id);
  if (new Set(ids).size !== ids.length) throw new Error(`duplicate rule id in: ${ids.join(', ')}`);
  const cfgs = new Map(rules.map((r) => [r.id, resolveRuleConfig(r, config)] as const));
  const scenarios = new Map<string, ScenarioState>();

  const ctx = (scenarioId: string, sc: ScenarioState, r: AnyRule) => ({
    scenarioId, now: sc.now, state: sc.states.get(r.id), cfg: cfgs.get(r.id),
  });

  const open = (scenarioId: string, ts: number): ScenarioState => {
    const existing = scenarios.get(scenarioId);
    if (existing) return existing;
    const created = { now: ts, states: new Map(rules.map((r) => [r.id, r.createState()] as const)) };
    scenarios.set(scenarioId, created);
    return created;
  };

  const finish = (scenarioId: string, sc: ScenarioState): Anomaly[] => {
    const out = rules.flatMap((r) => r.onScenarioEnd(ctx(scenarioId, sc, r)));
    scenarios.delete(scenarioId);
    return out;
  };

  return {
    push(e) {
      const sc = open(e.scenarioId, e.ts);
      sc.now = Math.max(sc.now, e.ts);
      const out = rules.flatMap((r) => r.onEvent(e, ctx(e.scenarioId, sc, r)));
      if (e.module === 'SYS' && e.type === 'SCENARIO_END') out.push(...finish(e.scenarioId, sc));
      return out;
    },
    end: () => [...scenarios.entries()].flatMap(([id, sc]) => finish(id, sc)),
    activeScenarios: () => scenarios.size,
  };
}
