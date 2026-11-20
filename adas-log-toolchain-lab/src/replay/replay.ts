import { dirname, resolve } from 'node:path';
import { performance } from 'node:perf_hooks';
import { runCaseFile } from '../casegen/case-runner.ts';
import { UsageError } from '../config.ts';
import { RULESET_VERSION, TOOL_VERSION } from '../version.ts';
import { loadManifest } from './manifest.ts';

export const FILTER_MODULES = ['ACC', 'APA', 'HMI', 'LKA'] as const;
export type FilterModule = (typeof FILTER_MODULES)[number];
export interface ReplayFilter { module: FilterModule }

export interface ReplayResult {
  caseId: string;
  ruleId: string;
  status: 'PASS' | 'FAIL';
  firedRules: string[];
  durationMs: number;
  /** Set when the case could not be run (e.g. missing data file). */
  error?: string;
}

/** `replay-report.json` (contract C2). */
export interface ReplayReport {
  meta: { toolVersion: string; rulesetVersion: string; manifest: string; filter: string | null };
  results: ReplayResult[];
  summary: { total: number; passed: number; failed: number; passRate: number };
}

/** Parse `--filter module=<ACC|APA|HMI|LKA>`; `undefined` means no filter. */
export function parseFilter(s: string | undefined): ReplayFilter | null {
  if (s === undefined) return null;
  const m = /^module=(.+)$/.exec(s);
  const mod = m?.[1];
  if (!mod || !(FILTER_MODULES as readonly string[]).includes(mod)) {
    throw new UsageError(`invalid filter "${s}": expected module=<${FILTER_MODULES.join('|')}>`);
  }
  return { module: mod as FilterModule };
}

/** Module of a rule id, e.g. `APA-02` → `APA`. */
const moduleOf = (ruleId: string) => ruleId.split('-')[0];

/** Replay every manifest case (optionally filtered) through the rule engine with its window. */
export async function replayManifest(manifestPath: string, opts: { filter?: ReplayFilter | null } = {}): Promise<ReplayReport> {
  const manifest = await loadManifest(manifestPath);
  const filter = opts.filter ?? null;
  const base = dirname(manifestPath);
  const results: ReplayResult[] = [];
  for (const c of manifest.cases) {
    if (filter && moduleOf(c.ruleId) !== filter.module) continue;
    const t0 = performance.now();
    let firedRules: string[] = [], error: string | undefined;
    try {
      firedRules = (await runCaseFile(resolve(base, c.source), { window: c.window })).firedRules;
    } catch (e) {
      error = e instanceof Error ? e.message : String(e);
    }
    const durationMs = Math.round((performance.now() - t0) * 100) / 100;
    const pass = error === undefined && firedRules.includes(c.expect.ruleId) === c.expect.triggered;
    results.push({ caseId: c.caseId, ruleId: c.ruleId, status: pass ? 'PASS' : 'FAIL', firedRules, durationMs, ...(error === undefined ? {} : { error }) });
  }
  const passed = results.filter((r) => r.status === 'PASS').length;
  const total = results.length;
  return {
    meta: { toolVersion: TOOL_VERSION, rulesetVersion: RULESET_VERSION, manifest: manifestPath, filter: filter ? `module=${filter.module}` : null },
    results,
    summary: { total, passed, failed: total - passed, passRate: total === 0 ? 0 : Math.round((passed / total) * 10000) / 10000 },
  };
}
