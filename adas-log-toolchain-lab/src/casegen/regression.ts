import { access, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { isAbsolute, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { z } from 'zod';
import { UsageError } from '../config.ts';
import { toJsonl } from '../io/jsonl.ts';
import { ScenarioId } from '../schema/events.ts';
import { RULESET_VERSION, TOOL_VERSION } from '../version.ts';
import { extractWindows } from './extract.ts';
import { renderCaseTest, type CaseTestEntry } from './template.ts';

/** The anomaly fields a regression case needs (the rest of anomalies.json is ignored). */
const AnomaliesFile = z.object({
  anomalies: z.array(z.object({
    ruleId: z.string().min(1), scenarioId: ScenarioId,
    startTs: z.number().int(), detectTs: z.number().int(), message: z.string(),
  })),
});
export type AnomalyRef = z.infer<typeof AnomaliesFile>['anomalies'][number];

const RUNNER = fileURLToPath(new URL('./case-runner.ts', import.meta.url));

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
  /** Output directory; its manifest.json, data/ and generated/ are replaced. */
  out: string;
  preMs: number;
  postMs: number;
}

export interface RegressionResult {
  manifest: Manifest;
  manifestPath: string;
  /** `<out>/generated/regression.test.ts`. */
  testPath: string;
}

/** `REG-<ruleId>-<scenarioId>-<startTs>`; repeated ids get `-2`, `-3`, … in input order. */
export function regressionCaseIds(anomalies: readonly Pick<AnomalyRef, 'ruleId' | 'scenarioId' | 'startTs'>[]): string[] {
  const seen = new Map<string, number>();
  return anomalies.map((a) => {
    const base = `REG-${a.ruleId}-${a.scenarioId}-${a.startTs}`;
    const n = (seen.get(base) ?? 0) + 1;
    seen.set(base, n);
    return n === 1 ? base : `${base}-${n}`;
  });
}

/** Read and validate anomalies.json; any problem is a usage error (exit 2). */
export async function loadAnomalies(path: string): Promise<AnomalyRef[]> {
  let text: string;
  try {
    text = await readFile(path, 'utf8');
  } catch {
    throw new UsageError(`anomalies file not found: ${path}`);
  }
  let raw: unknown;
  try {
    raw = JSON.parse(text);
  } catch {
    throw new UsageError(`anomalies file is not valid JSON: ${path}`);
  }
  const r = AnomaliesFile.safeParse(raw);
  if (!r.success) {
    throw new UsageError(`invalid anomalies file: ${r.error.issues.map((i) => `${i.path.map(String).join('.')}: ${i.message}`).join('; ')}`);
  }
  return r.data.anomalies;
}

/** Turn every anomaly into a self-contained regression case. */
export async function buildRegressionCases(o: RegressionOptions): Promise<RegressionResult> {
  const anomalies = await loadAnomalies(o.anomalies);
  try {
    await access(o.log);
  } catch {
    throw new UsageError(`log file not found: ${o.log}`);
  }
  const ids = regressionCaseIds(anomalies);
  const outDir = resolve(o.out);
  for (const id of ids) {
    const rel = relative(outDir, resolve(outDir, 'data', `${id}.jsonl`));
    if (rel.startsWith('..') || isAbsolute(rel)) throw new UsageError(`case ${id} escapes the output directory ${o.out}`);
  }
  const windows = await extractWindows(o.log, anomalies.map((a) => ({
    scenarioId: a.scenarioId, from: Math.max(0, a.startTs - o.preMs), to: a.detectTs + o.postMs,
  })));
  // Replace (never merge with) the previous run's output; nothing else under --out is touched.
  for (const p of ['data', 'generated', 'manifest.json']) await rm(join(o.out, p), { recursive: true, force: true });
  await mkdir(join(o.out, 'data'), { recursive: true });
  const cases: ManifestCase[] = [];
  const entries: CaseTestEntry[] = [];
  for (const [i, a] of anomalies.entries()) {
    const caseId = ids[i]!, { window, events } = windows[i]!;
    const source = `data/${caseId}.jsonl`;
    await writeFile(join(o.out, source), toJsonl(events));
    cases.push({ caseId, ruleId: a.ruleId, scenarioId: a.scenarioId, source, window, expect: { ruleId: a.ruleId, triggered: true } });
    entries.push({ caseId, ruleId: a.ruleId, file: `../${source}`, window, expectTriggered: true, description: a.message });
  }
  const manifest: Manifest = {
    meta: { toolVersion: TOOL_VERSION, rulesetVersion: RULESET_VERSION, anomalies: o.anomalies, log: o.log, preMs: o.preMs, postMs: o.postMs },
    cases,
  };
  const manifestPath = join(o.out, 'manifest.json');
  await writeFile(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);

  const generated = join(o.out, 'generated');
  const testPath = join(generated, 'regression.test.ts');
  await mkdir(generated, { recursive: true });
  const runnerImport = relative(generated, RUNNER).replaceAll('\\', '/');
  await writeFile(testPath, renderCaseTest(entries, { title: 'regression cases', runnerImport }));
  return { manifest, manifestPath, testPath };
}
