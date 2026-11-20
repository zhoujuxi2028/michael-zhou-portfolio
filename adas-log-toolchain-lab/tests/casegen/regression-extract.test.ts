import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { extractWindows, type WindowRequest } from '../../src/casegen/extract.ts';
import { ev, scenarioEnd, type AdasEvent } from '../../src/schema/events.ts';

const S = 'S1', T = 'S2';
const track = (ts: number, sid = S) => ev('ACC/TRACK', ts, sid, { egoSpeed: 20, setSpeed: 25, leadDistance: 30, leadRelSpeed: 0 });

function logFile(lines: readonly (AdasEvent | string)[]): string {
  const path = join(mkdtempSync(join(tmpdir(), 'adas-extract-')), 'events.jsonl');
  writeFileSync(path, lines.map((l) => (typeof l === 'string' ? l : JSON.stringify(l))).join('\n') + '\n');
  return path;
}
const req = (scenarioId: string, from: number, to: number): WindowRequest => ({ scenarioId, from, to });

describe('extractWindows', () => {
  it('keeps only the requested scenario events inside the window, in file order', async () => {
    const path = logFile([track(1000), track(5000), track(5000, T), track(6000), track(9000), scenarioEnd(S, 9500), scenarioEnd(T, 9600)]);
    const [w] = await extractWindows(path, [req(S, 4000, 9000)]);
    expect(w!.window).toEqual([4000, 9000]);
    expect(w!.events).toEqual([track(5000), track(6000), track(9000)]);
  });

  it('includes SCENARIO_END when it falls inside the window', async () => {
    const path = logFile([track(1000), scenarioEnd(S, 2000)]);
    const [w] = await extractWindows(path, [req(S, 0, 5000)]);
    expect(w!.events.at(-1)).toEqual(scenarioEnd(S, 2000));
  });
});
