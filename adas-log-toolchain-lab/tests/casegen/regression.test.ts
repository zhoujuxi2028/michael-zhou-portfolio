import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
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
});
