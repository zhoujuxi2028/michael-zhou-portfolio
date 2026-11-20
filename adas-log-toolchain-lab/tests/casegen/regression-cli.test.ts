import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { main } from '../../src/cli/main.ts';
import { fixture } from './regression-fixture.ts';

function capture() {
  const out: string[] = [], err: string[] = [];
  return { io: { out: (s: string) => out.push(s), err: (s: string) => err.push(s) }, out, err };
}

describe('adas-lab cases regression', () => {
  it('writes the manifest, data files and generated test', async () => {
    const { dir, log, anomalies } = await fixture(), out = join(dir, 'cases'), c = capture();
    expect(await main(['cases', 'regression', '--anomalies', anomalies, '--log', log, '--out', out, '--pre', '2000', '--post', '1000'], c.io)).toBe(0);
    const manifest = JSON.parse(readFileSync(join(out, 'manifest.json'), 'utf8')) as { meta: { preMs: number; postMs: number }; cases: unknown[] };
    const count = (JSON.parse(readFileSync(anomalies, 'utf8')) as { anomalies: unknown[] }).anomalies.length;
    expect(manifest.cases).toHaveLength(count);
    expect(manifest.meta).toMatchObject({ preMs: 2000, postMs: 1000 });
    expect(existsSync(join(out, 'generated', 'regression.test.ts'))).toBe(true);
    expect(c.out.join('\n')).toBe(`${count} regression cases written to ${join(out, 'manifest.json')}; test file ${join(out, 'generated', 'regression.test.ts')}`);
  });

  it('defaults --pre / --post to 3000 ms', async () => {
    const { dir, log, anomalies } = await fixture(), out = join(dir, 'cases');
    expect(await main(['cases', 'regression', '--anomalies', anomalies, '--log', log, '--out', out], capture().io)).toBe(0);
    expect(JSON.parse(readFileSync(join(out, 'manifest.json'), 'utf8')).meta).toMatchObject({ preMs: 3000, postMs: 3000 });
  });

  it.each([
    [['--log', 'x.jsonl'], /--anomalies/],
    [['--anomalies', '/nonexistent.json', '--log', 'x.jsonl'], /anomalies file not found/],
    [['--anomalies', 'a.json', '--log', 'x.jsonl', '--pre', 'abc'], /must be an integer/],
    [['--anomalies', 'a.json', '--log', 'x.jsonl', '--post', '-1'], /must be >= 0/],
  ])('exits 2 on usage error: %j', async (args, msg) => {
    const c = capture();
    expect(await main(['cases', 'regression', ...args], c.io)).toBe(2);
    expect(c.err.join('\n')).toMatch(msg);
  });
});
