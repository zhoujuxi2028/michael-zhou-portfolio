import { existsSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { main } from '../../src/cli/main.ts';
import { scenarioEnd } from '../../src/schema/events.ts';

function capture() {
  const out: string[] = [], err: string[] = [];
  return { io: { out: (s: string) => out.push(s), err: (s: string) => err.push(s) }, out, err };
}
const tmp = () => mkdtempSync(join(tmpdir(), 'adas-cli-'));

describe('adas-lab CLI', () => {
  it('gen writes events and labels', async () => {
    const dir = tmp(), c = capture();
    expect(await main(['gen', '--out', dir, '--seed', '7'], c.io)).toBe(0);
    expect(existsSync(join(dir, 'events.jsonl'))).toBe(true);
    expect(JSON.parse(readFileSync(join(dir, 'labels.json'), 'utf8'))).toHaveLength(16);
    expect(c.out.join('\n')).toMatch(/Generated \d+ events in 20 scenarios \(16 injected faults\)/);
  });

  it('parse writes a report and applies --fail-on', async () => {
    const dir = tmp();
    await main(['gen', '--out', dir], capture().io);
    const input = join(dir, 'events.jsonl'), report = join(dir, 'anomalies.json');
    const c = capture();
    expect(await main(['parse', input, '--out', report], c.io)).toBe(0);
    expect(JSON.parse(readFileSync(report, 'utf8')).anomalies).toHaveLength(16);
    expect(c.out.join('\n')).toContain('Anomalies: 16 (P1: 10, P2: 6)');
    expect(await main(['parse', input, '--out', report, '--fail-on', 'P1'], capture().io)).toBe(1);
    expect(await main(['parse', input, '--out', report, '--fail-on', 'P2'], capture().io)).toBe(1);
  });

  it('--strict fails on data quality issues only', async () => {
    const dir = tmp(), input = join(dir, 'in.jsonl'), report = join(dir, 'r.json');
    writeFileSync(input, `{bad\n${JSON.stringify(scenarioEnd('S', 0))}\n`);
    expect(await main(['parse', input, '--out', report], capture().io)).toBe(0);
    expect(await main(['parse', input, '--out', report, '--strict'], capture().io)).toBe(1);
    writeFileSync(input, `${JSON.stringify(scenarioEnd('S', 0))}\n`);
    expect(await main(['parse', input, '--out', report, '--strict', '--fail-on', 'P1'], capture().io)).toBe(0);
  });

  it('cases boundary writes and verifies cases', async () => {
    const dir = tmp(), c = capture();
    expect(await main(['cases', 'boundary', '--out', dir], c.io)).toBe(0);
    expect(existsSync(join(dir, 'boundary', 'expected.json'))).toBe(true);
    expect(c.out.join('\n')).toContain('38 passed, 0 failed');
  });

  it.each([
    [['parse', '/nonexistent.jsonl'], /input file not found/],
    [['parse', 'x.jsonl', '--fail-on', 'P9'], /P9/],
    [['gen', '--scenarios', '3'], /scenarios must be an integer >= 8/],
    [['gen', '--seed', 'abc'], /must be an integer/],
    [['gen', '--seed', '1.5'], /must be an integer/],
    [['parse', 'fixtures/golden/mixed.jsonl', '--config', '/nonexistent.json'], /config file not found/],
    [['unknown-command'], /unknown command/],
  ])('exits 2 on usage error: %j', async (args, msg) => {
    const c = capture();
    expect(await main(args, c.io)).toBe(2);
    expect(c.err.join('\n')).toMatch(msg);
  });

  it('prints the version', async () => {
    const c = capture();
    expect(await main(['--version'], c.io)).toBe(0);
    expect(c.out.join('\n')).toContain('0.1.0');
  });
});
