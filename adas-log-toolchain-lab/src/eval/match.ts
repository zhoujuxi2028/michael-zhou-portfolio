import type { Anomaly } from '../engine/types.ts';
import type { Label } from '../generator/generate.ts';

export interface MatchResult {
  matched: number;
  precision: number;
  recall: number;
  unmatchedAnomalies: Anomaly[];
  unmatchedLabels: Label[];
  /** The matched (anomaly, label) pairs of the one-to-one assignment. */
  pairs: { anomaly: Anomaly; label: Label }[];
}

const overlaps = (a: Anomaly, l: Label) => a.startTs <= l.endTs && l.startTs <= a.endTs;
const groupKey = (x: { scenarioId: string; ruleId: string }) => `${x.scenarioId}|${x.ruleId}`;

/** Kuhn's augmenting-path maximum bipartite matching; returns label index -> anomaly index. */
function maxMatching(anomalies: readonly Anomaly[], labels: readonly Label[]): Map<number, number> {
  const anomalyOwner = new Map<number, number>(); // anomaly idx -> label idx
  const tryAssign = (li: number, seen: Set<number>): boolean => {
    for (let ai = 0; ai < anomalies.length; ai++) {
      if (seen.has(ai) || !overlaps(anomalies[ai]!, labels[li]!)) continue;
      seen.add(ai);
      const owner = anomalyOwner.get(ai);
      if (owner === undefined || tryAssign(owner, seen)) {
        anomalyOwner.set(ai, li);
        return true;
      }
    }
    return false;
  };
  for (let li = 0; li < labels.length; li++) tryAssign(li, new Set());
  return new Map([...anomalyOwner].map(([ai, li]) => [li, ai]));
}

/**
 * Optimal one-to-one matching on scenario + rule + closed-interval overlap
 * (maximum bipartite matching per scenario/rule group).
 * Precision is defined as 1 when there are no anomalies; recall as 1 when there are no labels.
 */
export function matchLabels(anomalies: readonly Anomaly[], labels: readonly Label[]): MatchResult {
  const groups = new Map<string, { as: number[]; ls: number[] }>();
  const group = (k: string) => groups.get(k) ?? groups.set(k, { as: [], ls: [] }).get(k)!;
  anomalies.forEach((a, i) => group(groupKey(a)).as.push(i));
  labels.forEach((l, i) => group(groupKey(l)).ls.push(i));

  const usedAnomalies = new Set<number>();
  const usedLabels = new Set<number>();
  const pairs: { anomaly: Anomaly; label: Label }[] = [];
  for (const g of groups.values()) {
    const m = maxMatching(g.as.map((i) => anomalies[i]!), g.ls.map((i) => labels[i]!));
    for (const [li, ai] of m) {
      const gl = g.ls[li]!, ga = g.as[ai]!;
      usedLabels.add(gl);
      usedAnomalies.add(ga);
      pairs.push({ anomaly: anomalies[ga]!, label: labels[gl]! });
    }
  }
  const matched = pairs.length;
  return {
    matched,
    precision: anomalies.length === 0 ? 1 : matched / anomalies.length,
    recall: labels.length === 0 ? 1 : matched / labels.length,
    unmatchedAnomalies: anomalies.filter((_, i) => !usedAnomalies.has(i)),
    unmatchedLabels: labels.filter((_, i) => !usedLabels.has(i)),
    pairs,
  };
}
