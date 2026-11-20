import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { generate } from '../../src/generator/generate.ts';
import { toJsonl } from '../../src/io/jsonl.ts';
import { parseFile } from '../../src/parser/parse.ts';

describe('performance smoke', () => {
  it('parses >= 100k events in under 5 s', async () => {
    const { events } = generate({ seed: 7, scenarios: 150 });
    expect(events.length).toBeGreaterThanOrEqual(100_000);
    const file = join(mkdtempSync(join(tmpdir(), 'adas-perf-')), 'big.jsonl');
    writeFileSync(file, toJsonl(events));
    const t0 = performance.now();
    const { report } = await parseFile(file);
    const ms = performance.now() - t0;
    expect(report.meta.events).toBe(events.length);
    expect(ms).toBeLessThan(5000);
  });

  it('keeps rule state bounded regardless of file size', async () => {
    for (const scenarios of [20, 80]) {
      const file = join(mkdtempSync(join(tmpdir(), 'adas-perf-')), 'seq.jsonl');
      writeFileSync(file, toJsonl(generate({ seed: 1, scenarios }).events));
      const { peakActiveScenarios } = await parseFile(file);
      expect(peakActiveScenarios).toBe(1);
    }
  });
});
