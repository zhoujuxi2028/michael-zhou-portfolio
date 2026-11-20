import { existsSync, mkdtempSync, readFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { main } from '../../src/cli/main.ts';

function capture() {
  const out: string[] = [], err: string[] = [];
  return { io: { out: (s: string) => out.push(s), err: (s: string) => err.push(s) }, out, err };
}
const tmp = () => mkdtempSync(join(tmpdir(), 'adas-record-cli-'));
const FIXTURE = 'fixtures/collector/drive.jsonl';

describe('adas-lab record', () => {
  it('records clips from a file and prints a summary', async () => {
    const out = join(tmp(), 'clips'), c = capture();
    expect(await main(['record', '--source', 'file', '--input', FIXTURE, '--out', out], c.io)).toBe(0);
    expect(existsSync(join(out, 'index.json'))).toBe(true);
    expect(existsSync(join(out, 'quality.json'))).toBe(true);
    expect(c.out).toEqual([
      `Recorded 1482 events in 2 scenarios from ${FIXTURE}`,
      'Clips: 5 (rule 4, manual 1, truncated 1)',
      'Quality: TRACK frames 799, dropped 2 (0.25%), duplicates 1, ts anomalies 1',
      'Skipped lines: invalid JSON 1, schema errors 0, unknown events 0, out of order 0',
      `Clips written to ${out}`,
    ]);
  });

  it('passes buffer / pre / post through to the index', async () => {
    const out = tmp();
    const args = ['record', '--input', FIXTURE, '--out', out, '--buffer', '20000', '--pre', '1000', '--post', '2000'];
    expect(await main(args, capture().io)).toBe(0);
    const index = JSON.parse(readFileSync(join(out, 'index.json'), 'utf8'));
    expect(index.meta).toMatchObject({ source: FIXTURE, bufferMs: 20000, preMs: 1000, postMs: 2000 });
  });

  it('accepts --pre 0 and --post 0 (same as cases regression)', async () => {
    const out = tmp();
    const args = ['record', '--input', FIXTURE, '--out', out, '--pre', '0', '--post', '0'];
    expect(await main(args, capture().io)).toBe(0);
    const index = JSON.parse(readFileSync(join(out, 'index.json'), 'utf8'));
    expect(index.meta).toMatchObject({ preMs: 0, postMs: 0 });
    expect(index.clips.length).toBeGreaterThan(0);
  });

  it('records from the live generator stream', async () => {
    const out = tmp(), c = capture();
    const args = ['record', '--source', 'live', '--seed', '7', '--scenarios', '8', '--speed', '1000000', '--out', out];
    expect(await main(args, c.io)).toBe(0);
    expect(JSON.parse(readFileSync(join(out, 'index.json'), 'utf8')).meta.source).toBe('live');
    expect(c.out[0]).toMatch(/^Recorded \d+ events in 8 scenarios from live \(seed 7, speed 1000000x\)$/);
  });

  it.each([
    [['--source', 'file'], 'requires --input'],
    [['--source', 'file', '--input', 'missing.jsonl'], 'input file not found'],
    [['--source', 'live', '--input', FIXTURE], '--input is only valid with --source file'],
    [['--source', 'bus', '--input', FIXTURE], 'Allowed choices are file, live'],
    [['--input', FIXTURE, '--buffer', '0'], 'must be a positive integer'],
    [['--input', FIXTURE, '--pre', '-5'], 'must be a non-negative integer'],
    [['--input', FIXTURE, '--pre', '1.5'], 'must be a non-negative integer'],
    [['--input', FIXTURE, '--post', 'abc'], 'must be a non-negative integer'],
    [['--input', FIXTURE, '--buffer', '-1'], 'must be a positive integer'],
    [['--source', 'live', '--speed', '0'], 'must be a positive number'],
    [['--source', 'live', '--scenarios', '3'], 'scenarios must be an integer >= 8'],
  ])('exits 2 on usage error %j', async (args, message) => {
    const c = capture();
    expect(await main(['record', '--out', tmp(), ...args], c.io)).toBe(2);
    expect(c.err.join('\n')).toContain(message);
  });
});
