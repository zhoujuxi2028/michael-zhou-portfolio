import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { runCaseFile } from '../../src/casegen/case-runner.ts';
import { toJsonl } from '../../src/io/jsonl.ts';
import { ev, scenarioEnd } from '../../src/schema/events.ts';

const S = 'S1';
const req = (ts: number) => ev('ACC/TAKEOVER_REQUEST', ts, S, { reason: 't' });
const shown = (ts: number) => ev('HMI/DISPLAY', ts, S, { feature: 'ACC', item: 'TAKEOVER', value: 'SHOWN' });

function file(content: string): string {
  const p = join(mkdtempSync(join(tmpdir(), 'adas-run-')), 'case.jsonl');
  writeFileSync(p, content);
  return p;
}

describe('runCaseFile', () => {
  it('reports which rules fired and data quality issues', async () => {
    const r = await runCaseFile(file(toJsonl([req(1000), scenarioEnd(S, 2000)])));
    expect(r.firedRules).toEqual(['HMI-02']);
    expect(r.anomalies).toHaveLength(1);
    expect(r.dataQualityIssues).toBe(0);
  });

  it('only feeds events inside the window (inclusive)', async () => {
    const p = file(toJsonl([req(1000), shown(1100), req(5000), scenarioEnd(S, 9000)]));
    expect((await runCaseFile(p, { window: [0, 1100] })).firedRules).toEqual([]);
    expect((await runCaseFile(p, { window: [4000, 6000] })).firedRules).toEqual(['HMI-02']);
  });

  it('counts bad lines as data quality issues', async () => {
    const r = await runCaseFile(file(`{bad\n${toJsonl([scenarioEnd(S, 0)])}`));
    expect(r.dataQualityIssues).toBe(1);
  });

  it('still counts bad lines when a window is applied', async () => {
    const r = await runCaseFile(file(`{bad\n${toJsonl([scenarioEnd(S, 50)])}`), { window: [0, 100] });
    expect(r.dataQualityIssues).toBe(1);
  });

  it('applies rule config overrides', async () => {
    const p = file(toJsonl([req(1000), shown(1300), scenarioEnd(S, 2000)]));
    expect((await runCaseFile(p)).firedRules).toEqual(['HMI-02']);
    expect((await runCaseFile(p, { config: { rules: { 'HMI-02': { takeoverWindowMs: 400 } } } })).firedRules).toEqual([]);
  });
});
