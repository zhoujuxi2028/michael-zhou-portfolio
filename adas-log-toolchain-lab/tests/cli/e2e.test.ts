import { spawnSync } from 'node:child_process';
import { existsSync, mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const root = fileURLToPath(new URL('../..', import.meta.url));
const cli = (args: string[]) => spawnSync(process.execPath, ['src/cli.ts', ...args], { cwd: root, encoding: 'utf8' });

describe('adas-lab (subprocess)', () => {
  it('runs gen -> parse -> cases boundary end to end', () => {
    const dir = mkdtempSync(join(tmpdir(), 'adas-e2e-'));
    expect(cli(['gen', '--out', dir]).status).toBe(0);
    const parse = cli(['parse', join(dir, 'events.jsonl'), '--out', join(dir, 'anomalies.json'), '--fail-on', 'P1']);
    expect(parse.status).toBe(1);
    expect(parse.stdout).toContain('Anomalies: 16');
    expect(cli(['cases', 'boundary', '--out', join(dir, 'cases')]).status).toBe(0);
    expect(existsSync(join(dir, 'cases', 'boundary', 'expected.json'))).toBe(true);
  }, 30000);

  it('exits 2 for a missing input file', () => {
    const r = cli(['parse', 'does-not-exist.jsonl']);
    expect(r.status).toBe(2);
    expect(r.stderr).toContain('input file not found');
  });
});
