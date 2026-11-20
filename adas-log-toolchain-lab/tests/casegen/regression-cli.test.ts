import { existsSync, mkdirSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { main } from '../../src/cli/main.ts';
import { toJsonl } from '../../src/io/jsonl.ts';
import { fixture, fixtureEvents } from './regression-fixture.ts';

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

  it('replaces previous data/ and generated/ output but leaves other files under --out alone', async () => {
    const { dir, log, anomalies } = await fixture(), out = join(dir, 'cases');
    mkdirSync(join(out, 'data'), { recursive: true });
    mkdirSync(join(out, 'generated'), { recursive: true });
    mkdirSync(join(out, 'keep'), { recursive: true });
    writeFileSync(join(out, 'data', 'REG-STALE.jsonl'), '{}\n');
    writeFileSync(join(out, 'generated', 'stale.test.ts'), '');
    writeFileSync(join(out, 'notes.txt'), 'mine');
    writeFileSync(join(out, 'keep', 'x.json'), '{}');
    expect(await main(['cases', 'regression', '--anomalies', anomalies, '--log', log, '--out', out], capture().io)).toBe(0);
    const manifest = JSON.parse(readFileSync(join(out, 'manifest.json'), 'utf8')) as { cases: { source: string }[] };
    expect(readdirSync(join(out, 'data')).sort()).toEqual(manifest.cases.map((c) => c.source.slice('data/'.length)).sort());
    expect(readdirSync(join(out, 'generated'))).toEqual(['regression.test.ts']);
    expect(readFileSync(join(out, 'notes.txt'), 'utf8')).toBe('mine');
    expect(existsSync(join(out, 'keep', 'x.json'))).toBe(true);
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

  it('drops log lines whose scenarioId is a path and never writes outside --out', async () => {
    const { dir, log, anomalies } = await fixture();
    const evil = 'x/../../../../escaped';
    writeFileSync(log, toJsonl(fixtureEvents.map((e) => ({ ...e, scenarioId: evil }))));
    const out = join(dir, 'a', 'b', 'cases');
    expect(await main(['parse', log, '--out', anomalies], capture().io)).toBe(0);
    const report = JSON.parse(readFileSync(anomalies, 'utf8')) as { anomalies: unknown[]; dataQuality: { counts: { schemaErrors: number } } };
    expect(report.anomalies).toEqual([]);
    expect(report.dataQuality.counts.schemaErrors).toBe(fixtureEvents.length);
    expect(await main(['cases', 'regression', '--anomalies', anomalies, '--log', log, '--out', out], capture().io)).toBe(0);
    expect(readdirSync(join(out, 'data'))).toEqual([]);
  });

  it.each([
    ['scenarioId', { scenarioId: '../../../escaped' }, /scenarioId/],
    ['ruleId', { ruleId: '/../../../../escaped' }, /escapes the output directory/],
  ])('exits 2 and writes nothing outside --out for a crafted %s in anomalies.json', async (_n, patch, msg) => {
    const { dir, log, anomalies } = await fixture();
    const r = JSON.parse(readFileSync(anomalies, 'utf8')) as { anomalies: Record<string, unknown>[] };
    Object.assign(r.anomalies[0]!, patch);
    writeFileSync(anomalies, JSON.stringify(r));
    const out = join(dir, 'a', 'b', 'cases'), c = capture();
    expect(await main(['cases', 'regression', '--anomalies', anomalies, '--log', log, '--out', out], c.io)).toBe(2);
    expect(c.err.join('\n')).toMatch(msg);
    expect(readdirSync(dir).sort()).toEqual(['anomalies.json', 'events.jsonl']);
    expect(existsSync(join(out, 'manifest.json'))).toBe(false);
  });
});
