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
const accState = (ts: number, to: 'ACTIVE' | 'STANDBY', sid = S) => ev('ACC/STATE', ts, sid, { from: 'OFF', to, reason: 'r' });
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

  it('prepends the latest state event per dimension before the window and lowers window[0] to cover it', async () => {
    const path = logFile([
      accState(100, 'STANDBY'), ev('LKA/STATE', 200, S, { from: 'OFF', to: 'ACTIVE' }), accState(300, 'ACTIVE'),
      ev('VEH/TURN_SIGNAL', 400, S, { side: 'LEFT' }), ev('APA/STEP', 500, S, { step: 'SEARCHING', reason: 'r' }),
      accState(600, 'ACTIVE', T), track(700), track(5000), accState(5500, 'STANDBY'),
    ]);
    const [w] = await extractWindows(path, [req(S, 4000, 6000)]);
    expect(w!.window).toEqual([200, 6000]);
    expect(w!.events.map((e) => [e.ts, `${e.module}/${e.type}`])).toEqual([
      [200, 'LKA/STATE'], [300, 'ACC/STATE'], [400, 'VEH/TURN_SIGNAL'], [500, 'APA/STEP'], [5000, 'ACC/TRACK'], [5500, 'ACC/STATE'],
    ]);
  });

  it('keeps window[0] when there are no preconditions and ignores states from an earlier ended instance', async () => {
    const path = logFile([accState(100, 'ACTIVE'), scenarioEnd(S, 1000), track(5000), track(6000)]);
    const [w] = await extractWindows(path, [req(S, 4000, 6000)]);
    expect(w!.window).toEqual([4000, 6000]);
    expect(w!.events).toEqual([track(5000), track(6000)]);
  });

  it('skips invalid lines and serves several windows of one scenario in a single pass', async () => {
    const path = logFile([track(1000), '{bad', '{"ts":1}', track(2000), track(8000)]);
    const [a, b] = await extractWindows(path, [req(S, 0, 2500), req(S, 7000, 9000)]);
    expect(a!.events).toEqual([track(1000), track(2000)]);
    expect(b!.events).toEqual([track(8000)]);
  });

  it('writes window events sorted by ts (stable) so an out-of-order line does not dirty the case data', async () => {
    const hmi = ev('HMI/DISPLAY', 5000, S, { feature: 'ACC', item: 'STATE', value: 'ACTIVE' });
    const path = logFile([track(4000), track(5050), track(5000), hmi, track(6000)]);
    const [w] = await extractWindows(path, [req(S, 4000, 6000)]);
    expect(w!.events).toEqual([track(4000), track(5000), hmi, track(5050), track(6000)]);
  });

  it('drops late events of an ended scenario exactly like parse does', async () => {
    const path = logFile([track(1000), scenarioEnd(S, 2000), track(1500), track(3000), scenarioEnd(S, 4000)]);
    const [w] = await extractWindows(path, [req(S, 0, 5000)]);
    expect(w!.events).toEqual([track(1000), scenarioEnd(S, 2000), track(3000), scenarioEnd(S, 4000)]);
  });
});
