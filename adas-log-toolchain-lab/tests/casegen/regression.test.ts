import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { UsageError } from '../../src/config.ts';
import { buildRegressionCases, regressionCaseIds } from '../../src/casegen/regression.ts';
import type { Anomaly } from '../../src/engine/types.ts';
import { toJsonl } from '../../src/io/jsonl.ts';
import { scenarioEnd } from '../../src/schema/events.ts';
import { RULESET_VERSION, TOOL_VERSION } from '../../src/version.ts';
import { fixture, takeover } from './regression-fixture.ts';

const a = (ruleId: string, scenarioId: string, startTs: number) => ({ ruleId, scenarioId, startTs }) as Anomaly;

describe('regressionCaseIds', () => {
  it('names cases REG-<ruleId>-<scenarioId>-<startTs> and suffixes collisions', () => {
    expect(regressionCaseIds([a('ACC-01', 'S007', 12340), a('HMI-02', 'S1', 0), a('ACC-01', 'S007', 12340), a('ACC-01', 'S007', 12340)]))
      .toEqual(['REG-ACC-01-S007-12340', 'REG-HMI-02-S1-0', 'REG-ACC-01-S007-12340-2', 'REG-ACC-01-S007-12340-3']);
  });
});

describe('buildRegressionCases', () => {
  it('writes one self-contained case per anomaly and a manifest', async () => {
    const { dir, log, anomalies } = await fixture();
    const report = JSON.parse(readFileSync(anomalies, 'utf8')) as { anomalies: Anomaly[] };
    const out = join(dir, 'cases');
    const { manifest, manifestPath } = await buildRegressionCases({ anomalies, log, out, preMs: 3000, postMs: 3000 });

    expect(manifestPath).toBe(join(out, 'manifest.json'));
    expect(JSON.parse(readFileSync(manifestPath, 'utf8'))).toEqual(manifest);
    expect(manifest.meta).toEqual({ toolVersion: TOOL_VERSION, rulesetVersion: RULESET_VERSION, anomalies, log, preMs: 3000, postMs: 3000 });
    expect(manifest.cases).toHaveLength(report.anomalies.length);

    const s1 = manifest.cases.find((c) => c.scenarioId === 'S1')!;
    const s1Anomaly = report.anomalies.find((x) => x.scenarioId === 'S1')!;
    expect(s1).toEqual({
      caseId: 'REG-HMI-02-S1-1000', ruleId: 'HMI-02', scenarioId: 'S1', source: 'data/REG-HMI-02-S1-1000.jsonl',
      window: [0, s1Anomaly.detectTs + 3000], expect: { ruleId: 'HMI-02', triggered: true },
    });
    expect(readFileSync(join(out, s1.source), 'utf8')).toBe(toJsonl([takeover(1000, 'S1'), scenarioEnd('S1', 2000)]));

    // S2: the ACC/STATE precondition (ts 100) is kept and window[0] lowered to it.
    const s2 = manifest.cases.find((c) => c.scenarioId === 'S2' && c.ruleId === 'HMI-02')!;
    expect(s2.window[0]).toBe(100);
    const lines = readFileSync(join(out, s2.source), 'utf8').trimEnd().split('\n').map((l) => JSON.parse(l) as { ts: number });
    expect(lines.map((e) => e.ts)).toEqual([100, 6000, 7000, 8000, 8500, 9000]);
    for (const c of manifest.cases) expect(existsSync(join(out, c.source))).toBe(true);
  });

  it('generates a regression test that type-checks, runs green, and fails when an expectation is flipped', async () => {
    const root = fileURLToPath(new URL('../..', import.meta.url));
    const { log, anomalies } = await fixture();
    mkdirSync(join(root, 'out'), { recursive: true });
    const out = mkdtempSync(join(root, 'out', 'reg-'));
    const { manifest, testPath } = await buildRegressionCases({ anomalies, log, out, preMs: 3000, postMs: 3000 });
    expect(testPath).toBe(join(out, 'generated', 'regression.test.ts'));
    const src = readFileSync(testPath, 'utf8');
    for (const c of manifest.cases) expect(src).toContain(`"file": "../${c.source}"`);

    const dir = join(out, 'generated');
    writeFileSync(join(dir, 'tsconfig.json'), JSON.stringify({ extends: relative(dir, join(root, 'tsconfig.json')), include: ['*.ts'] }));
    const tsc = spawnSync('npx', ['tsc', '--noEmit', '-p', join(dir, 'tsconfig.json')], { cwd: root, encoding: 'utf8' });
    expect(tsc.stdout + tsc.stderr).toBe('');
    const vitest = () => spawnSync('npx', ['vitest', 'run', '--config', 'vitest.generated.config.ts', dir], { cwd: root, encoding: 'utf8' });
    const green = vitest();
    expect(green.status, green.stdout).toBe(0);
    expect(green.stdout).toContain(`${manifest.cases.length} passed`);

    writeFileSync(testPath, src.replace('"expectTriggered": true', '"expectTriggered": false'));
    const red = vitest();
    expect(red.status).toBe(1);
    expect(red.stdout).toContain('1 failed');
  }, 60_000);

  it.each([
    ['missing anomalies file', (d: string) => ({ anomalies: join(d, 'nope.json') }), /anomalies file not found/],
    ['invalid JSON', (d: string) => { writeFileSync(join(d, 'bad.json'), '{'); return { anomalies: join(d, 'bad.json') }; }, /not valid JSON/],
    ['wrong shape', (d: string) => { writeFileSync(join(d, 'bad.json'), '{"anomalies":[{"ruleId":1}]}'); return { anomalies: join(d, 'bad.json') }; }, /invalid anomalies file/],
    ['missing log file', (d: string) => ({ log: join(d, 'nope.jsonl') }), /log file not found/],
  ])('rejects bad input with a UsageError: %s', async (_name, override, msg) => {
    const f = await fixture();
    const p = buildRegressionCases({ anomalies: f.anomalies, log: f.log, out: join(f.dir, 'cases'), preMs: 3000, postMs: 3000, ...override(f.dir) });
    await expect(p).rejects.toThrow(UsageError);
    await expect(p).rejects.toThrow(msg);
  });
});
