import { createEngine, type EngineConfig } from '../src/engine/engine.ts';
import type { Anomaly, AnyRule } from '../src/engine/types.ts';
import type { AdasEvent } from '../src/schema/events.ts';

/** Run events through an engine with the given rules and flush at EOF. */
export function run(rules: readonly AnyRule[], events: readonly AdasEvent[], config?: EngineConfig): Anomaly[] {
  const engine = createEngine(rules, config);
  return [...events.flatMap((e) => engine.push(e)), ...engine.end()];
}

/** Compact view for assertions. */
export const brief = (as: readonly Anomaly[]) =>
  as.map((a) => ({ ruleId: a.ruleId, scenarioId: a.scenarioId, startTs: a.startTs, endTs: a.endTs, detectTs: a.detectTs }));
