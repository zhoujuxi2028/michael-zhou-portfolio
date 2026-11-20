import { describe, expect, it } from 'vitest';
import sample from '../public/summary.json' with { type: 'json' };
import { byModuleOption, byRuleOption, kpis, timelineOption } from '../src/charts.ts';
import { parseSummary, type Summary } from '../src/summary.ts';

function load(): Summary {
  const r = parseSummary(sample);
  if (!r.ok) throw new Error(r.error);
  return r.value;
}

function empty(): Summary {
  const s = load();
  return {
    ...s,
    totals: { events: 0, scenarios: 0, anomalies: 0, p1: 0, p2: 0 },
    byRule: [],
    byModule: [],
    byScenario: [],
    timeline: [],
    dataQuality: { invalidJson: 0, schemaErrors: 0, unknownEvents: 0, outOfOrder: 0 },
    replay: null,
  };
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

describe('byRuleOption', () => {
  it('builds a bar chart with one colored bar per rule', () => {
    const o = byRuleOption(load()) as Any;
    expect(o.xAxis.data).toEqual(['ACC-01', 'AEB-01', 'LKA-01', 'SYS-01']);
    const s = o.series[0];
    expect(s.type).toBe('bar');
    expect(s.data.map((d: Any) => d.value)).toEqual([2, 1, 3, 3]);
    const colors = s.data.map((d: Any) => d.itemStyle.color);
    expect(colors[0]).toBe(colors[1]); // both P1
    expect(colors[0]).not.toBe(colors[2]); // P1 vs P2
    expect(colors[2]).toBe(colors[3]);
  });

  it('handles empty data', () => {
    const o = byRuleOption(empty()) as Any;
    expect(o.xAxis.data).toEqual([]);
    expect(o.series[0].data).toEqual([]);
    expect(o.title.subtext).toMatch(/no anomalies/i);
  });
});

describe('byModuleOption', () => {
  it('builds a pie chart', () => {
    const o = byModuleOption(load()) as Any;
    expect(o.series[0].type).toBe('pie');
    expect(o.series[0].data).toEqual([
      { name: 'ACC', value: 2 },
      { name: 'AEB', value: 1 },
      { name: 'LKA', value: 3 },
      { name: 'SYS', value: 3 },
    ]);
  });

  it('handles empty data', () => {
    const o = byModuleOption(empty()) as Any;
    expect(o.series[0].data).toEqual([]);
    expect(o.title.subtext).toMatch(/no anomalies/i);
  });
});

describe('timelineOption', () => {
  it('builds a line over 5 s buckets labelled in seconds', () => {
    const o = timelineOption(load()) as Any;
    expect(o.xAxis.data).toEqual(['0s', '5s', '10s', '15s']);
    expect(o.series[0].type).toBe('line');
    expect(o.series[0].data).toEqual([1, 0, 4, 4]);
  });

  it('handles empty data', () => {
    const o = timelineOption(empty()) as Any;
    expect(o.xAxis.data).toEqual([]);
    expect(o.series[0].data).toEqual([]);
  });
});

describe('kpis', () => {
  it('computes metric cards', () => {
    const k = kpis(load());
    const get = (label: string) => k.find((c) => c.label === label)?.value;
    expect(get('Anomalies')).toBe('9');
    expect(get('P1')).toBe('3');
    expect(get('P2')).toBe('6');
    expect(get('Data quality issues')).toBe('6');
    expect(get('Replay pass rate')).toBe('91.7%');
  });

  it('shows n/a for a missing replay and zeros for empty data', () => {
    const k = kpis(empty());
    const get = (label: string) => k.find((c) => c.label === label)?.value;
    expect(get('Replay pass rate')).toBe('n/a');
    expect(get('Anomalies')).toBe('0');
    expect(get('Data quality issues')).toBe('0');
  });
});
