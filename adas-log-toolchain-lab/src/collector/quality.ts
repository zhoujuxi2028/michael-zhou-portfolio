import type { AdasEvent } from '../schema/events.ts';

export const TRACK_PERIOD_MS = 50;
/** A TRACK gap above this many periods counts as dropped frames. */
const DROP_FACTOR = 1.5;
/** Adjacent events in one scenario further apart than this are a ts anomaly. */
export const MAX_GAP_MS = 1000;

export interface QualityCounts {
  trackFrames: number;
  droppedFrames: number;
  /** droppedFrames / (trackFrames + droppedFrames), 0 when there are no frames. */
  dropRate: number;
  duplicates: number;
  tsAnomalies: number;
}

export interface QualityReport {
  trackPeriodMs: number;
  totals: QualityCounts;
  byScenario: (QualityCounts & { scenarioId: string })[];
}

export interface QualityMonitor {
  /** `line` is the raw input line (defaults to the serialized event). */
  push(e: AdasEvent, line?: string): void;
  report(): QualityReport;
  /** Scenarios with live tracking state (released at SCENARIO_END). */
  openScenarios(): number;
}

type Counts = Omit<QualityCounts, 'dropRate'>;
interface Open { maxTs: number; lastTrackTs: number | null; lastLine: string }

const zero = (): Counts => ({ trackFrames: 0, droppedFrames: 0, duplicates: 0, tsAnomalies: 0 });
const withRate = (c: Counts): QualityCounts => {
  const expected = c.trackFrames + c.droppedFrames;
  const dropRate = expected === 0 ? 0 : Math.round((c.droppedFrames / expected) * 10000) / 10000;
  return { trackFrames: c.trackFrames, droppedFrames: c.droppedFrames, dropRate, duplicates: c.duplicates, tsAnomalies: c.tsAnomalies };
};

/** Capture quality per the C3 contract: dropped TRACK frames, exact duplicate lines, ts anomalies. */
export function createQualityMonitor(): QualityMonitor {
  const counts = new Map<string, Counts>();
  const open = new Map<string, Open>();

  return {
    push(e, line = JSON.stringify(e)) {
      let c = counts.get(e.scenarioId);
      if (!c) counts.set(e.scenarioId, (c = zero()));
      const o = open.get(e.scenarioId);
      if (o) {
        if (line === o.lastLine) c.duplicates++;
        if (e.ts < o.maxTs || e.ts - o.maxTs > MAX_GAP_MS) c.tsAnomalies++;
      }
      const next: Open = o ?? { maxTs: e.ts, lastTrackTs: null, lastLine: line };
      if (e.module === 'ACC' && e.type === 'TRACK') {
        c.trackFrames++;
        const gap = next.lastTrackTs === null ? 0 : e.ts - next.lastTrackTs;
        if (gap > DROP_FACTOR * TRACK_PERIOD_MS) c.droppedFrames += Math.round(gap / TRACK_PERIOD_MS) - 1;
        next.lastTrackTs = Math.max(next.lastTrackTs ?? e.ts, e.ts);
      }
      next.maxTs = Math.max(next.maxTs, e.ts);
      next.lastLine = line;
      if (e.module === 'SYS' && e.type === 'SCENARIO_END') open.delete(e.scenarioId);
      else open.set(e.scenarioId, next);
    },
    report() {
      const ids = [...counts.keys()].sort();
      const totals = zero();
      for (const c of counts.values()) {
        totals.trackFrames += c.trackFrames;
        totals.droppedFrames += c.droppedFrames;
        totals.duplicates += c.duplicates;
        totals.tsAnomalies += c.tsAnomalies;
      }
      return {
        trackPeriodMs: TRACK_PERIOD_MS,
        totals: withRate(totals),
        byScenario: ids.map((scenarioId) => ({ scenarioId, ...withRate(counts.get(scenarioId) as Counts) })),
      };
    },
    openScenarios: () => open.size,
  };
}
