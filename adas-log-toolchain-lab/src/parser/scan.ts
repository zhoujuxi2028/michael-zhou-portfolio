import type { AdasEvent } from '../schema/events.ts';
import { parseLine } from '../schema/validate.ts';

export interface LineIssue { line: number; detail: string }
export interface OutOfOrder { line: number; scenarioId: string; ts: number; prevTs: number }

export interface DataQuality {
  counts: { invalidJson: number; schemaErrors: number; unknownEvents: number; outOfOrder: number };
  invalidJson: LineIssue[];
  schemaErrors: LineIssue[];
  unknownEvents: Record<string, number>;
  outOfOrder: OutOfOrder[];
}

export interface ScanOptions { maxLineBytes?: number; maxIssues?: number }
export interface ScanResult { quality: DataQuality; events: number; scenarios: number }

export const DEFAULT_MAX_LINE_BYTES = 1024 * 1024;
export const DEFAULT_MAX_ISSUES = 100;
/** Ended scenario ids remembered to catch late events (bounded, oldest evicted). */
export const ENDED_MEMORY = 1000;
const UNKNOWN_OTHER = '(other)';
const UNKNOWN_KEY_MAX = 64;

export function hasQualityIssues(q: DataQuality): boolean {
  return Object.values(q.counts).some((n) => n > 0);
}

/**
 * Read lines, validate, record data quality issues, and hand valid events to `onEvent`.
 * Blank lines are ignored. `scenarios` counts scenario instances (an id reused after
 * SCENARIO_END counts again), so per-scenario bookkeeping is released at scenario end.
 * An event for a recently ended scenario with ts earlier than its SCENARIO_END is a late
 * event: it is recorded as out of order and dropped, so it cannot reopen the scenario.
 * A leading UTF-8 BOM on the first line is ignored.
 */
export async function scanLines(
  lines: AsyncIterable<string> | Iterable<string>,
  onEvent: (e: AdasEvent) => void,
  opts: ScanOptions = {},
): Promise<ScanResult> {
  const maxLineBytes = opts.maxLineBytes ?? DEFAULT_MAX_LINE_BYTES;
  const maxIssues = opts.maxIssues ?? DEFAULT_MAX_ISSUES;
  const quality: DataQuality = {
    counts: { invalidJson: 0, schemaErrors: 0, unknownEvents: 0, outOfOrder: 0 },
    invalidJson: [], schemaErrors: [], unknownEvents: {}, outOfOrder: [],
  };
  const keep = <T>(list: T[], item: T) => { if (list.length < maxIssues) list.push(item); };
  const lastTs = new Map<string, number>();
  const endedAt = new Map<string, number>();
  let lineNo = 0, events = 0, scenarios = 0;

  for await (const line of lines) {
    lineNo++;
    const raw = lineNo === 1 && line.charCodeAt(0) === 0xfeff ? line.slice(1) : line;
    if (raw.trim() === '') continue;
    if (Buffer.byteLength(raw, 'utf8') > maxLineBytes) {
      quality.counts.invalidJson++;
      keep(quality.invalidJson, { line: lineNo, detail: `line exceeds ${maxLineBytes} bytes` });
      continue;
    }
    const r = parseLine(raw);
    if (!r.ok) {
      if (r.kind === 'unknownEvent') {
        quality.counts.unknownEvents++;
        // Bound memory: at most `maxIssues` distinct keys, each truncated; the rest share one bucket.
        const name = r.detail.slice(0, UNKNOWN_KEY_MAX);
        const full = Object.keys(quality.unknownEvents).length >= maxIssues;
        const key = Object.hasOwn(quality.unknownEvents, name) || !full ? name : UNKNOWN_OTHER;
        quality.unknownEvents[key] = (quality.unknownEvents[key] ?? 0) + 1;
      } else {
        const key = r.kind === 'invalidJson' ? 'invalidJson' : 'schemaErrors';
        quality.counts[key]++;
        keep(quality[key], { line: lineNo, detail: r.detail });
      }
      continue;
    }
    const e = r.event;
    const prev = lastTs.get(e.scenarioId);
    const endTs = endedAt.get(e.scenarioId);
    if (prev === undefined && endTs !== undefined && e.ts < endTs) {
      quality.counts.outOfOrder++;
      keep(quality.outOfOrder, { line: lineNo, scenarioId: e.scenarioId, ts: e.ts, prevTs: endTs });
      continue;
    }
    if (prev === undefined) { scenarios++; endedAt.delete(e.scenarioId); }
    else if (e.ts < prev) {
      quality.counts.outOfOrder++;
      keep(quality.outOfOrder, { line: lineNo, scenarioId: e.scenarioId, ts: e.ts, prevTs: prev });
    }
    lastTs.set(e.scenarioId, Math.max(prev ?? e.ts, e.ts));
    if (e.module === 'SYS' && e.type === 'SCENARIO_END') {
      endedAt.set(e.scenarioId, lastTs.get(e.scenarioId) ?? e.ts);
      lastTs.delete(e.scenarioId);
      if (endedAt.size > ENDED_MEMORY) endedAt.delete(endedAt.keys().next().value as string);
    }
    events++;
    onEvent(e);
  }
  return { quality, events, scenarios };
}
