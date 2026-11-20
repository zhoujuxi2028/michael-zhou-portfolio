import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { loadConfig, UsageError } from '../src/config.ts';

const write = (content: string) => {
  const p = join(mkdtempSync(join(tmpdir(), 'adas-cfg-')), 'rules.config.json');
  writeFileSync(p, content);
  return p;
};

describe('loadConfig', () => {
  it('loads rule overrides and epsilon', async () => {
    const p = write('{"rules":{"ACC-01":{"timeGapS":1.2}},"epsilon":{"time":2}}');
    expect(await loadConfig(p)).toEqual({ rules: { 'ACC-01': { timeGapS: 1.2 } }, epsilon: { time: 2 } });
  });

  it.each([
    ['missing file', '/nonexistent/rules.json', /config file not found/],
    ['invalid JSON', null, /not valid JSON/],
  ])('rejects %s', async (_name, path, msg) => {
    await expect(loadConfig(path ?? write('{'))).rejects.toThrow(msg);
  });

  it('rejects unknown keys and non-numeric thresholds', async () => {
    await expect(loadConfig(write('{"rule":{}}'))).rejects.toBeInstanceOf(UsageError);
    await expect(loadConfig(write('{"rules":{"ACC-01":{"timeGapS":"x"}}}'))).rejects.toThrow(/invalid config/);
  });

  it('rejects overrides for unknown rule ids and unknown keys', async () => {
    await expect(loadConfig(write('{"rules":{"NOPE-99":{"x":1}}}'))).rejects.toThrow(/unknown rule id "NOPE-99"/);
    await expect(loadConfig(write('{"rules":{"ACC-01":{"bogusKey":1}}}'))).rejects.toThrow(/unknown key "bogusKey" for rule ACC-01/);
  });
});
