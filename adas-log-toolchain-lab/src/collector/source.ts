import { generate, type GenOptions } from '../generator/generate.ts';
import { readLines } from '../io/jsonl.ts';
import { scanLines, type DataQuality } from '../parser/scan.ts';
import type { AdasEvent } from '../schema/events.ts';
import { paced, realClock, type Clock } from './clock.ts';

/** Receives each valid event; `line` is the raw input line when there is one. */
export type Sink = (e: AdasEvent, line?: string) => void;
export interface SourceResult { events: number; dataQuality: DataQuality['counts'] }
/** A data source pushes events into the shared recorder core. */
export type Source = (sink: Sink) => Promise<SourceResult>;

/** Offline source: stream a JSON Lines file through the parser's scanner (same validation as `parse`). */
export function fileSource(path: string): Source {
  return async (sink) => {
    let current = '';
    async function* capture(lines: AsyncIterable<string>) {
      for await (const line of lines) {
        current = line;
        yield line;
      }
    }
    // scanLines calls onEvent synchronously for the line it just read, so `current` is that line.
    const scan = await scanLines(capture(readLines(path)), (e) => sink(e, current));
    return { events: scan.events, dataQuality: scan.quality.counts };
  };
}

export interface LiveOptions extends GenOptions { speed: number; clock?: Clock }

/** Live source: stream generator output, paced at scenario time / speed. */
export function liveSource(o: LiveOptions): Source {
  return async (sink) => {
    const { events } = generate(o);
    for await (const e of paced(events, o.speed, o.clock ?? realClock)) sink(e);
    return { events: events.length, dataQuality: { invalidJson: 0, schemaErrors: 0, unknownEvents: 0, outOfOrder: 0 } };
  };
}
