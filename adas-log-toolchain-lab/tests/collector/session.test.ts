import { mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { recordSession, type SessionResult } from '../../src/collector/session.ts';
import { fileSource } from '../../src/collector/source.ts';
import { runCaseFile } from '../../src/casegen/case-runner.ts';
import { generate } from '../../src/generator/generate.ts';
import { toJsonl } from '../../src/io/jsonl.ts';
import { parseFile } from '../../src/parser/parse.ts';

const FIXTURE = 'fixtures/collector/drive.jsonl';
const tmp = () => mkdtempSync(join(tmpdir(), 'adas-rec-'));

async function recordFile(input: string, out = tmp()): Promise<SessionResult & { out: string }> {
  const r = await recordSession(fileSource(input), { out, source: input });
  return { ...r, out };
}

/** Every rule-triggered clip, replayed on its own, fires each of its rules. */
async function expectRoundTrip(r: SessionResult & { out: string }) {
  const ruleClips = r.index.clips.filter((c) => c.ruleIds.length > 0);
  for (const c of ruleClips) {
    const { firedRules } = await runCaseFile(join(r.out, c.file));
    expect(firedRules, c.file).toEqual(expect.arrayContaining(c.ruleIds));
  }
  return ruleClips;
}

/** Every anomaly the parser finds in the input is covered by a clip of the same scenario. */
async function expectAllAnomaliesRecorded(input: string, r: SessionResult) {
  const { report } = await parseFile(input);
  for (const a of report.anomalies) {
    const covered = r.index.clips.some((c) => c.scenarioId === a.scenarioId && c.ruleIds.includes(a.ruleId)
      && c.endTs >= a.detectTs);
    expect(covered, `${a.ruleId} ${a.scenarioId} @${a.startTs}`).toBe(true);
  }
  return report.anomalies.length;
}

describe('record session: file source', () => {
  it('writes clips, index.json and quality.json for the hand-built drive', async () => {
    const r = await recordFile(FIXTURE);
    expect(JSON.parse(readFileSync(join(r.out, 'index.json'), 'utf8'))).toEqual(r.index);
    expect(JSON.parse(readFileSync(join(r.out, 'quality.json'), 'utf8'))).toEqual(r.quality);
    expect(r.index.meta).toMatchObject({ source: FIXTURE, bufferMs: 10000, preMs: 5000, postMs: 5000 });
    expect(r.index.clips.map((c) => [c.file, c.trigger, c.truncated])).toEqual([
      ['A01_10000_MANUAL.jsonl', 'manual', false],
      ['A01_20000_ACC-01.jsonl', 'rule', false],
      ['A01_35000_ACC-02.jsonl', 'rule', false],
      ['P01_8000_APA-01.jsonl', 'rule', false],
      ['P01_2000_APA-02.jsonl', 'rule', true],
    ]);
    expect(r.quality.totals).toMatchObject({ droppedFrames: 2, duplicates: 1, tsAnomalies: 1 });
    expect(r.events).toBe(1482);
    expect(r.dataQuality.invalidJson).toBe(1);
  });

  it('round trip: every rule-triggered clip replays with its target rule firing', async () => {
    const r = await recordFile(FIXTURE);
    expect(await expectRoundTrip(r)).toHaveLength(4);
    expect(await expectAllAnomaliesRecorded(FIXTURE, r)).toBe(4);
  });

  it('counts duplicates on the raw input lines', async () => {
    const dir = tmp(), input = join(dir, 'in.jsonl');
    // Same event, different key order: not an exact duplicate line.
    writeFileSync(input, '{"ts":0,"scenarioId":"S","module":"SYS","type":"MANUAL_TRIGGER","data":{"note":"a"}}\n'
      + '{"scenarioId":"S","ts":0,"module":"SYS","type":"MANUAL_TRIGGER","data":{"note":"a"}}\n');
    const r = await recordFile(input, join(dir, 'clips'));
    expect(r.quality.totals.duplicates).toBe(0);
  });

  it.each([42, 7])('round trip on generated logs (seed %i, interleaved)', async (seed) => {
    const dir = tmp(), input = join(dir, 'events.jsonl');
    writeFileSync(input, toJsonl(generate({ seed, scenarios: 20, interleave: true }).events));
    const r = await recordFile(input, join(dir, 'clips'));
    const n = await expectAllAnomaliesRecorded(input, r);
    expect(n).toBeGreaterThan(0);
    await expectRoundTrip(r);
  });
});
