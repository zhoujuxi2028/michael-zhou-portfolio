import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { main } from '../../src/cli/main.ts';

const quiet = { out: () => {}, err: () => {} };
const json = <T>(p: string) => JSON.parse(readFileSync(p, 'utf8')) as T;

describe('closed loop: gen -> parse -> cases regression -> replay', () => {
  it.each([
    [7, false], [42, false], [1234, false], [42, true], [99, true],
  ])('seed %i (interleave %s) replays every anomaly as a passing case', async (seed, interleave) => {
    const dir = mkdtempSync(join(tmpdir(), 'adas-loop-'));
    const log = join(dir, 'events.jsonl'), anomalies = join(dir, 'anomalies.json'), out = join(dir, 'cases');
    const report = join(dir, 'replay-report.json');
    expect(await main(['gen', '--out', dir, '--seed', String(seed), ...(interleave ? ['--interleave'] : [])], quiet)).toBe(0);
    expect(await main(['parse', log, '--out', anomalies], quiet)).toBe(0);
    expect(await main(['cases', 'regression', '--anomalies', anomalies, '--log', log, '--out', out], quiet)).toBe(0);
    expect(await main(['replay', '--manifest', join(out, 'manifest.json'), '--out', report], quiet)).toBe(0);

    const count = json<{ anomalies: unknown[] }>(anomalies).anomalies.length;
    expect(count).toBeGreaterThan(0);
    expect(json<{ cases: unknown[] }>(join(out, 'manifest.json')).cases).toHaveLength(count);
    expect(json<{ summary: unknown }>(report).summary).toEqual({ total: count, passed: count, failed: 0, passRate: 1 });
  });

  it('keeps replay and the generated tests in agreement when the log has an out-of-order line', async () => {
    const root = fileURLToPath(new URL('../..', import.meta.url));
    mkdirSync(join(root, 'out'), { recursive: true });
    const dir = mkdtempSync(join(root, 'out', 'loop-'));
    try {
      const log = join(dir, 'events.jsonl'), anomalies = join(dir, 'anomalies.json'), out = join(dir, 'cases');
      expect(await main(['gen', '--out', dir, '--seed', '42'], quiet)).toBe(0);
      // Swap two adjacent S001 ACC/TRACK lines inside the ACC-01 fault window (15000-16000).
      const lines = readFileSync(log, 'utf8').trimEnd().split('\n');
      const at = (ts: number) => lines.findIndex((l) => {
        const e = JSON.parse(l) as { ts: number; scenarioId: string; type: string };
        return e.scenarioId === 'S001' && e.type === 'TRACK' && e.ts === ts;
      });
      const i = at(15500), j = at(15550);
      expect([i >= 0, j === i + 1]).toEqual([true, true]);
      [lines[i], lines[j]] = [lines[j]!, lines[i]!];
      writeFileSync(log, `${lines.join('\n')}\n`);

      expect(await main(['parse', log, '--out', anomalies], quiet)).toBe(0);
      expect(json<{ dataQuality: { counts: { outOfOrder: number } } }>(anomalies).dataQuality.counts.outOfOrder).toBe(1);
      expect(await main(['cases', 'regression', '--anomalies', anomalies, '--log', log, '--out', out], quiet)).toBe(0);
      const report = join(dir, 'replay-report.json');
      expect(await main(['replay', '--manifest', join(out, 'manifest.json'), '--out', report], quiet)).toBe(0);
      const generated = spawnSync('npx', ['vitest', 'run', '--config', 'vitest.generated.config.ts', out], { cwd: root, encoding: 'utf8' });
      expect(generated.status, generated.stdout).toBe(0);
    } finally {
      rmSync(dir, { recursive: true, force: true });
    }
  }, 60_000);
});
