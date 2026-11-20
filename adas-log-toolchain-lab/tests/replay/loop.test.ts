import { mkdtempSync, readFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
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
});
