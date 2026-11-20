import type { AdasEvent, Module } from '../schema/events.ts';

export type Severity = 'P1' | 'P2';

export interface Anomaly {
  ruleId: string;
  module: Module;
  severity: Severity;
  scenarioId: string;
  /** Event that started the judgment (threshold rules: first hit). */
  startTs: number;
  /** Threshold rules: last hit; window rules: deadline; unfinished flows: SCENARIO_END ts. */
  endTs: number;
  /** Scenario time when the rule decided. */
  detectTs: number;
  message: string;
  evidence: Record<string, unknown>;
}

export interface Epsilon { distance: number; time: number; torque: number }
export const DEFAULT_EPSILON: Epsilon = { distance: 0.01, time: 1, torque: 0.01 };

export interface RuleCtx<S, C> {
  scenarioId: string;
  /** Max ts seen in this scenario (never moves backwards). */
  now: number;
  /** Per-scenario rule state: mutate it in place; reassigning `ctx.state` would be lost. */
  readonly state: S;
  cfg: C;
}

export interface BoundaryCase {
  caseId: string;
  description: string;
  /** Complete scenario incl. preconditions and SCENARIO_END. */
  events: AdasEvent[];
  expectTriggered: boolean;
}

export interface RuleMeta { readonly id: string; readonly module: Module; readonly severity: Severity }

export interface Rule<S, C extends object> extends RuleMeta {
  readonly title: string;
  readonly defaults: C;
  createState(): S;
  onEvent(e: AdasEvent, ctx: RuleCtx<S, C>): Anomaly[];
  onScenarioEnd(ctx: RuleCtx<S, C>): Anomaly[];
  boundaryCases(cfg: C, eps: Epsilon): BoundaryCase[];
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type AnyRule = Rule<any, any>;

export function anomaly(
  meta: RuleMeta,
  ctx: { scenarioId: string; now: number },
  f: { startTs: number; endTs: number; message: string; evidence?: Record<string, unknown> },
): Anomaly {
  return {
    ruleId: meta.id, module: meta.module, severity: meta.severity, scenarioId: ctx.scenarioId,
    startTs: f.startTs, endTs: f.endTs, detectTs: ctx.now, message: f.message, evidence: f.evidence ?? {},
  };
}
