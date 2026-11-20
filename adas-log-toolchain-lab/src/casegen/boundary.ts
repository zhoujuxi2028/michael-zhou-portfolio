import { mkdir, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { resolveEpsilon, resolveRuleConfig, type EngineConfig } from '../engine/engine.ts';
import type { AnyRule, BoundaryCase } from '../engine/types.ts';
import { toJsonl } from '../io/jsonl.ts';
import { parseLines } from '../parser/parse.ts';
import { RULES } from '../rules/index.ts';

export type RuleBoundaryCase = BoundaryCase & { ruleId: string };
export interface BoundaryEntry { caseId: string; ruleId: string; file: string; expectTriggered: boolean; description: string }
export interface CaseResult {
  caseId: string;
  ruleId: string;
  expectTriggered: boolean;
  triggered: boolean;
  /** Distinct rule ids that fired for this case (should be only the target rule, if any). */
  firedRules: string[];
  dataQualityIssues: number;
  pass: boolean;
}

/** Collect boundary cases from every rule; rules own the construction logic. */
export function buildBoundaryCases(rules: readonly AnyRule[] = RULES, config: EngineConfig = {}): RuleBoundaryCase[] {
  const eps = resolveEpsilon(config);
  return rules.flatMap((r) => r.boundaryCases(resolveRuleConfig(r, config), eps).map((c) => ({ ...c, ruleId: r.id })));
}

/**
 * Serialize each case and run it through the parser (scan + engine, the `adas-lab parse` path).
 * A case passes when its target rule fired as expected, no other rule fired, and the
 * data quality report is clean.
 */
export async function verifyBoundaryCases(
  cases: readonly RuleBoundaryCase[],
  rules: readonly AnyRule[] = RULES,
  config: EngineConfig = {},
): Promise<CaseResult[]> {
  const results: CaseResult[] = [];
  for (const c of cases) {
    const lines = toJsonl(c.events).trimEnd().split('\n');
    const { report } = await parseLines(lines, c.caseId, { rules, config });
    const firedRules = [...new Set(report.anomalies.map((a) => a.ruleId))].sort();
    const triggered = firedRules.includes(c.ruleId);
    const dataQualityIssues = Object.values(report.dataQuality.counts).reduce((x, y) => x + y, 0);
    const onlyTarget = firedRules.every((id) => id === c.ruleId);
    results.push({
      caseId: c.caseId, ruleId: c.ruleId, expectTriggered: c.expectTriggered, triggered, firedRules, dataQualityIssues,
      pass: triggered === c.expectTriggered && onlyTarget && dataQualityIssues === 0,
    });
  }
  return results;
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
