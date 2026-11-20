import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { afterAll, describe, expect, it } from 'vitest';
import { RULES } from '../../src/rules/index.ts';
import { resolveEpsilon, resolveRuleConfig } from '../../src/engine/engine.ts';
import { generateTasks } from '../../src/ticket/tasks.ts';
import type { Ticket } from '../../src/ticket/parse.ts';

const root = fileURLToPath(new URL('../..', import.meta.url));
const dirs: string[] = [];
const tmp = () => { mkdirSync(join(root, 'out'), { recursive: true }); const d = mkdtempSync(join(root, 'out', 'tk-')); dirs.push(d); return d; };
afterAll(() => { for (const d of dirs) rmSync(d, { recursive: true, force: true }); });
const tickets: Ticket[] = [
  { key: 'ADAS-1', summary: 'hmi', description: 'Steps:\n# a\nExpected:\n# b', components: ['HMI'], labels: ['rule:HMI-02'] },
  { key: 'ADAS-2', summary: 'acc', description: '', components: ['ACC'], labels: ['rule:ACC-01'] },
  { key: 'ADAS-3', summary: 'none', description: '', components: ['ACC'], labels: [] },
];
const readJson = (p: string) => JSON.parse(readFileSync(p, 'utf8')) as unknown;

describe('generateTasks', () => {
  it('writes task json, data files, generated tests and unmapped.json', async () => {
    const out = tmp();
    const r = await generateTasks(tickets, out);
    expect(r).toEqual({ mapped: 2, unmapped: 1 });
    expect(readJson(join(out, 'unmapped.json'))).toEqual([{ key: 'ADAS-3', reason: 'no rule label' }]);
    const task = readJson(join(out, 'ADAS-1.json')) as { cases: string[]; testFile: string; steps: string[]; ruleId: string };
    const rule = RULES.find((x) => x.id === 'HMI-02')!;
    const expectedIds = rule.boundaryCases(resolveRuleConfig(rule, {}), resolveEpsilon({})).filter((c) => c.expectTriggered).map((c) => c.caseId);
    expect(task).toMatchObject({ key: 'ADAS-1', summary: 'hmi', module: 'HMI', ruleId: 'HMI-02', steps: ['a'], expected: ['b'], testFile: 'generated/ADAS-1.test.ts' });
    expect(task.cases).toEqual(expectedIds);
    expect(expectedIds.length).toBeGreaterThan(0);
    for (const id of expectedIds) expect(existsSync(join(out, 'data', `ADAS-1-${id}.jsonl`))).toBe(true);
    expect(readdirSync(join(out, 'generated')).sort()).toEqual(['ADAS-1.test.ts', 'ADAS-2.test.ts']);
    expect(existsSync(join(out, 'ADAS-3.json'))).toBe(false);
  });

  it('removes stale outputs from a previous run but leaves unrelated files', async () => {
    const out = tmp();
    await generateTasks(tickets, out);
    writeFileSync(join(out, 'keep.txt'), 'x');
    await generateTasks(tickets.slice(1, 2), out);
    expect(readdirSync(join(out, 'generated'))).toEqual(['ADAS-2.test.ts']);
    expect(readdirSync(join(out, 'data')).every((f) => f.startsWith('ADAS-2-'))).toBe(true);
    expect(existsSync(join(out, 'ADAS-1.json'))).toBe(false);
    expect(readJson(join(out, 'unmapped.json'))).toEqual([]);
    expect(existsSync(join(out, 'keep.txt'))).toBe(true);
  });

  it('writes an empty unmapped.json when everything maps', async () => {
    const out = tmp();
    await generateTasks(tickets.slice(0, 2), out);
    expect(readJson(join(out, 'unmapped.json'))).toEqual([]);
  });

  it('generated scripts type-check and run green; a broken expectation fails', async () => {
    const out = tmp();
    await generateTasks(tickets.slice(0, 2), out);
    const gen = join(out, 'generated');
    writeFileSync(join(gen, 'tsconfig.json'), JSON.stringify({ extends: relative(gen, join(root, 'tsconfig.json')), include: ['*.ts'] }));
    const tsc = spawnSync('npx', ['tsc', '--noEmit', '-p', join(gen, 'tsconfig.json')], { cwd: root, encoding: 'utf8' });
    expect(tsc.stdout + tsc.stderr).toBe('');
    const run = (f: string) => spawnSync('npx', ['vitest', 'run', '--pool=threads', '--maxWorkers=1', '--config', 'vitest.generated.config.ts', join(gen, f)], { cwd: root, encoding: 'utf8' }).status;
    expect(run('ADAS-1.test.ts')).toBe(0);
    expect(run('ADAS-2.test.ts')).toBe(0);
    const p = join(gen, 'ADAS-1.test.ts');
    writeFileSync(p, readFileSync(p, 'utf8').replaceAll('"expectTriggered": true', '"expectTriggered": false'));
    expect(run('ADAS-1.test.ts')).toBe(1);
  }, 90_000);
});
