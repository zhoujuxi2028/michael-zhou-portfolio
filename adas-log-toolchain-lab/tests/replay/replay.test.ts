import { appendFileSync, mkdirSync, mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import type { Manifest, ManifestCase } from '../../src/casegen/regression.ts';
import { UsageError } from '../../src/config.ts';
import { toJsonl } from '../../src/io/jsonl.ts';
import { failReason, parseFilter, replayManifest, type ReplayResult } from '../../src/replay/replay.ts';
import { ev, scenarioEnd } from '../../src/schema/events.ts';
import { RULESET_VERSION, TOOL_VERSION } from '../../src/version.ts';

const meta: Manifest['meta'] = { toolVersion: '0.0.0', rulesetVersion: '0.0.0', anomalies: 'a.json', log: 'e.jsonl', preMs: 3000, postMs: 3000 };

/** Hand-built manifest: an HMI-02 case and an APA-02 case (unfinished maneuver). */
function manifestDir(edit: (cases: ManifestCase[]) => void = () => {}): string {
  const dir = mkdtempSync(join(tmpdir(), 'adas-replay-'));
  mkdirSync(join(dir, 'data'));
  writeFileSync(join(dir, 'data', 'hmi.jsonl'), toJsonl([ev('ACC/TAKEOVER_REQUEST', 1000, 'S1', { reason: 'r' }), scenarioEnd('S1', 2000)]));
  writeFileSync(join(dir, 'data', 'apa.jsonl'), toJsonl([
    ev('APA/STEP', 0, 'S2', { step: 'MANEUVERING', reason: 'r' }),
    ev('HMI/DISPLAY', 10, 'S2', { feature: 'APA', item: 'STATE', value: 'MANEUVERING' }),
    scenarioEnd('S2', 1000),
  ]));
  const cases: ManifestCase[] = [
    { caseId: 'REG-HMI-02-S1-1000', ruleId: 'HMI-02', scenarioId: 'S1', source: 'data/hmi.jsonl', window: [0, 5000], expect: { ruleId: 'HMI-02', triggered: true } },
    { caseId: 'REG-APA-02-S2-0', ruleId: 'APA-02', scenarioId: 'S2', source: 'data/apa.jsonl', window: [0, 4000], expect: { ruleId: 'APA-02', triggered: true } },
  ];
  edit(cases);
  writeFileSync(join(dir, 'manifest.json'), JSON.stringify({ meta, cases }));
  return join(dir, 'manifest.json');
}

describe('replayManifest', () => {
  it('replays every case and reports PASS with fired rules, duration and summary', async () => {
    const manifest = manifestDir();
    const report = await replayManifest(manifest);
    expect(report.meta).toEqual({ toolVersion: TOOL_VERSION, rulesetVersion: RULESET_VERSION, manifest, filter: null });
    expect(report.results.map(({ caseId, ruleId, status, firedRules, dataQualityIssues }) => ({ caseId, ruleId, status, firedRules, dataQualityIssues }))).toEqual([
      { caseId: 'REG-HMI-02-S1-1000', ruleId: 'HMI-02', status: 'PASS', firedRules: ['HMI-02'], dataQualityIssues: 0 },
      { caseId: 'REG-APA-02-S2-0', ruleId: 'APA-02', status: 'PASS', firedRules: ['APA-02'], dataQualityIssues: 0 },
    ]);
    for (const r of report.results) expect(r.durationMs).toBeGreaterThanOrEqual(0);
    expect(report.summary).toEqual({ total: 2, passed: 2, failed: 0, passRate: 1 });
  });

  it('marks a case FAIL when the target rule does not fire as expected', async () => {
    const report = await replayManifest(manifestDir((cs) => { cs[1]!.expect.triggered = false; }));
    expect(report.results.map((r) => r.status)).toEqual(['PASS', 'FAIL']);
    expect(report.summary).toEqual({ total: 2, passed: 1, failed: 1, passRate: 0.5 });
  });

  it('marks a case FAIL when its data has data quality issues even though the rule fires (same criterion as generated tests)', async () => {
    const manifest = manifestDir();
    appendFileSync(join(manifest, '..', 'data', 'hmi.jsonl'), '{bad json\n');
    const report = await replayManifest(manifest);
    expect(report.results[0]).toMatchObject({ status: 'FAIL', firedRules: ['HMI-02'], dataQualityIssues: 1 });
    expect(report.results[1]).toMatchObject({ status: 'PASS', dataQualityIssues: 0 });
  });

  it('applies the window: events outside it are not replayed', async () => {
    const report = await replayManifest(manifestDir((cs) => { cs[0]!.window = [1500, 5000]; }));
    expect(report.results[0]).toMatchObject({ status: 'FAIL', firedRules: [] });
  });

  it('marks a case FAIL with an error when its data file is missing', async () => {
    const report = await replayManifest(manifestDir((cs) => { cs[0]!.source = 'data/missing.jsonl'; }));
    expect(report.results[0]).toMatchObject({ status: 'FAIL', firedRules: [], error: expect.stringContaining('missing.jsonl') });
  });

  it('replays only cases of the filtered module (module taken from the ruleId prefix)', async () => {
    const report = await replayManifest(manifestDir(), { filter: parseFilter('module=APA') });
    expect(report.meta.filter).toBe('module=APA');
    expect(report.results.map((r) => r.caseId)).toEqual(['REG-APA-02-S2-0']);
    expect(report.summary.total).toBe(1);
    const none = await replayManifest(manifestDir(), { filter: parseFilter('module=LKA') });
    expect(none.summary).toEqual({ total: 0, passed: 0, failed: 0, passRate: 0 });
  });
});

describe('failReason', () => {
  const r = (o: Partial<ReplayResult>): ReplayResult => ({ caseId: 'C', ruleId: 'HMI-02', status: 'FAIL', firedRules: [], durationMs: 0, ...o });
  it('names the error, the data quality issues, or the unmet expectation', () => {
    expect(failReason(r({ error: 'ENOENT x' }))).toBe('error ENOENT x');
    expect(failReason(r({ dataQualityIssues: 2, firedRules: ['HMI-02'] }))).toBe('case data has 2 data quality issues; fired rules [HMI-02]');
    expect(failReason(r({ dataQualityIssues: 0 }))).toBe('expected HMI-02 to fire; fired rules []');
    expect(failReason(r({ dataQualityIssues: 0, firedRules: ['ACC-01', 'HMI-02'] }))).toBe('expected HMI-02 not to fire; fired rules [ACC-01, HMI-02]');
  });
});

describe('parseFilter', () => {
  it('accepts module=<ACC|APA|HMI|LKA> and nothing else', () => {
    expect(parseFilter(undefined)).toBeNull();
    expect(parseFilter('module=LKA')).toEqual({ module: 'LKA' });
    for (const bad of ['module=XYZ', 'rule=ACC-01', 'APA', 'module=']) expect(() => parseFilter(bad)).toThrow(UsageError);
  });
});

describe('loading the manifest', () => {
  it.each([
    ['missing', (d: string) => join(d, 'nope.json'), /manifest not found/],
    ['invalid JSON', (d: string) => { writeFileSync(join(d, 'm.json'), '{'); return join(d, 'm.json'); }, /not valid JSON/],
    ['wrong shape', (d: string) => { writeFileSync(join(d, 'm.json'), '{"cases":[{"caseId":1}]}'); return join(d, 'm.json'); }, /invalid manifest/],
  ])('rejects a %s manifest with a UsageError', async (_n, make, msg) => {
    const p = replayManifest(make(mkdtempSync(join(tmpdir(), 'adas-replay-'))));
    await expect(p).rejects.toThrow(UsageError);
    await expect(p).rejects.toThrow(msg);
  });
});
