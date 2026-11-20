import { describe, expect, it } from 'vitest';
import { createRecorder, type Clip, type RecorderOptions } from '../../src/collector/recorder.ts';
import { ev, type AccState, type AdasEvent } from '../../src/schema/events.ts';
import { fakeRule, fakeRule2 } from './fake-rule.ts';

const track = (ts: number, s = 'S1') =>
  ev('ACC/TRACK', ts, s, { egoSpeed: 20, setSpeed: 20, leadDistance: null, leadRelSpeed: 0 });
const hit = (ts: number, lag: number, s = 'S1', rule = 'T-1') =>
  ev('ACC/TAKEOVER_REQUEST', ts, s, { reason: `${rule}:${lag}` });
const ticks = (from: number, to: number, s = 'S1') => {
  const out: AdasEvent[] = [];
  for (let ts = from; ts <= to; ts += 100) out.push(track(ts, s));
  return out;
};
/** Merge event lists by ts (stable: earlier lists first on ties). */
const byTs = (...lists: AdasEvent[][]) => lists.flat().sort((a, b) => a.ts - b.ts);

const OPTS: RecorderOptions = { bufferMs: 10000, preMs: 2000, postMs: 3000, rules: [fakeRule, fakeRule2] };

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

describe('recorder: merging', () => {
  it('merges overlapping windows in a scenario; the file name uses the first trigger', () => {
    const clips = record(byTs(ticks(0, 20000), [hit(8000, 1000), hit(10000, 500, 'S1', 'T-2'), hit(10500, 0)]));
    expect(clips).toHaveLength(1);
    expect(clips[0]).toMatchObject({ file: 'S1_7000_T-1.jsonl', ruleIds: ['T-1', 'T-2'], startTs: 5000, endTs: 13500 });
    expect(range(clips[0] as Clip)).toEqual([5000, 13500]);
  });

  it('extends the start backwards when a later trigger starts earlier', () => {
    const clips = record(byTs(ticks(0, 20000), [hit(8000, 0), hit(9000, 4000, 'S1', 'T-2')]));
    expect(clips).toHaveLength(1);
    expect(clips[0]).toMatchObject({ file: 'S1_8000_T-1.jsonl', startTs: 3000, endTs: 12000 });
    expect(range(clips[0] as Clip)).toEqual([3000, 12000]);
  });

  it('keeps non-overlapping windows and other scenarios apart', () => {
    const events = byTs(ticks(0, 30000), ticks(0, 30000, 'S2'), [hit(8000, 1000), hit(16000, 0), hit(8000, 1000, 'S2')]);
    const clips = record(events);
    expect(clips.map((c) => [c.file, c.startTs, c.endTs])).toEqual([
      ['S1_7000_T-1.jsonl', 5000, 11000],
      ['S2_7000_T-1.jsonl', 5000, 11000],
      ['S1_16000_T-1.jsonl', 14000, 19000],
    ]);
  });
});

describe('recorder: manual trigger', () => {
  const manual = (ts: number, s = 'S1') => ev('SYS/MANUAL_TRIGGER', ts, s, { note: 'driver button' });

  it('records a window around SYS/MANUAL_TRIGGER', () => {
    const clips = record(byTs(ticks(0, 20000), [manual(8050)]));
    expect(clips).toHaveLength(1);
    expect(clips[0]).toMatchObject({
      file: 'S1_8050_MANUAL.jsonl', trigger: 'manual', ruleIds: [], startTs: 6050, endTs: 11050, truncated: false,
    });
    expect(range(clips[0] as Clip)).toEqual([6100, 11000]);
  });

  it('merges with an overlapping rule trigger and keeps the first trigger', () => {
    const clips = record(byTs(ticks(0, 20000), [manual(8000), hit(9000, 0)]));
    expect(clips).toHaveLength(1);
    expect(clips[0]).toMatchObject({ file: 'S1_8000_MANUAL.jsonl', trigger: 'manual', ruleIds: ['T-1'], endTs: 12000 });
  });
});

describe('recorder: truncation', () => {
  const short = { ...OPTS, bufferMs: 3000, preMs: 5000 };

  it('truncates and flags a window that starts before the buffered data', () => {
    const clips = record(byTs(ticks(0, 20000), [hit(10000, 0)]), short);
    expect(clips[0]).toMatchObject({ startTs: 7000, endTs: 13000, truncated: true });
    expect(range(clips[0] as Clip)).toEqual([7000, 13000]);
  });

  it('does not flag a window that only reaches before the first event of the scenario', () => {
    const clips = record(byTs(ticks(4000, 20000), [hit(6000, 0)]), short);
    expect(clips[0]).toMatchObject({ startTs: 1000, truncated: false });
    expect(range(clips[0] as Clip)).toEqual([4000, 9000]);
  });

  it('clamps a negative window start to 0', () => {
    const clips = record(byTs(ticks(0, 20000), [hit(1000, 0)]), short);
    expect(clips[0]).toMatchObject({ startTs: 0, truncated: false });
  });
});

describe('recorder: preconditions', () => {
  const acc = (ts: number, from: AccState, to: AccState) => ev('ACC/STATE', ts, 'S1', { from, to, reason: 't' });
  const states = [
    acc(0, 'OFF', 'ACTIVE'), acc(1000, 'ACTIVE', 'STANDBY'), acc(2000, 'STANDBY', 'ACTIVE'),
    ev('APA/STEP', 500, 'S1', { step: 'MANEUVERING', reason: 't' }),
    ev('LKA/STATE', 600, 'S1', { from: 'OFF', to: 'ACTIVE' }),
    ev('VEH/TURN_SIGNAL', 700, 'S1', { side: 'LEFT' }),
    ev('VEH/TURN_SIGNAL', 800, 'S1', { side: 'OFF' }),
  ];
  const pre = (c: Clip) => c.events.filter((e) => e.ts < c.startTs).map((e) => `${e.module}/${e.type}@${e.ts}`);

  it('prepends the latest state event of each dimension before the window, sorted by ts', () => {
    const clips = record(byTs(ticks(0, 20000), states, [acc(9000, 'ACTIVE', 'OVERRIDE'), hit(10000, 0)]));
    const c = clips[0] as Clip;
    expect(c.startTs).toBe(8000);
    expect(pre(c)).toEqual(['APA/STEP@500', 'LKA/STATE@600', 'VEH/TURN_SIGNAL@800', 'ACC/STATE@2000']);
    expect(c.events.filter((e) => e.module === 'ACC' && e.type === 'STATE').map((e) => e.ts)).toEqual([2000, 9000]);
  });

  it('remembers state events already evicted from the buffer', () => {
    const clips = record(byTs(ticks(0, 20000), states, [hit(10000, 0)]), { ...OPTS, bufferMs: 3000 });
    expect(pre(clips[0] as Clip)).toEqual(['APA/STEP@500', 'LKA/STATE@600', 'VEH/TURN_SIGNAL@800', 'ACC/STATE@2000']);
  });

  it('restores a state event lost to truncation inside the window', () => {
    const clips = record(byTs(ticks(0, 20000), [acc(5000, 'OFF', 'ACTIVE'), hit(12000, 7000)]), { ...OPTS, bufferMs: 3000 });
    const c = clips[0] as Clip;
    expect(c).toMatchObject({ truncated: true, startTs: 9000 });
    expect(pre(c)).toEqual(['ACC/STATE@5000']);
  });

  it('adds nothing when no state event precedes the window', () => {
    const c = record(byTs(ticks(0, 20000), [hit(10000, 0)]))[0] as Clip;
    expect(pre(c)).toEqual([]);
  });
});
