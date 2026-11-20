import { describe, expect, it } from 'vitest';
import { createRecorder, type Clip, type RecorderOptions } from '../../src/collector/recorder.ts';
import { ev, type AdasEvent } from '../../src/schema/events.ts';
import { fakeRule } from './fake-rule.ts';

const track = (ts: number, s = 'S1') =>
  ev('ACC/TRACK', ts, s, { egoSpeed: 20, setSpeed: 20, leadDistance: null, leadRelSpeed: 0 });
const hit = (ts: number, lag: number, s = 'S1') => ev('ACC/TAKEOVER_REQUEST', ts, s, { reason: `lag:${lag}` });
const ticks = (from: number, to: number, s = 'S1') => {
  const out: AdasEvent[] = [];
  for (let ts = from; ts <= to; ts += 100) out.push(track(ts, s));
  return out;
};
/** Merge event lists by ts (stable: earlier lists first on ties). */
const byTs = (...lists: AdasEvent[][]) => lists.flat().sort((a, b) => a.ts - b.ts);

const OPTS: RecorderOptions = { bufferMs: 10000, preMs: 2000, postMs: 3000, rules: [fakeRule] };

function record(events: readonly AdasEvent[], opts: RecorderOptions = OPTS, eof = true): Clip[] {
  const clips: Clip[] = [];
  const r = createRecorder(opts, (c) => clips.push(c));
  for (const e of events) r.push(e);
  if (eof) r.end();
  return clips;
}
const range = (c: Clip) => [c.events[0]?.ts, c.events.at(-1)?.ts];

describe('recorder: rule triggers', () => {
  it('writes the window [startTs − pre, detectTs + post] once recording passes its end', () => {
    const clips = record(byTs(ticks(0, 20000), [hit(8000, 1000)]), OPTS, false);
    expect(clips).toHaveLength(1);
    const c = clips[0] as Clip;
    expect(c).toMatchObject({
      file: 'S1_7000_T-1.jsonl', scenarioId: 'S1', trigger: 'rule', ruleIds: ['T-1'],
      startTs: 5000, endTs: 11000, truncated: false,
    });
    expect(range(c)).toEqual([5000, 11000]);
    expect(c.events.filter((e) => e.type === 'TAKEOVER_REQUEST')).toHaveLength(1);
  });

  it('keeps recording until scenario time passes detectTs + post', () => {
    const clips: Clip[] = [];
    const r = createRecorder(OPTS, (c) => clips.push(c));
    for (const e of byTs(ticks(0, 11000), [hit(8000, 1000)])) r.push(e);
    expect(clips).toHaveLength(0);
    r.push(track(11100));
    expect(clips).toHaveLength(1);
  });

  it('defaults to a 10 s buffer, 5 s pre and 5 s post with the full ruleset', () => {
    const clips: Clip[] = [];
    const r = createRecorder({}, (c) => clips.push(c));
    expect(r.options).toEqual({ bufferMs: 10000, preMs: 5000, postMs: 5000 });
  });
});
