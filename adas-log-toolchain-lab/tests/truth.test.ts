import { describe, expect, it } from 'vitest';
import { matchLabels } from '../src/eval/match.ts';
import { generate } from '../src/generator/generate.ts';
import { toJsonl } from '../src/io/jsonl.ts';
import { parseLines } from '../src/parser/parse.ts';

async function evaluate(seed: number, interleave: boolean) {
  const { events, labels } = generate({ seed, scenarios: 20, interleave });
  const lines = toJsonl(events).trimEnd().split('\n');
  const { report } = await parseLines(lines, `seed-${seed}`);
  return { report, result: matchLabels(report.anomalies, labels) };
}

describe('truth labels (spec §6.4)', () => {
  it('seed 42 reaches 100% precision and recall', async () => {
    const { report, result } = await evaluate(42, false);
    expect(result.unmatchedAnomalies).toEqual([]);
    expect(result.unmatchedLabels).toEqual([]);
    expect(result).toMatchObject({ precision: 1, recall: 1, matched: 12 });
    expect(result.pairs).toHaveLength(12);
    for (const { anomaly, label } of result.pairs) {
      expect([anomaly.startTs, anomaly.endTs]).toEqual([label.startTs, label.endTs]);
    }
    expect(report.dataQuality.counts).toEqual({ invalidJson: 0, schemaErrors: 0, unknownEvents: 0, outOfOrder: 0 });
  });

  it('interleaved scenarios give the same result', async () => {
    const { result } = await evaluate(42, true);
    expect(result).toMatchObject({ precision: 1, recall: 1, matched: 12 });
  });

  it.each([1, 2, 3, 7, 2026])('seed %i stays at 100%', async (seed) => {
    const { result } = await evaluate(seed, false);
    expect(result).toMatchObject({ precision: 1, recall: 1, matched: 12 });
  });
});
