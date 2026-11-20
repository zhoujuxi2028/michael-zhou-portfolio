import { describe, expect, it } from 'vitest';
import { matchLabels } from '../../src/eval/match.ts';
import type { Anomaly } from '../../src/engine/types.ts';

const a = (scenarioId: string, ruleId: string, startTs: number, endTs: number): Anomaly => ({
  ruleId, module: 'ACC', severity: 'P1', scenarioId, startTs, endTs, detectTs: endTs, message: '', evidence: {},
});
const l = (scenarioId: string, ruleId: string, startTs: number, endTs: number) => ({ scenarioId, ruleId, startTs, endTs });

describe('matchLabels', () => {
  it('matches on scenario + rule + closed-interval overlap', () => {
    const r = matchLabels([a('S1', 'ACC-01', 100, 200)], [l('S1', 'ACC-01', 200, 300)]);
    expect(r).toMatchObject({ matched: 1, precision: 1, recall: 1 });
  });

  it('does not match a different rule, scenario, or a disjoint interval', () => {
    const r = matchLabels(
      [a('S1', 'ACC-01', 0, 10), a('S2', 'ACC-01', 0, 10), a('S1', 'ACC-02', 0, 10)],
      [l('S1', 'ACC-01', 11, 20)],
    );
    expect(r).toMatchObject({ matched: 0, precision: 0, recall: 0 });
    expect(r.unmatchedAnomalies).toHaveLength(3);
    expect(r.unmatchedLabels).toHaveLength(1);
  });

  it('is one-to-one', () => {
    const r = matchLabels([a('S1', 'ACC-01', 0, 100)], [l('S1', 'ACC-01', 0, 10), l('S1', 'ACC-01', 50, 60)]);
    expect(r).toMatchObject({ matched: 1, precision: 1, recall: 0.5 });
  });

  it('finds the optimal assignment where greedy matching would miss', () => {
    const r = matchLabels(
      [a('S1', 'ACC-01', 0, 100), a('S1', 'ACC-01', 50, 60)],
      [l('S1', 'ACC-01', 50, 60), l('S1', 'ACC-01', 0, 10)],
    );
    expect(r).toMatchObject({ matched: 2, precision: 1, recall: 1 });
    expect(r.unmatchedAnomalies).toEqual([]);
    expect(r.unmatchedLabels).toEqual([]);
  });

  it('defines precision and recall as 1 for empty sets', () => {
    expect(matchLabels([], [])).toMatchObject({ precision: 1, recall: 1 });
  });
});
