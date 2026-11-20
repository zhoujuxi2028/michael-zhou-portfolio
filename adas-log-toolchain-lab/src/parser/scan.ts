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

export function hasQualityIssues(q: DataQuality): boolean {
  return Object.values(q.counts).some((n) => n > 0);
}

/**
 * Read lines, validate, record data quality issues, and hand valid events to `onEvent`.
 * Blank lines are ignored. `scenarios` counts scenario instances (an id reused after
 * SCENARIO_END counts again), so per-scenario bookkeeping is released at scenario end.
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
  let lineNo = 0, events = 0, scenarios = 0;

  for await (const raw of lines) {
    lineNo++;
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
        quality.unknownEvents[r.detail] = (quality.unknownEvents[r.detail] ?? 0) + 1;
      } else {
        const key = r.kind === 'invalidJson' ? 'invalidJson' : 'schemaErrors';
        quality.counts[key]++;
        keep(quality[key], { line: lineNo, detail: r.detail });
      }
      continue;
    }
    const e = r.event;
    const prev = lastTs.get(e.scenarioId);
    if (prev === undefined) scenarios++;
    else if (e.ts < prev) {
      quality.counts.outOfOrder++;
      keep(quality.outOfOrder, { line: lineNo, scenarioId: e.scenarioId, ts: e.ts, prevTs: prev });
    }
    lastTs.set(e.scenarioId, Math.max(prev ?? e.ts, e.ts));
    if (e.module === 'SYS' && e.type === 'SCENARIO_END') lastTs.delete(e.scenarioId);
    events++;
    onEvent(e);
  }
  return { quality, events, scenarios };
}
