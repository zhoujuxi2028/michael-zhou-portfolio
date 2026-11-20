import { mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { main } from '../../src/cli/main.ts';
import { fixture } from '../casegen/regression-fixture.ts';

function capture() {
  const out: string[] = [], err: string[] = [];
  return { io: { out: (s: string) => out.push(s), err: (s: string) => err.push(s) }, out, err };
}

async function cases(): Promise<{ dir: string; manifest: string }> {
  const { dir, log, anomalies } = await fixture();
  await main(['cases', 'regression', '--anomalies', anomalies, '--log', log, '--out', join(dir, 'cases')], capture().io);
  return { dir, manifest: join(dir, 'cases', 'manifest.json') };
}

describe('adas-lab replay', () => {
  it('writes replay-report.json and exits 0 when every case passes', async () => {
    const { dir, manifest } = await cases(), report = join(dir, 'replay-report.json'), c = capture();
    expect(await main(['replay', '--manifest', manifest, '--out', report], c.io)).toBe(0);
    const r = JSON.parse(readFileSync(report, 'utf8')) as { summary: { total: number; passed: number } };
    expect(r.summary.passed).toBe(r.summary.total);
    expect(c.out.join('\n')).toBe(`Replayed ${r.summary.total} cases: ${r.summary.total} passed, 0 failed (pass rate 100.0%) -> ${report}`);
  });

  it('exits 1 and names failing cases when any case fails', async () => {
    const { dir, manifest } = await cases();
    const m = JSON.parse(readFileSync(manifest, 'utf8')) as { cases: { caseId: string; expect: { triggered: boolean } }[] };
    m.cases[0]!.expect.triggered = false;
    writeFileSync(manifest, JSON.stringify(m));
    const c = capture();
    expect(await main(['replay', '--manifest', manifest, '--out', join(dir, 'r.json')], c.io)).toBe(1);
    expect(c.err.join('\n')).toContain(`FAIL ${m.cases[0]!.caseId}`);
  });

  it('applies --filter module=<M>', async () => {
    const { dir, manifest } = await cases(), report = join(dir, 'r.json');
    const total = (JSON.parse(readFileSync(manifest, 'utf8')) as { cases: unknown[] }).cases.length;
    expect(await main(['replay', '--manifest', manifest, '--out', report, '--filter', 'module=HMI'], capture().io)).toBe(0);
    expect(JSON.parse(readFileSync(report, 'utf8'))).toMatchObject({ meta: { filter: 'module=HMI' }, summary: { total } });
    expect(await main(['replay', '--manifest', manifest, '--out', report, '--filter', 'module=APA'], capture().io)).toBe(0);
    expect(JSON.parse(readFileSync(report, 'utf8'))).toMatchObject({ meta: { filter: 'module=APA' }, summary: { total: 0 } });
  });

  it.each([
    [['replay'], /--manifest/],
    [['replay', '--manifest', '/nonexistent/manifest.json'], /manifest not found/],
    [['replay', '--manifest', 'm.json', '--filter', 'module=XYZ'], /invalid filter/],
  ])('exits 2 on usage error: %j', async (args, msg) => {
    const c = capture();
    expect(await main(args, c.io)).toBe(2);
    expect(c.err.join('\n')).toMatch(msg);
  });

  it('exits 2 for a malformed manifest', async () => {
    const path = join(mkdtempSync(join(tmpdir(), 'adas-replay-cli-')), 'm.json');
    writeFileSync(path, '{"cases": 1}');
    const c = capture();
    expect(await main(['replay', '--manifest', path, '--out', join(path, '..', 'r.json')], c.io)).toBe(2);
    expect(c.err.join('\n')).toMatch(/invalid manifest/);
  });

  it.each(['../../etc/x', '/etc/passwd', 'data/../../x.jsonl'])('exits 2 when a case source escapes the manifest directory: %s', async (source) => {
    const { dir, manifest } = await cases();
    const m = JSON.parse(readFileSync(manifest, 'utf8')) as { cases: { source: string }[] };
    m.cases[0]!.source = source;
    writeFileSync(manifest, JSON.stringify(m));
    const c = capture();
    expect(await main(['replay', '--manifest', manifest, '--out', join(dir, 'r.json')], c.io)).toBe(2);
    expect(c.err.join('\n')).toMatch(/escapes the manifest directory/);
  });
});
