import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import type { Anomaly } from '../engine/types.ts';
import { toJsonl } from '../io/jsonl.ts';
import { RULESET_VERSION, TOOL_VERSION } from '../version.ts';
import { extractWindows } from './extract.ts';

/** One regression case in `manifest.json` (contract C1). */
export interface ManifestCase {
  caseId: string;
  ruleId: string;
  scenarioId: string;
  /** Data file, relative to the manifest directory. */
  source: string;
  /** Inclusive replay window `[from, to]`. */
  window: [number, number];
  expect: { ruleId: string; triggered: boolean };
}

export interface Manifest {
  meta: { toolVersion: string; rulesetVersion: string; anomalies: string; log: string; preMs: number; postMs: number };
  cases: ManifestCase[];
}

export interface RegressionOptions {
  /** anomalies.json path. */
  anomalies: string;
  /** Original JSON Lines log. */
  log: string;
  /** Output directory (manifest.json, data/, generated/). */
  out: string;
  preMs: number;
  postMs: number;
}

export interface RegressionResult { manifest: Manifest; manifestPath: string }

/** `REG-<ruleId>-<scenarioId>-<startTs>`; repeated ids get `-2`, `-3`, … in input order. */
export function regressionCaseIds(anomalies: readonly Anomaly[]): string[] {
  const seen = new Map<string, number>();
  return anomalies.map((a) => {
    const base = `REG-${a.ruleId}-${a.scenarioId}-${a.startTs}`;
    const n = (seen.get(base) ?? 0) + 1;
    seen.set(base, n);
    return n === 1 ? base : `${base}-${n}`;
  });
}

/** Turn every anomaly into a self-contained regression case. */
export async function buildRegressionCases(o: RegressionOptions): Promise<RegressionResult> {
  const { anomalies } = JSON.parse(await readFile(o.anomalies, 'utf8')) as { anomalies: Anomaly[] };
  const ids = regressionCaseIds(anomalies);
  const windows = await extractWindows(o.log, anomalies.map((a) => ({
    scenarioId: a.scenarioId, from: Math.max(0, a.startTs - o.preMs), to: a.detectTs + o.postMs,
  })));
  await mkdir(join(o.out, 'data'), { recursive: true });
  const cases: ManifestCase[] = [];
  for (const [i, a] of anomalies.entries()) {
    const caseId = ids[i]!, { window, events } = windows[i]!;
    const source = `data/${caseId}.jsonl`;
    await writeFile(join(o.out, source), toJsonl(events));
    cases.push({ caseId, ruleId: a.ruleId, scenarioId: a.scenarioId, source, window, expect: { ruleId: a.ruleId, triggered: true } });
  }
  const manifest: Manifest = {
    meta: { toolVersion: TOOL_VERSION, rulesetVersion: RULESET_VERSION, anomalies: o.anomalies, log: o.log, preMs: o.preMs, postMs: o.postMs },
    cases,
  };
  const manifestPath = join(o.out, 'manifest.json');
  await writeFile(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);
  return { manifest, manifestPath };
}
