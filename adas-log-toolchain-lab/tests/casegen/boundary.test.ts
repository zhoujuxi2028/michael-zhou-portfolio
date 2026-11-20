import { mkdtempSync, readFileSync, readdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { buildBoundaryCases, verifyBoundaryCases, writeBoundaryCases } from '../../src/casegen/boundary.ts';
import { ev, scenarioEnd } from '../../src/schema/events.ts';
import { RULES } from '../../src/rules/index.ts';
import { parseLine } from '../../src/schema/validate.ts';

describe('casegen boundary', () => {
  const cases = buildBoundaryCases();

  it('produces at least 3 cases per rule with unique ids', () => {
    for (const r of RULES) expect(cases.filter((c) => c.ruleId === r.id).length).toBeGreaterThanOrEqual(3);
    expect(new Set(cases.map((c) => c.caseId)).size).toBe(cases.length);
    expect(cases).toHaveLength(38);
  });

  it('covers the LKA groups: torque, confidence, persistence, display window', () => {
    const groups = (id: string) => [...new Set(cases.filter((c) => c.ruleId === id).map((c) => c.caseId.replace(/-(minus|eq|plus)$/, '')))];
    expect(groups('LKA-01')).toEqual(['LKA-01-torque']);
    expect(groups('LKA-02')).toEqual(['LKA-02-conf', 'LKA-02-persist', 'LKA-02-window']);
    const triggered = cases.filter((c) => c.ruleId.startsWith('LKA') && c.expectTriggered).map((c) => c.caseId);
    expect(triggered).toEqual(['LKA-01-torque-plus', 'LKA-02-conf-minus', 'LKA-02-persist-plus', 'LKA-02-window-plus']);
  });

  it('every case is a complete scenario ending with SCENARIO_END', () => {
    for (const c of cases) {
      const last = c.events.at(-1)!;
      expect([last.module, last.type, last.scenarioId]).toEqual(['SYS', 'SCENARIO_END', c.caseId]);
    }
  });

  it('all generated cases pass through the parser and fire only their target rule', async () => {
    const results = await verifyBoundaryCases(cases);
    expect(results).toHaveLength(38);
    expect(results.filter((r) => !r.pass)).toEqual([]);
    for (const r of results) expect(r.firedRules).toEqual(r.expectTriggered ? [r.ruleId] : []);
  });

  it('fails a case when another rule fires', async () => {
    const id = 'X-extra';
    const bad = { caseId: id, ruleId: 'ACC-01', expectTriggered: false, description: 'no HMI display',
      events: [ev('ACC/STATE', 0, id, { from: 'OFF', to: 'ACTIVE', reason: 'x' }), scenarioEnd(id, 2000)] };
    const [r] = await verifyBoundaryCases([bad]);
    expect(r).toMatchObject({ triggered: false, pass: false, firedRules: ['HMI-01'] });
  });

  it('fails a case with data quality issues', async () => {
    const id = 'X-late';
    const bad = { caseId: id, ruleId: 'ACC-01', expectTriggered: false, description: 'late event',
      events: [scenarioEnd(id, 2000), ev('ACC/BRAKE_PEDAL', 1000, id, { pressed: false })] };
    const [r] = await verifyBoundaryCases([bad]);
    expect(r).toMatchObject({ pass: false, dataQualityIssues: 1 });
  });

  it('applies config overrides to thresholds', async () => {
    const custom = buildBoundaryCases(undefined, { rules: { 'ACC-02': { exitWindowMs: 500 } }, epsilon: { time: 5 } });
    const plus = custom.find((c) => c.caseId === 'ACC-02-window-plus')!;
    expect(plus.events.find((e) => e.type === 'STATE' && e.ts > 0)?.ts).toBe(1505);
    const results = await verifyBoundaryCases(custom, undefined, { rules: { 'ACC-02': { exitWindowMs: 500 } } });
    expect(results.every((r) => r.pass)).toBe(true);
  });

  it('writes one JSONL file per case plus expected.json', async () => {
    const out = mkdtempSync(join(tmpdir(), 'adas-cases-'));
    const entries = await writeBoundaryCases(out, cases);
    const dir = join(out, 'boundary');
    expect(readdirSync(dir)).toHaveLength(cases.length + 1);
    const expected = JSON.parse(readFileSync(join(dir, 'expected.json'), 'utf8')) as typeof entries;
    expect(expected).toEqual(entries);
    expect(expected).toHaveLength(38);
    for (const entry of expected) {
      const lines = readFileSync(join(dir, entry.file), 'utf8').trimEnd().split('\n');
      expect(lines.every((x) => parseLine(x).ok)).toBe(true);
    }
  });
});
