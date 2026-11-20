import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { parseFile, parseLines } from '../../src/parser/parse.ts';
import { RULESET_VERSION, TOOL_VERSION } from '../../src/version.ts';
import { brief } from '../helpers.ts';

const golden = fileURLToPath(new URL('../../fixtures/golden/mixed.jsonl', import.meta.url));

describe('parseFile', () => {
  it('detects the hand-derived anomalies in the golden sample', async () => {
    const { report } = await parseFile(golden);
    expect(brief(report.anomalies)).toEqual([
      { ruleId: 'ACC-01', scenarioId: 'G1', startTs: 1000, endTs: 1050, detectTs: 1500 },
      { ruleId: 'APA-02', scenarioId: 'G2', startTs: 0, endTs: 2000, detectTs: 2000 },
      { ruleId: 'HMI-01', scenarioId: 'G2', startTs: 0, endTs: 300, detectTs: 500 },
      { ruleId: 'APA-01', scenarioId: 'G2', startTs: 500, endTs: 700, detectTs: 2000 },
    ]);
    expect(report.meta).toEqual({ input: golden, events: 10, scenarios: 2, toolVersion: TOOL_VERSION, rulesetVersion: RULESET_VERSION });
    expect(report.summary).toEqual({ byRule: { 'ACC-01': 1, 'APA-02': 1, 'HMI-01': 1, 'APA-01': 1 }, bySeverity: { P1: 2, P2: 2 } });
  });

  it('matches the report snapshot', async () => {
    const { report } = await parseFile(golden);
    expect({ ...report, meta: { ...report.meta, input: 'fixtures/golden/mixed.jsonl' } }).toMatchSnapshot();
  });

  it('produces an empty report for empty input', async () => {
    const { report } = await parseLines([], 'empty.jsonl');
    expect(report.meta.events).toBe(0);
    expect(report.anomalies).toEqual([]);
    expect(report.summary).toEqual({ byRule: {}, bySeverity: {} });
  });
});
