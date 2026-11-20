import { createHash } from 'node:crypto';
import { mkdtempSync, readFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { createClipWriter } from '../../src/collector/clip-writer.ts';
import type { Clip } from '../../src/collector/recorder.ts';
import { ev, scenarioEnd } from '../../src/schema/events.ts';
import { RULESET_VERSION, TOOL_VERSION } from '../../src/version.ts';

const clip = (over: Partial<Clip> = {}): Clip => ({
  file: 'S1_1000_T-1.jsonl', scenarioId: 'S1', trigger: 'rule', ruleIds: ['T-1'], startTs: 0, endTs: 2000, truncated: false,
  events: [ev('ACC/BRAKE_PEDAL', 1000, 'S1', { pressed: true }), scenarioEnd('S1', 2000)],
  ...over,
});
const META = { source: 'in.jsonl', bufferMs: 10000, preMs: 5000, postMs: 5000 };
const sha256 = (path: string) => createHash('sha256').update(readFileSync(path)).digest('hex');

describe('clip writer', () => {
  it('writes clip files and an index with versions, event counts and SHA-256 of the file bytes', async () => {
    const dir = mkdtempSync(join(tmpdir(), 'adas-clips-'));
    const w = createClipWriter(dir, META);
    await w.write(clip());
    await w.write(clip({ file: 'S2_0_MANUAL.jsonl', scenarioId: 'S2', trigger: 'manual', ruleIds: [], truncated: true }));
    const index = await w.finish();

    expect(JSON.parse(readFileSync(join(dir, 'index.json'), 'utf8'))).toEqual(index);
    expect(index.meta).toEqual({ toolVersion: TOOL_VERSION, rulesetVersion: RULESET_VERSION, ...META });
    expect(index.clips).toHaveLength(2);
    for (const c of index.clips) {
      const path = join(dir, c.file);
      expect(c.sha256).toBe(sha256(path));
      expect(readFileSync(path, 'utf8').trimEnd().split('\n')).toHaveLength(c.events);
    }
    expect(index.clips[0]).toEqual({
      file: 'S1_1000_T-1.jsonl', scenarioId: 'S1', trigger: 'rule', ruleIds: ['T-1'],
      startTs: 0, endTs: 2000, events: 2, sha256: expect.stringMatching(/^[0-9a-f]{64}$/), truncated: false,
    });
    expect(index.clips[1]).toMatchObject({ trigger: 'manual', truncated: true });
  });

  it('never overwrites a clip that reuses a file name', async () => {
    const dir = mkdtempSync(join(tmpdir(), 'adas-clips-'));
    const w = createClipWriter(dir, META);
    await w.write(clip());
    await w.write(clip());
    const index = await w.finish();
    expect(index.clips.map((c) => c.file)).toEqual(['S1_1000_T-1.jsonl', 'S1_1000_T-1_2.jsonl']);
  });

  it('writes an empty index when nothing was recorded', async () => {
    const dir = join(mkdtempSync(join(tmpdir(), 'adas-clips-')), 'nested');
    const index = await createClipWriter(dir, META).finish();
    expect(index.clips).toEqual([]);
    expect(JSON.parse(readFileSync(join(dir, 'index.json'), 'utf8')).clips).toEqual([]);
  });
});
