import { existsSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { main } from '../../src/cli/main.ts';

const fixture = fileURLToPath(new URL('../../fixtures/jira/export.json', import.meta.url));
const capture = () => { const out: string[] = [], err: string[] = []; return { io: { out: (s: string) => out.push(s), err: (s: string) => err.push(s) }, out, err }; };
const tmp = () => mkdtempSync(join(tmpdir(), 'adas-ticket-'));

describe('adas-lab ticket', () => {
  it('maps the fixture: 3 mapped, one unmapped per reason', async () => {
    const dir = tmp(), c = capture();
    expect(await main(['ticket', fixture, '--out', dir], c.io)).toBe(0);
    expect(c.out.join('\n')).toContain('6 tickets: 3 mapped, 3 unmapped');
    const unmapped = JSON.parse(readFileSync(join(dir, 'unmapped.json'), 'utf8')) as { key: string; reason: string }[];
    expect(unmapped.map((u) => u.reason).sort()).toEqual(['component does not match rule module', 'no rule label', 'unknown rule id']);
    const mods = ['ADAS-101', 'ADAS-102', 'ADAS-103'].map((k) => (JSON.parse(readFileSync(join(dir, `${k}.json`), 'utf8')) as { module: string; steps: string[]; expected: string[] }));
    expect(mods.map((m) => m.module)).toEqual(['ACC', 'APA', 'HMI']);
    for (const m of mods) { expect(m.steps.length).toBeGreaterThan(0); expect(m.expected.length).toBeGreaterThan(0); }
  });
  it('--strict exits 1 when something is unmapped', async () => {
    expect(await main(['ticket', fixture, '--out', tmp(), '--strict'], capture().io)).toBe(1);
  });
  it('--strict exits 0 when everything maps', async () => {
    const dir = tmp(), f = join(dir, 'e.json');
    writeFileSync(f, JSON.stringify({ issues: [{ key: 'A-1', fields: { summary: 's', components: [{ name: 'HMI' }], labels: ['rule:HMI-02'] } }] }));
    expect(await main(['ticket', f, '--out', join(dir, 'o'), '--strict'], capture().io)).toBe(0);
  });
  it('exits 2 for missing or invalid input without writing output', async () => {
    const dir = tmp(), bad = join(dir, 'bad.json'), c = capture();
    writeFileSync(bad, '{"issues": 3}');
    expect(await main(['ticket', join(dir, 'nope.json'), '--out', join(dir, 'o')], c.io)).toBe(2);
    expect(await main(['ticket', bad, '--out', join(dir, 'o')], c.io)).toBe(2);
    expect(c.err.join('\n')).toMatch(/not found[\s\S]*invalid ticket export/);
    expect(existsSync(join(dir, 'o'))).toBe(false);
  });
});
