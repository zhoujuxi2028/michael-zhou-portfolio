import type { Anomaly } from '../engine/types.ts';
import type { Label } from '../generator/generate.ts';

export interface MatchResult {
  matched: number;
  precision: number;
  recall: number;
  unmatchedAnomalies: Anomaly[];
  unmatchedLabels: Label[];
}

const overlaps = (a: Anomaly, l: Label) =>
  a.scenarioId === l.scenarioId && a.ruleId === l.ruleId && a.startTs <= l.endTs && l.startTs <= a.endTs;

/** One-to-one greedy matching (labels in order, first overlapping unmatched anomaly). */
export function matchLabels(anomalies: readonly Anomaly[], labels: readonly Label[]): MatchResult {
  const used = new Set<number>();
  const unmatchedLabels: Label[] = [];
  for (const l of labels) {
    const i = anomalies.findIndex((a, idx) => !used.has(idx) && overlaps(a, l));
    if (i === -1) unmatchedLabels.push(l);
    else used.add(i);
  }
  const matched = used.size;
  return {
    matched,
    precision: anomalies.length === 0 ? 1 : matched / anomalies.length,
    recall: labels.length === 0 ? 1 : matched / labels.length,
    unmatchedAnomalies: anomalies.filter((_, idx) => !used.has(idx)),
    unmatchedLabels,
  };
}
