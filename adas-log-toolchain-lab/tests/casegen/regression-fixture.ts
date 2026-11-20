import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { toJsonl } from '../../src/io/jsonl.ts';
import { parseFile } from '../../src/parser/parse.ts';
import { ev, scenarioEnd } from '../../src/schema/events.ts';

export const takeover = (ts: number, sid: string) => ev('ACC/TAKEOVER_REQUEST', ts, sid, { reason: 'cut-in' });
const track = (ts: number, sid: string) => ev('ACC/TRACK', ts, sid, { egoSpeed: 20, setSpeed: 25, leadDistance: 40, leadRelSpeed: 0 });

/** Two scenarios: S1 a bare HMI-02 fault; S2 an HMI-02 fault 8 s after ACC was activated (shown on HMI). */
export const fixtureEvents = [
  takeover(1000, 'S1'), scenarioEnd('S1', 2000),
  ev('ACC/STATE', 100, 'S2', { from: 'STANDBY', to: 'ACTIVE', reason: 'driver' }),
  ev('HMI/DISPLAY', 150, 'S2', { feature: 'ACC', item: 'STATE', value: 'ACTIVE' }),
  ...[1000, 4000, 6000, 7000].map((ts) => track(ts, 'S2')),
  takeover(8000, 'S2'), track(8500, 'S2'), scenarioEnd('S2', 9000),
];

/** Write the fixture log and its parsed anomalies.json; returns their paths. */
export async function fixture(): Promise<{ dir: string; log: string; anomalies: string }> {
  const dir = mkdtempSync(join(tmpdir(), 'adas-reg-'));
  const log = join(dir, 'events.jsonl'), anomalies = join(dir, 'anomalies.json');
  writeFileSync(log, toJsonl(fixtureEvents));
  writeFileSync(anomalies, JSON.stringify((await parseFile(log)).report));
  return { dir, log, anomalies };
}
