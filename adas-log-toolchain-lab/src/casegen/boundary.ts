import { mkdir, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { createEngine, resolveEpsilon, resolveRuleConfig, type EngineConfig } from '../engine/engine.ts';
import type { AnyRule, BoundaryCase } from '../engine/types.ts';
import { toJsonl } from '../io/jsonl.ts';
import { RULES } from '../rules/index.ts';

export type RuleBoundaryCase = BoundaryCase & { ruleId: string };
export interface BoundaryEntry { caseId: string; ruleId: string; file: string; expectTriggered: boolean; description: string }
export interface CaseResult { caseId: string; ruleId: string; expectTriggered: boolean; triggered: boolean; pass: boolean }

/** Collect boundary cases from every rule; rules own the construction logic. */
export function buildBoundaryCases(rules: readonly AnyRule[] = RULES, config: EngineConfig = {}): RuleBoundaryCase[] {
  const eps = resolveEpsilon(config);
  return rules.flatMap((r) => r.boundaryCases(resolveRuleConfig(r, config), eps).map((c) => ({ ...c, ruleId: r.id })));
}

/** Run each case through a fresh engine and compare with `expectTriggered`. */
export function verifyBoundaryCases(
  cases: readonly RuleBoundaryCase[],
  rules: readonly AnyRule[] = RULES,
  config: EngineConfig = {},
): CaseResult[] {
  return cases.map((c) => {
    const engine = createEngine(rules, config);
    const anomalies = [...c.events.flatMap((e) => engine.push(e)), ...engine.end()];
    const triggered = anomalies.some((a) => a.ruleId === c.ruleId);
    return { caseId: c.caseId, ruleId: c.ruleId, expectTriggered: c.expectTriggered, triggered, pass: triggered === c.expectTriggered };
  });
}

export async function writeBoundaryCases(outDir: string, cases: readonly RuleBoundaryCase[]): Promise<BoundaryEntry[]> {
  const dir = join(outDir, 'boundary');
  await mkdir(dir, { recursive: true });
  const entries: BoundaryEntry[] = [];
  for (const c of cases) {
    const file = `${c.caseId}.jsonl`;
    await writeFile(join(dir, file), toJsonl(c.events));
    entries.push({ caseId: c.caseId, ruleId: c.ruleId, file, expectTriggered: c.expectTriggered, description: c.description });
  }
  await writeFile(join(dir, 'expected.json'), `${JSON.stringify(entries, null, 2)}\n`);
  return entries;
}
