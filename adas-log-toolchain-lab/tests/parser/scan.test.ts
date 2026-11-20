import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { readLines, toJsonl } from '../../src/io/jsonl.ts';
import { hasQualityIssues, scanLines } from '../../src/parser/scan.ts';
import type { AdasEvent } from '../../src/schema/events.ts';
import { ev, scenarioEnd } from '../../src/schema/events.ts';

const fixture = fileURLToPath(new URL('../../fixtures/bad-lines.jsonl', import.meta.url));

describe('scanLines', () => {
  it('skips and records bad lines without stopping', async () => {
    const seen: AdasEvent[] = [];
    const r = await scanLines(readLines(fixture), (e) => seen.push(e));
    expect(seen.map((e) => e.ts)).toEqual([0, 50, 40, 60]);
    expect(r.events).toBe(4);
    expect(r.scenarios).toBe(1);
    expect(r.quality.invalidJson).toEqual([{ line: 2, detail: 'invalid JSON' }]);
    expect(r.quality.schemaErrors.map((i) => i.line)).toEqual([3, 8]);
    expect(r.quality.schemaErrors[0]?.detail).toContain('data.egoSpeed');
    expect(r.quality.unknownEvents).toEqual({ 'RADAR/POINTS': 1 });
    expect(r.quality.outOfOrder).toEqual([{ line: 7, scenarioId: 'B1', ts: 40, prevTs: 50 }]);
    expect(r.quality.counts).toEqual({ invalidJson: 1, schemaErrors: 2, unknownEvents: 1, outOfOrder: 1 });
    expect(hasQualityIssues(r.quality)).toBe(true);
  });

  it('reports a clean input as having no issues', async () => {
    const r = await scanLines([JSON.stringify(scenarioEnd('S', 0))], () => {});
    expect(hasQualityIssues(r.quality)).toBe(false);
    expect(r.events).toBe(1);
  });

  it('handles empty input', async () => {
    const r = await scanLines([], () => {});
    expect(r).toMatchObject({ events: 0, scenarios: 0 });
  });

  it('treats over-long lines as invalid', async () => {
    const r = await scanLines(['x'.repeat(200)], () => {}, { maxLineBytes: 100 });
    expect(r.quality.invalidJson).toEqual([{ line: 1, detail: 'line exceeds 100 bytes' }]);
  });

  it('caps issue details but keeps counting', async () => {
    const r = await scanLines(['{', '{', '{'], () => {}, { maxIssues: 2 });
    expect(r.quality.invalidJson).toHaveLength(2);
    expect(r.quality.counts.invalidJson).toBe(3);
  });

  it('counts interleaved scenarios as separate instances', async () => {
    const lines = toJsonl([
      ev('ACC/BRAKE_PEDAL', 0, 'A', { pressed: false }),
      ev('ACC/BRAKE_PEDAL', 0, 'B', { pressed: false }),
      scenarioEnd('A', 10),
      scenarioEnd('B', 10),
    ]).trimEnd().split('\n');
    const r = await scanLines(lines, () => {});
    expect(r.scenarios).toBe(2);
    expect(r.quality.counts.outOfOrder).toBe(0);
  });
});
