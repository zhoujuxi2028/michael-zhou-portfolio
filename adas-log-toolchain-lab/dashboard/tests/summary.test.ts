import { describe, expect, it } from 'vitest';
import sample from '../public/summary.json' with { type: 'json' };
import { parseSummary } from '../src/summary.ts';

describe('parseSummary', () => {
  it('accepts the sample summary', () => {
    const r = parseSummary(sample);
    expect(r.ok).toBe(true);
    if (r.ok) expect(r.value.totals.anomalies).toBe(9);
  });

  it('accepts replay: null', () => {
    const r = parseSummary({ ...sample, replay: null, meta: { ...sample.meta, replay: null } });
    expect(r.ok).toBe(true);
    if (r.ok) expect(r.value.replay).toBeNull();
  });

  it('rejects non-objects', () => {
    for (const bad of [null, 42, 'x', []]) expect(parseSummary(bad).ok).toBe(false);
  });

  it('reports missing top-level fields', () => {
    const rest: Record<string, unknown> = { ...sample };
    delete rest.totals;
    const r = parseSummary(rest);
    expect(r.ok).toBe(false);
    if (!r.ok) expect(r.error).toContain('totals');
  });

  it('reports wrong nested types with a path', () => {
    const bad = { ...sample, byRule: [{ ruleId: 'X', module: 'M', severity: 'P1', count: 'many' }] };
    const r = parseSummary(bad);
    expect(r.ok).toBe(false);
    if (!r.ok) expect(r.error).toContain('byRule[0].count');
  });

  it('rejects unknown severity', () => {
    const bad = { ...sample, byRule: [{ ruleId: 'X', module: 'M', severity: 'P9', count: 1 }] };
    expect(parseSummary(bad).ok).toBe(false);
  });

  it('rejects a malformed replay object', () => {
    const r = parseSummary({ ...sample, replay: { total: 1 } });
    expect(r.ok).toBe(false);
    if (!r.ok) expect(r.error).toContain('replay');
  });
});
