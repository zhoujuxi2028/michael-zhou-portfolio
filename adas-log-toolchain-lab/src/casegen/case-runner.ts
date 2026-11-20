import type { EngineConfig } from '../engine/engine.ts';
import type { Anomaly, AnyRule } from '../engine/types.ts';
import { readLines } from '../io/jsonl.ts';
import { parseLines } from '../parser/parse.ts';
import { hasQualityIssues } from '../parser/scan.ts';

export interface CaseRunOptions {
  /** Inclusive `[from, to]` ts window; events outside it are not fed to the rules. */
  window?: readonly [number, number];
  config?: EngineConfig;
  rules?: readonly AnyRule[];
}

export interface CaseRunResult {
  /** Distinct rule ids that fired, sorted. */
  firedRules: string[];
  anomalies: Anomaly[];
  /** Total data quality issue count (0 = clean input). */
  dataQualityIssues: number;
}

/** Keep lines whose `ts` lies inside the window; unparsable lines pass through so they are counted. */
async function* inWindow(lines: AsyncIterable<string>, [from, to]: readonly [number, number]): AsyncIterable<string> {
  for await (const line of lines) {
    let ts: unknown;
    try {
      ts = (JSON.parse(line) as { ts?: unknown }).ts;
    } catch {
      yield line;
      continue;
    }
    if (typeof ts !== 'number' || (ts >= from && ts <= to)) yield line;
  }
}

/** Run one case file (JSON Lines) through the same scan + engine path as `adas-lab parse`. */
export async function runCaseFile(path: string, opts: CaseRunOptions = {}): Promise<CaseRunResult> {
  const lines = opts.window ? inWindow(readLines(path), opts.window) : readLines(path);
  const { report } = await parseLines(lines, path, { config: opts.config, rules: opts.rules });
  const q = report.dataQuality;
  return {
    firedRules: [...new Set(report.anomalies.map((a) => a.ruleId))].sort(),
    anomalies: report.anomalies,
    dataQualityIssues: hasQualityIssues(q) ? Object.values(q.counts).reduce((a, b) => a + b, 0) : 0,
  };
}
