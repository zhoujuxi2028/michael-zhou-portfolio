import { createEngine, type EngineConfig } from '../engine/engine.ts';
import type { Anomaly, AnyRule } from '../engine/types.ts';
import { readLines } from '../io/jsonl.ts';
import { RULES } from '../rules/index.ts';
import { RULESET_VERSION, TOOL_VERSION } from '../version.ts';
import { scanLines, type DataQuality, type ScanOptions, type ScanResult } from './scan.ts';

export interface Report {
  meta: { input: string; events: number; scenarios: number; toolVersion: string; rulesetVersion: string };
  anomalies: Anomaly[];
  summary: { byRule: Record<string, number>; bySeverity: Record<string, number> };
  dataQuality: DataQuality;
}

export interface ParseOptions extends ScanOptions {
  config?: EngineConfig;
  rules?: readonly AnyRule[];
}

export interface ParseOutcome { report: Report; peakActiveScenarios: number }

const byPosition = (a: Anomaly, b: Anomaly) =>
  a.scenarioId < b.scenarioId ? -1 : a.scenarioId > b.scenarioId ? 1
    : a.startTs - b.startTs || (a.ruleId < b.ruleId ? -1 : a.ruleId > b.ruleId ? 1 : 0);

export function buildReport(input: string, scan: ScanResult, anomalies: readonly Anomaly[]): Report {
  const sorted = [...anomalies].sort(byPosition);
  const byRule: Record<string, number> = {};
  const bySeverity: Record<string, number> = {};
  for (const a of sorted) {
    byRule[a.ruleId] = (byRule[a.ruleId] ?? 0) + 1;
    bySeverity[a.severity] = (bySeverity[a.severity] ?? 0) + 1;
  }
  return {
    meta: { input, events: scan.events, scenarios: scan.scenarios, toolVersion: TOOL_VERSION, rulesetVersion: RULESET_VERSION },
    anomalies: sorted,
    summary: { byRule, bySeverity },
    dataQuality: scan.quality,
  };
}

export async function parseLines(
  lines: AsyncIterable<string> | Iterable<string>,
  input: string,
  opts: ParseOptions = {},
): Promise<ParseOutcome> {
  const engine = createEngine(opts.rules ?? RULES, opts.config);
  const anomalies: Anomaly[] = [];
  let peakActiveScenarios = 0;
  const scan = await scanLines(lines, (e) => {
    anomalies.push(...engine.push(e));
    peakActiveScenarios = Math.max(peakActiveScenarios, engine.activeScenarios());
  }, opts);
  anomalies.push(...engine.end());
  return { report: buildReport(input, scan, anomalies), peakActiveScenarios };
}

export function parseFile(path: string, opts: ParseOptions = {}): Promise<ParseOutcome> {
  return parseLines(readLines(path), path, opts);
}
